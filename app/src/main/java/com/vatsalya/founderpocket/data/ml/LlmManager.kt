package com.vatsalya.founderpocket.data.ml

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * On-device LLM inference wrapper — Spike C (MediaPipe tasks-genai).
 *
 * MODEL SETUP (do after Spike C benchmark passes >5 tok/s):
 *  1. Download from Kaggle (accept Gemma terms):
 *       https://www.kaggle.com/models/google/gemma-3/frameworks/litert
 *     File: gemma3-1b-it-cpu-int4.task
 *  2. Push to device:
 *       adb push gemma3-1b-it-cpu-int4.task \
 *         /data/data/com.vatsalya.founderpocket/files/models/
 *  3. isAvailable will return true — AssistantScreen enables the AI query form.
 *
 * STREAMING: In tasks-genai ≥0.10.14 the result listener is registered at LlmInference
 * construction time via the options builder. We store the active token callback in a
 * member var; only one concurrent generate() call is supported (sufficient for this app).
 *
 * VERSION: If Gemma 3 needs a newer tasks-genai version, update mediapipeTasksGenai
 * in libs.versions.toml after Spike C confirms which version works on your device.
 */
@Singleton
class LlmManager @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        const val TAG = "LlmManager"
        const val MODEL_FILENAME = "gemma3-1b-it-cpu-int4.task"
        const val MAX_TOKENS = 512
    }

    fun modelFile(): File = context.filesDir.resolve("models/$MODEL_FILENAME")

    val isAvailable: Boolean get() = modelFile().exists()

    // These are accessed from the LlmInference result-listener thread
    @Volatile private var activeContinuation: kotlinx.coroutines.CancellableContinuation<Unit>? = null
    @Volatile private var activeOnToken: ((String) -> Unit)? = null

    private var llmInference: LlmInference? = null
    private val initMutex = Mutex()

    private suspend fun ensureLoaded(): Boolean = initMutex.withLock {
        if (llmInference != null) return@withLock true
        if (!isAvailable) return@withLock false
        return@withLock withContext(Dispatchers.Default) {
            runCatching {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile().absolutePath)
                    .setMaxTokens(MAX_TOKENS)
                    .setTopK(40)
                    .setTemperature(0.8f)
                    .setRandomSeed(101)
                    .setResultListener { partial, done ->
                        // Called on LlmInference's internal thread per token
                        if (partial != null) activeOnToken?.invoke(partial)
                        if (done) {
                            val cont = activeContinuation
                            activeContinuation = null
                            activeOnToken = null
                            cont?.resume(Unit)
                        }
                    }
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                Log.i(TAG, "Model loaded: $MODEL_FILENAME")
                true
            }.getOrElse { e ->
                Log.e(TAG, "Failed to load model", e)
                false
            }
        }
    }

    /**
     * Generates a response, streaming partial tokens to [onToken].
     * Suspends until generation is complete or an error occurs.
     */
    suspend fun generate(prompt: String, onToken: (String) -> Unit) {
        if (!ensureLoaded()) {
            onToken(
                "AI not activated yet.\n\n" +
                "Push Gemma 3 1B model to:\n${modelFile().absolutePath}\n\n" +
                "See LlmManager.kt for full instructions."
            )
            return
        }
        withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { cont ->
                activeContinuation = cont
                activeOnToken = onToken
                try {
                    llmInference!!.generateResponseAsync(prompt)
                } catch (e: Exception) {
                    Log.e(TAG, "generateResponseAsync failed", e)
                    activeContinuation = null
                    activeOnToken = null
                    onToken("\n[Error: ${e.message}]")
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    /** Release the model to reclaim ~2 GB RAM; reloads lazily on next generate(). */
    fun release() {
        llmInference?.close()
        llmInference = null
    }
}

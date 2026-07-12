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
 * On-device LLM inference — tasks-genai 0.10.35 + Gemma 3 1B int4.
 *
 * API changes in 0.10.35 vs 0.10.14:
 *  - setTopK/setTemperature/setRandomSeed moved to LlmInferenceSession (per-query)
 *  - setMaxTopK replaces setTopK in LlmInferenceOptions
 *  - setResultListener removed from options; listener passed to generateResponseAsync(prompt, listener)
 *  - generateResponseAsync now returns ListenableFuture<String> (ignored; we use the callback)
 *
 * MODEL SETUP: gemma3-1B-it-int4.task (529 MB) is bundled in assets.
 * First generate() call copies it to filesDir/models/ — takes ~30 s, only once.
 */
@Singleton
class LlmManager @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        const val TAG = "LlmManager"
        const val MODEL_FILENAME = "gemma3-1B-it-int4.task"
        const val MAX_TOKENS = 512
    }

    fun modelFile(): File = context.filesDir.resolve("models/$MODEL_FILENAME")

    val isAvailable: Boolean
        get() = modelFile().exists() || assetExists()

    private fun assetExists(): Boolean = runCatching {
        context.assets.open(MODEL_FILENAME).use { true }
    }.getOrDefault(false)

    private var llmInference: LlmInference? = null
    private val initMutex = Mutex()

    private suspend fun ensureLoaded(): Boolean = initMutex.withLock {
        if (llmInference != null) return@withLock true
        if (!isAvailable) return@withLock false

        return@withLock withContext(Dispatchers.IO) {
            // One-time copy from assets → filesDir
            if (!modelFile().exists()) {
                Log.i(TAG, "First run: copying $MODEL_FILENAME from assets (~529 MB)…")
                modelFile().parentFile?.mkdirs()
                runCatching {
                    context.assets.open(MODEL_FILENAME).use { src ->
                        modelFile().outputStream().use { dst -> src.copyTo(dst) }
                    }
                    Log.i(TAG, "Copy complete.")
                }.onFailure { e ->
                    Log.e(TAG, "Asset copy failed", e)
                    return@withContext false
                }
            }

            // Load model
            runCatching {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile().absolutePath)
                    .setMaxTokens(MAX_TOKENS)
                    .setMaxTopK(40)           // model-level cap; per-query topK set in session
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                Log.i(TAG, "Model ready.")
                true
            }.getOrElse { e ->
                Log.e(TAG, "Failed to load model", e)
                false
            }
        }
    }

    /**
     * Streams generated tokens into [onToken]. Suspends until generation completes.
     *
     * In tasks-genai 0.10.35 the ProgressListener is passed directly to
     * generateResponseAsync(prompt, listener) — no options-level listener needed.
     */
    suspend fun generate(prompt: String, onToken: (String) -> Unit) {
        if (!ensureLoaded()) {
            onToken("Model unavailable. Check logcat (LlmManager) for errors.")
            return
        }
        withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { cont ->
                try {
                    llmInference!!.generateResponseAsync(prompt) { partial: String?, done: Boolean ->
                        partial?.let { onToken(it) }
                        if (done && cont.isActive) cont.resume(Unit)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "generateResponseAsync failed", e)
                    onToken("\n[Error: ${e.message}]")
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    fun release() {
        llmInference?.close()
        llmInference = null
        Log.i(TAG, "Model released.")
    }
}

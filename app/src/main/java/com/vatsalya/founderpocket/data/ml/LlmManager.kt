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
 * On-device LLM inference — Spike C (MediaPipe tasks-genai + Gemma 3 1B int4).
 *
 * Model: gemma3-1B-it-int4.task (529 MB, bundled in app/src/main/assets/)
 *
 * First-run behaviour: copies the .task file from assets → filesDir/models/
 * on the first generate() call. Subsequent runs skip the copy.
 * The copy takes ~30 s depending on storage speed — only happens once.
 *
 * VERSION NOTE: tasks-genai 0.10.14 is used. If Gemma 3 needs a newer version
 * after Spike C benchmarking, update mediapipeTasksGenai in libs.versions.toml.
 * The litert-lm library can replace tasks-genai with the same API surface.
 */
@Singleton
class LlmManager @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        const val TAG = "LlmManager"
        const val MODEL_FILENAME = "gemma3-1B-it-int4.task"
        const val MAX_TOKENS = 512
    }

    /** Destination path that LlmInference reads from (needs a real file path, not an InputStream). */
    fun modelFile(): File = context.filesDir.resolve("models/$MODEL_FILENAME")

    /**
     * True if the model is ready to use:
     *  - already copied to filesDir, OR
     *  - present in assets (will be copied on first generate() call)
     */
    val isAvailable: Boolean
        get() = modelFile().exists() || assetExists()

    private fun assetExists(): Boolean = runCatching {
        context.assets.open(MODEL_FILENAME).use { true }
    }.getOrDefault(false)

    // Streaming callback state — one concurrent generate() call at a time
    @Volatile private var activeCont: kotlinx.coroutines.CancellableContinuation<Unit>? = null
    @Volatile private var activeOnToken: ((String) -> Unit)? = null

    private var llmInference: LlmInference? = null
    private val initMutex = Mutex()

    private suspend fun ensureLoaded(): Boolean = initMutex.withLock {
        if (llmInference != null) return@withLock true
        if (!isAvailable) return@withLock false

        return@withLock withContext(Dispatchers.IO) {
            // Copy from assets to filesDir if not already there
            if (!modelFile().exists()) {
                Log.i(TAG, "First-run: copying $MODEL_FILENAME from assets (~529 MB)…")
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

            // Load model into LlmInference
            runCatching {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile().absolutePath)
                    .setMaxTokens(MAX_TOKENS)
                    .setTopK(40)
                    .setTemperature(0.8f)
                    .setRandomSeed(101)
                    .setResultListener { partial, done ->
                        if (partial != null) activeOnToken?.invoke(partial)
                        if (done) {
                            val cont = activeCont
                            activeCont = null
                            activeOnToken = null
                            cont?.resume(Unit)
                        }
                    }
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
     * First call triggers the one-time asset-copy + model load (~30–60 s on first run).
     */
    suspend fun generate(prompt: String, onToken: (String) -> Unit) {
        if (!ensureLoaded()) {
            onToken("Model unavailable. Check logcat for LlmManager errors.")
            return
        }
        withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { cont ->
                activeCont = cont
                activeOnToken = onToken
                try {
                    llmInference!!.generateResponseAsync(prompt)
                } catch (e: Exception) {
                    Log.e(TAG, "generateResponseAsync failed", e)
                    activeCont = null
                    activeOnToken = null
                    onToken("\n[Error: ${e.message}]")
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    /** Release loaded model to reclaim ~1 GB RAM; reloads lazily on next generate(). */
    fun release() {
        llmInference?.close()
        llmInference = null
        Log.i(TAG, "Model released.")
    }
}

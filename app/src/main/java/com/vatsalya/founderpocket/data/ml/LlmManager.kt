package com.vatsalya.founderpocket.data.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for on-device LLM inference (LiteRT-LM + Gemma 3 1B).
 *
 * HOW TO ACTIVATE after Spike C passes:
 *  1. Download the Gemma 3 1B CPU int4 model from Kaggle (accept Gemma terms).
 *     Filename: gemma3-1b-it-cpu-int4.task  (MediaPipe format)
 *     or the LiteRT-LM format after Spike C confirms the exact filename.
 *  2. Push the model file to the device:
 *       adb push gemma3-1b-it-cpu-int4.task /data/data/com.vatsalya.founderpocket/files/models/
 *  3. Add to libs.versions.toml:
 *       mediapipeTasksGenai = "0.10.22"    ← verify exact version in Spike C
 *       mediapipe-tasks-genai = { group = "com.google.mediapipe", name = "tasks-genai", version.ref = "mediapipeTasksGenai" }
 *  4. Add to app/build.gradle.kts:
 *       implementation(libs.mediapipe.tasks.genai)
 *  5. Uncomment the imports + inference block in generate() below.
 *
 * Fallback: if model file absent → isAvailable = false → UI shows rules-based results only.
 */
@Singleton
class LlmManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val MODEL_FILENAME = "gemma3-1b-it-cpu-int4.task"

    fun modelFile(): File = context.filesDir.resolve("models/$MODEL_FILENAME")

    val isAvailable: Boolean get() = modelFile().exists()

    /**
     * Streams generated tokens into [onToken]. Calls onToken once with the full response when done.
     *
     * After Spike C: replace the stub below with the real inference call, e.g.:
     *
     *   import com.google.mediapipe.tasks.genai.llminference.LlmInference
     *
     *   val options = LlmInference.LlmInferenceOptions.builder()
     *       .setModelPath(modelFile().absolutePath)
     *       .setMaxTokens(512)
     *       .setTopK(40)
     *       .setTemperature(0.8f)
     *       .build()
     *   val llm = LlmInference.createFromOptions(context, options)
     *   llm.generateResponseAsync(prompt) { partialResult, done ->
     *       onToken(partialResult ?: "")
     *   }
     */
    suspend fun generate(prompt: String, onToken: (String) -> Unit) {
        // Stub — replace after Spike C confirms model file path + library version.
        onToken(
            "AI assistant not yet activated.\n\n" +
            "Place the Gemma 3 1B model at:\n${modelFile().absolutePath}\n\n" +
            "See LlmManager.kt for full setup instructions."
        )
    }
}

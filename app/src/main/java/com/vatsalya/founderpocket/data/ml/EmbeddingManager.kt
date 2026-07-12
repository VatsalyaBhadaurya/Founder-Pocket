package com.vatsalya.founderpocket.data.ml

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps ONNX Runtime Mobile + WordPiece tokenizer for all-MiniLM-L6-v2.
 *
 * SETUP REQUIRED (Spike A gate):
 *   app/src/main/assets/all-MiniLM-L6-v2.onnx  — exported ONNX model
 *   app/src/main/assets/vocab.txt               — BERT vocab file, one token per line
 *
 * If either file is absent, embed() returns null and EmbedWorker skips silently.
 * This lets the spine run before Spike A is complete.
 */
@Singleton
class EmbeddingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val env by lazy { OrtEnvironment.getEnvironment() }
    private var session: OrtSession? = null
    private var tokenizer: WordPieceTokenizer? = null

    val isReady: Boolean get() = session != null && tokenizer != null

    // Called from background threads; safe to call multiple times.
    private fun ensureLoaded(): Boolean {
        if (isReady) return true
        return try {
            val vocab = context.assets.open("vocab.txt")
                .bufferedReader()
                .readLines()
                .mapIndexed { i, line -> line.trim() to i }
                .toMap()
            tokenizer = WordPieceTokenizer(vocab)

            val modelBytes = context.assets.open("all-MiniLM-L6-v2.onnx").readBytes()
            session = env.createSession(modelBytes, OrtSession.SessionOptions())
            true
        } catch (_: Exception) {
            false // Spike A not done yet — silent fallback
        }
    }

    /**
     * Embed [text] using MiniLM. Returns null if model is not loaded or inference fails.
     * Always runs on Dispatchers.Default; safe to call from any coroutine.
     */
    suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.Default) {
        if (!ensureLoaded()) return@withContext null
        val tok = tokenizer ?: return@withContext null
        val sess = session ?: return@withContext null
        try {
            val enc = tok.encode(text)
            val shape = longArrayOf(1, enc.inputIds.size.toLong())

            OnnxTensor.createTensor(env, LongBuffer.wrap(enc.inputIds), shape).use { idsTensor ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(enc.attentionMask), shape).use { maskTensor ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(enc.tokenTypeIds), shape).use { typeTensor ->
                val inputs = mapOf(
                    "input_ids"      to idsTensor,
                    "attention_mask" to maskTensor,
                    "token_type_ids" to typeTensor
                )
                sess.run(inputs).use { result ->
                    // Output: last_hidden_state [1, seqLen, hiddenSize]
                    val outputTensor = result.first().value as OnnxTensor
                    @Suppress("UNCHECKED_CAST")
                    val hiddenStates = outputTensor.value as Array<Array<FloatArray>>
                    meanPool(hiddenStates[0], enc.attentionMask)
                }
            }}}
        } catch (_: Exception) {
            null
        }
    }

    private fun meanPool(states: Array<FloatArray>, mask: LongArray): FloatArray {
        val hiddenSize = states[0].size
        val result = FloatArray(hiddenSize)
        var count = 0
        for (i in states.indices) {
            if (i < mask.size && mask[i] == 1L) {
                count++
                for (j in 0 until hiddenSize) result[j] += states[i][j]
            }
        }
        if (count > 0) for (j in 0 until hiddenSize) result[j] /= count
        return result
    }
}

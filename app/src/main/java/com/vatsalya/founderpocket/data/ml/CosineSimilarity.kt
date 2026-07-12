package com.vatsalya.founderpocket.data.ml

import kotlin.math.sqrt

object CosineSimilarity {

    /** Returns cosine similarity in [0, 1] (clamped; handles zero vectors). */
    fun compute(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vectors must have equal length" }
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        if (denom == 0f) return 0f
        return ((dot / denom + 1f) / 2f).coerceIn(0f, 1f) // shift [-1,1] → [0,1]
    }
}

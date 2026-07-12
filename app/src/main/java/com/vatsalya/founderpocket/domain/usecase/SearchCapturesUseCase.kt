package com.vatsalya.founderpocket.domain.usecase

import com.vatsalya.founderpocket.data.db.Converters
import com.vatsalya.founderpocket.data.ml.CosineSimilarity
import com.vatsalya.founderpocket.data.ml.EmbeddingManager
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchCapturesUseCase @Inject constructor(
    private val repository: CaptureRepository,
    private val embeddingManager: EmbeddingManager
) {
    /** Keyword search via Room FTS4. Returns a live Flow. */
    fun keyword(query: String): Flow<List<Capture>> {
        if (query.isBlank()) return repository.getAll()
        return repository.searchKeyword(query)
    }

    /**
     * Semantic search: embed query → cosine rank → recency boost.
     * Returns empty list if model is not loaded (Spike A not done yet).
     * Caller should run this in a ViewModel coroutine on Dispatchers.Default.
     */
    suspend fun semantic(query: String): List<Capture> {
        if (query.isBlank()) return emptyList()
        val queryVec = embeddingManager.embed(query) ?: return emptyList()
        val converters = Converters()
        return repository.getAllWithEmbeddings()
            .mapNotNull { capture ->
                val vec = converters.toFloatArray(capture.embedding) ?: return@mapNotNull null
                val score = fusionScore(queryVec, vec, capture.createdAt)
                Pair(capture, score)
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun fusionScore(queryVec: FloatArray, captureVec: FloatArray, createdAt: Long): Float {
        val semantic = CosineSimilarity.compute(queryVec, captureVec)
        val recency = recencyBoost(createdAt)
        return semantic * recency
    }

    private fun recencyBoost(createdAt: Long): Float {
        val ageMs = System.currentTimeMillis() - createdAt
        val sevenDays = 7L * 24 * 60 * 60 * 1000
        return if (ageMs <= sevenDays) 1.0f else 0.8f
    }
}

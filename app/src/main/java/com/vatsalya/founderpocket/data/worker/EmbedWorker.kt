package com.vatsalya.founderpocket.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vatsalya.founderpocket.data.db.Converters
import com.vatsalya.founderpocket.data.ml.EmbeddingManager
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EmbedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: CaptureRepository,
    private val embeddingManager: EmbeddingManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val captureId = inputData.getLong(KEY_CAPTURE_ID, -1L)
        if (captureId == -1L) return Result.failure()

        val capture = repository.getById(captureId) ?: return Result.failure()
        val text = buildTextForEmbedding(capture.ftsText, capture.body)

        val vector = embeddingManager.embed(text)
            ?: return Result.success() // model not ready yet — skip silently, not a hard failure

        val bytes = Converters().fromFloatArray(vector) ?: return Result.success()
        repository.updateEmbedding(captureId, bytes)
        return Result.success()
    }

    private fun buildTextForEmbedding(ftsText: String, body: String): String =
        ftsText.ifBlank { body }

    companion object {
        const val KEY_CAPTURE_ID = "capture_id"
    }
}

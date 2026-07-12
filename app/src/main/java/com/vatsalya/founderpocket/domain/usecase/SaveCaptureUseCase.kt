package com.vatsalya.founderpocket.domain.usecase

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import com.vatsalya.founderpocket.data.worker.EmbedWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SaveCaptureUseCase @Inject constructor(
    private val repository: CaptureRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(capture: Capture): Long {
        val id = repository.save(capture.copy(ftsText = buildFtsText(capture)))
        enqueueEmbedding(id)
        return id
    }

    private fun buildFtsText(capture: Capture): String =
        listOfNotNull(capture.body, capture.placeLabel, capture.tags)
            .joinToString(" ")

    private fun enqueueEmbedding(captureId: Long) {
        val request = OneTimeWorkRequestBuilder<EmbedWorker>()
            .setInputData(workDataOf(EmbedWorker.KEY_CAPTURE_ID to captureId))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

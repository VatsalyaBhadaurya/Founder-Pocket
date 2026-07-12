package com.vatsalya.founderpocket.domain.usecase

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.CaptureType
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import com.vatsalya.founderpocket.data.worker.EmbedWorker
import com.vatsalya.founderpocket.data.worker.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SaveCaptureUseCase @Inject constructor(
    private val repository: CaptureRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(capture: Capture, remindAt: Long? = null): Long {
        val id = repository.save(capture.copy(ftsText = buildFtsText(capture)))
        enqueueEmbedding(id)
        if (remindAt != null && remindAt > System.currentTimeMillis()) {
            enqueueReminder(id, capture, remindAt)
        }
        return id
    }

    private fun buildFtsText(capture: Capture): String =
        listOfNotNull(capture.body, capture.placeLabel, capture.tags.takeIf { it != "[]" })
            .joinToString(" ")

    private fun enqueueEmbedding(captureId: Long) {
        val request = OneTimeWorkRequestBuilder<EmbedWorker>()
            .setInputData(workDataOf(EmbedWorker.KEY_CAPTURE_ID to captureId))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun enqueueReminder(captureId: Long, capture: Capture, remindAt: Long) {
        val delayMs = remindAt - System.currentTimeMillis()
        val title = when (capture.type) {
            CaptureType.FOLLOWUP -> "Follow-up reminder"
            CaptureType.TASK     -> "Task due"
            else                 -> "Reminder"
        }
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(
                ReminderWorker.KEY_TITLE      to title,
                ReminderWorker.KEY_BODY       to capture.body,
                ReminderWorker.KEY_CAPTURE_ID to captureId
            ))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

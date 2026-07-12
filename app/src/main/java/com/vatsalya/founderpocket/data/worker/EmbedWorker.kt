package com.vatsalya.founderpocket.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// Placeholder — real ONNX embedding wired in Block 2 after Spike A proves the integration.
@HiltWorker
class EmbedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: CaptureRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val captureId = inputData.getLong(KEY_CAPTURE_ID, -1L)
        if (captureId == -1L) return Result.failure()
        // TODO Block 2: replace with real MiniLM embedding via ONNX Runtime Mobile
        return Result.success()
    }

    companion object {
        const val KEY_CAPTURE_ID = "capture_id"
    }
}

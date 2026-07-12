package com.vatsalya.founderpocket.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vatsalya.founderpocket.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Reminder"
        val body  = inputData.getString(KEY_BODY) ?: ""
        val id    = inputData.getLong(KEY_CAPTURE_ID, System.currentTimeMillis()).toInt()

        ensureChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success() // permission not granted; skip silently
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(id, notification)
        return Result.success()
    }

    private fun ensureChannel() {
        val mgr = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    companion object {
        const val CHANNEL_ID      = "reminders"
        const val KEY_TITLE       = "title"
        const val KEY_BODY        = "body"
        const val KEY_CAPTURE_ID  = "capture_id"
    }
}

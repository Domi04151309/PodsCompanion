package io.github.domi04151309.podscompanion.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.domi04151309.podscompanion.R

class NotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManagerCompat

    init {
        createNotificationChannel()
        notificationManager = NotificationManagerCompat.from(context)
    }

    fun showNotification() {
        notificationManager.notify(
            NOTIFICATION_ID,
            generateNotification(context.resources.getString(R.string.loading))
        )
    }

    fun updateNotification(left: String, case: String, right: String) {
        notificationManager.notify(
            NOTIFICATION_ID,
            generateNotification(context.resources.getString(R.string.status_text, left, case, right))
        )

    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun generateNotification(text: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_pods_white)
            .setShowWhen(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.resources.getString(R.string.status_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "status_channel"
        private const val NOTIFICATION_ID = 50
    }
}
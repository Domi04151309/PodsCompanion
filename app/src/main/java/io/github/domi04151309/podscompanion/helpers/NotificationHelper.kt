package io.github.domi04151309.podscompanion.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import io.github.domi04151309.podscompanion.R
import io.github.domi04151309.podscompanion.services.PodsService

class NotificationHelper(private val context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val notificationManager: NotificationManagerCompat
    private var shouldShowNotification: Boolean

    init {
        createNotificationChannel()
        notificationManager = NotificationManagerCompat.from(context)
        shouldShowNotification = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            PREF_SHOW_NOTIFICATION, PREF_SHOW_NOTIFICATION_DEFAULT
        )
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this)
    }

    fun showNotification() {
        if (shouldShowNotification) {
            notificationManager.notify(
                NOTIFICATION_ID,
                generateNotification(context.resources.getString(R.string.loading))
            )
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(PodsService.REQUEST_AIRPODS_BATTERY))
        }
    }

    fun updateNotification(left: String, case: String, right: String) {
        if (shouldShowNotification) {
            notificationManager.notify(
                NOTIFICATION_ID,
                generateNotification(context.resources.getString(R.string.status_text, left, case, right))
            )
        }
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        if (key == PREF_SHOW_NOTIFICATION) {
            if (prefs.getBoolean(PREF_SHOW_NOTIFICATION, PREF_SHOW_NOTIFICATION_DEFAULT)) {
                shouldShowNotification = true
                showNotification()
            } else {
                shouldShowNotification = false
                cancelNotification()
            }
        }
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
        private const val PREF_SHOW_NOTIFICATION = "show_notification"
        private const val PREF_SHOW_NOTIFICATION_DEFAULT = true
    }
}
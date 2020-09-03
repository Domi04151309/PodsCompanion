package io.github.domi04151309.podscompanion.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import io.github.domi04151309.podscompanion.R
import io.github.domi04151309.podscompanion.services.PodsService

class NotificationHelper(private val context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val notificationManager: NotificationManagerCompat
    private val prefs: SharedPreferences
    private var shouldShowNotification: Boolean

    init {
        createNotificationChannel()
        notificationManager = NotificationManagerCompat.from(context)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        shouldShowNotification = prefs.getBoolean(
            PREF_SHOW_NOTIFICATION, PREF_SHOW_NOTIFICATION_DEFAULT
        )
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    fun showNotification() {
        if (shouldShowNotification) {
            val loading = context.resources.getString(R.string.loading)
            notificationManager.notify(
                NOTIFICATION_ID,
                generateNotification(loading, loading, loading)
            )
            requestUpdate()
        }
    }

    fun updateNotification(left: String, case: String, right: String) {
        if (shouldShowNotification) {
            notificationManager.notify(
                NOTIFICATION_ID,
                generateNotification(left, case, right)
            )
        }
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        cancelNotification()
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
        } else if (key == PREF_NOTIFICATION_STYLE) {
            requestUpdate()
        }
    }

    private fun requestUpdate() {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(PodsService.REQUEST_AIRPODS_BATTERY))
    }

    private fun generateNotification(left: String, case: String, right: String): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pods_white)
            .setShowWhen(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        when (prefs.getString(PREF_NOTIFICATION_STYLE, PREF_NOTIFICATION_STYLE_RICH)) {
            PREF_NOTIFICATION_STYLE_RICH -> {
                val disconnectedLong = context.resources.getString(R.string.unknown_status)
                val disconnectedShort = context.resources.getString(R.string.unknown_status_short)
                val views = RemoteViews(context.packageName, R.layout.notification_status)
                views.setTextViewText(R.id.txt_left, if (left == disconnectedLong) disconnectedShort else left)
                views.setTextViewText(R.id.txt_case, if (case == disconnectedLong) disconnectedShort else case)
                views.setTextViewText(R.id.txt_right, if (right == disconnectedLong) disconnectedShort else right)
                builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                builder.setContent(views)
            }
            PREF_NOTIFICATION_STYLE_TEXT_ONLY -> {
                builder.setContentText(context.resources.getString(R.string.status_text, left, case, right))
            }
        }
        return builder.build()
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
        private const val PREF_NOTIFICATION_STYLE = "notification_style"
        private const val PREF_NOTIFICATION_STYLE_RICH = "rich"
        private const val PREF_NOTIFICATION_STYLE_TEXT_ONLY = "text"
    }
}
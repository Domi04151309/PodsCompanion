package io.github.domi04151309.podscompanion.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import io.github.domi04151309.podscompanion.R
import io.github.domi04151309.podscompanion.services.PodsService.Companion.status

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

    fun updateNotification() {
        if (shouldShowNotification) {
            notificationManager.notify(
                NOTIFICATION_ID,
                generateNotification()
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
                updateNotification()
            } else {
                shouldShowNotification = false
                cancelNotification()
            }
        } else if (key == PREF_NOTIFICATION_STYLE) {
            updateNotification()
        }
    }

    private fun generateNotification(): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pods_white)
            .setShowWhen(false)
            .setOngoing(true)
            .setNotificationSilent()
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        when (prefs.getString(PREF_NOTIFICATION_STYLE, PREF_NOTIFICATION_STYLE_RICH)) {
            PREF_NOTIFICATION_STYLE_RICH -> {
                val views = RemoteViews(context.packageName, R.layout.notification_status)
                views.setImageViewResource(R.id.state_left, status.generateDrawableId(status.left))
                views.setTextViewText(R.id.txt_left, status.generateString(context, status.left, R.string.unknown_status_short))
                views.setImageViewResource(R.id.state_case, status.generateDrawableId(status.case))
                views.setTextViewText(R.id.txt_case, status.generateString(context, status.case, R.string.unknown_status_short))
                views.setImageViewResource(R.id.state_right, status.generateDrawableId(status.right))
                views.setTextViewText(R.id.txt_right, status.generateString(context, status.right, R.string.unknown_status_short))
                builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                builder.setContent(views)
            }
            PREF_NOTIFICATION_STYLE_RICH_WITHOUT_BATTERY_ICONS -> {
                val views = RemoteViews(context.packageName, R.layout.notification_status_without_icons)
                views.setTextViewText(R.id.txt_left, status.generateString(context, status.left, R.string.unknown_status_short))
                views.setTextViewText(R.id.txt_case, status.generateString(context, status.case, R.string.unknown_status_short))
                views.setTextViewText(R.id.txt_right, status.generateString(context, status.right, R.string.unknown_status_short))
                builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                builder.setContent(views)
            }
            PREF_NOTIFICATION_STYLE_TEXT_ONLY -> {
                builder.setContentText(
                    context.getString(
                        R.string.status_text,
                        status.generateString(context, status.left, R.string.unknown_status_short),
                        status.generateString(context, status.case, R.string.unknown_status_short),
                        status.generateString(context, status.right, R.string.unknown_status_short)
                    )
                )
            }
        }
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.status_channel),
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
        private const val PREF_NOTIFICATION_STYLE_RICH_WITHOUT_BATTERY_ICONS = "rich_without"
        private const val PREF_NOTIFICATION_STYLE_TEXT_ONLY = "text"
    }
}
package io.github.domi04151309.podscompanion.receivers

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import io.github.domi04151309.podscompanion.R
import io.github.domi04151309.podscompanion.services.PodsService.Companion.status

class StatusWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_status)
        views.setTextViewText(R.id.txt_left, status.generateString(context, status.left, R.string.unknown_status_short))
        views.setTextViewText(R.id.txt_case, status.generateString(context, status.case, R.string.unknown_status_short))
        views.setTextViewText(R.id.txt_right, status.generateString(context, status.right, R.string.unknown_status_short))
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
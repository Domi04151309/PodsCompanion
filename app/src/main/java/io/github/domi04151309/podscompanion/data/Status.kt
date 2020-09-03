package io.github.domi04151309.podscompanion.data

import android.content.Context
import io.github.domi04151309.podscompanion.R

data class Status(
    val left: StatusElement = StatusElement(),
    val right: StatusElement = StatusElement(),
    val case: StatusElement = StatusElement()
) {
    companion object {
        fun generateString(context: Context, element: StatusElement, disconnectedId: Int): String {
            return if (element.connected) context.getString(R.string.battery_percentage, element.charge) else context.getString(disconnectedId)
        }
    }
}
package io.github.domi04151309.podscompanion.data

import android.content.Context
import io.github.domi04151309.podscompanion.R

data class Status(
    val left: StatusElement = StatusElement(),
    val right: StatusElement = StatusElement(),
    val case: StatusElement = StatusElement()
) {

    private var cacheLeft: StatusElement = StatusElement()
    private var cacheRight: StatusElement = StatusElement()
    private var cacheCase: StatusElement = StatusElement()

    fun updateCache() {
        cacheLeft = left.createClone()
        cacheRight = right.createClone()
        cacheCase = case.createClone()
    }

    fun hasChangedSinceCacheUpdate(): Boolean {
        return left.charge != cacheLeft.charge
                || left.charging != cacheLeft.charging
                || left.connected != cacheLeft.connected
                || right.charge != cacheRight.charge
                || right.charging != cacheRight.charging
                || right.connected != cacheRight.connected
                || case.charge != cacheCase.charge
                || case.charging != cacheCase.charging
                || case.connected != cacheCase.connected
    }

    companion object {
        fun generateString(context: Context, element: StatusElement, disconnectedId: Int): String {
            return if (element.connected) context.getString(R.string.battery_percentage, element.charge) else context.getString(disconnectedId)
        }

        fun generateDrawable(element: StatusElement): Int {
            return if (element.connected) {
                if (element.charging) {
                    R.drawable.ic_battery_charging
                } else {
                    R.drawable.ic_battery
                }
            } else {
                R.drawable.ic_battery_unknown
            }
        }
    }
}
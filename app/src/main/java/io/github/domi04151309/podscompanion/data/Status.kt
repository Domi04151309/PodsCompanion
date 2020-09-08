package io.github.domi04151309.podscompanion.data

import android.content.Context
import io.github.domi04151309.podscompanion.R

data class Status(
    val left: StatusElement = StatusElement(),
    val right: StatusElement = StatusElement(),
    val case: StatusElement = StatusElement(),
    var available: Boolean = false
) {

    private var cacheLeft: StatusElement = StatusElement()
    private var cacheRight: StatusElement = StatusElement()
    private var cacheCase: StatusElement = StatusElement()
    private var cacheAvailable: Boolean = false

    fun updateCache() {
        cacheLeft = left.createClone()
        cacheRight = right.createClone()
        cacheCase = case.createClone()
        cacheAvailable = available
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
                || available != cacheAvailable
    }

    fun generateString(context: Context, element: StatusElement, disconnectedId: Int): String {
        return if (available) {
            if (element.charge != 0.toByte()) context.getString(R.string.battery_percentage, element.charge)
            else context.getString(disconnectedId)
        } else {
            context.getString(disconnectedId)
        }
    }

    fun generateDrawableId(element: StatusElement): Int {
        return if (element.connected && available) {
            if (element.charging) {
                R.drawable.ic_battery_charging
            } else {
                when {
                    element.charge >= 100 -> R.drawable.ic_battery_100
                    element.charge >= 90 -> R.drawable.ic_battery_90
                    element.charge >= 80 -> R.drawable.ic_battery_80
                    element.charge >= 60 -> R.drawable.ic_battery_60
                    element.charge >= 50 -> R.drawable.ic_battery_50
                    element.charge >= 30 -> R.drawable.ic_battery_30
                    element.charge >= 20 -> R.drawable.ic_battery_20
                    element.charge >= 0 -> R.drawable.ic_battery_0
                    else -> R.drawable.ic_battery_0
                }
            }
        } else {
            R.drawable.ic_battery_unknown
        }
    }
}
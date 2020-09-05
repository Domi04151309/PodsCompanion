package io.github.domi04151309.podscompanion.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import io.github.domi04151309.podscompanion.R

class BatteryPreference : Preference {

    var leftTxt: TextView? = null
    var caseTxt: TextView? = null
    var rightTxt: TextView? = null

    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle) {
        setupView()
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        setupView()
    }

    constructor(context: Context): super(context) {
        setupView()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        leftTxt = holder?.itemView?.findViewById(R.id.txt_left)
        caseTxt = holder?.itemView?.findViewById(R.id.txt_case)
        rightTxt = holder?.itemView?.findViewById(R.id.txt_right)
    }

    private fun setupView() {
        layoutResource = R.layout.preference_battery
        isSelectable = false
    }
}
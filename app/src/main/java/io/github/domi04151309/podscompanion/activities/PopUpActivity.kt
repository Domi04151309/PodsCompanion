package io.github.domi04151309.podscompanion.activities

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.domi04151309.podscompanion.R
import io.github.domi04151309.podscompanion.services.PodsService
import io.github.domi04151309.podscompanion.services.PodsService.Companion.status

class PopUpActivity : Activity() {

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            findViewById<TextView>(R.id.txt_left).text = status.generateString(context, status.left, R.string.unknown_status)
            findViewById<TextView>(R.id.txt_case).text = status.generateString(context, status.case, R.string.unknown_status)
            findViewById<TextView>(R.id.txt_right).text = status.generateString(context, status.right, R.string.unknown_status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pop_up_status)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    override fun onStart() {
        super.onStart()
        localBroadcastManager.registerReceiver(batteryReceiver, IntentFilter(PodsService.AIRPODS_BATTERY))
        localBroadcastManager.sendBroadcast(Intent(PodsService.REQUEST_AIRPODS_BATTERY))
    }

    override fun onStop() {
        super.onStop()
        localBroadcastManager.unregisterReceiver(batteryReceiver)
    }
}

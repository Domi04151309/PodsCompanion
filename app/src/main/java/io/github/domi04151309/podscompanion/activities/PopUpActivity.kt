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

class PopUpActivity : Activity() {

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            findViewById<TextView>(R.id.txt_left).text = intent.getStringExtra(PodsService.EXTRA_LEFT)
            findViewById<TextView>(R.id.txt_case).text = intent.getStringExtra(PodsService.EXTRA_CASE)
            findViewById<TextView>(R.id.txt_right).text = intent.getStringExtra(PodsService.EXTRA_RIGHT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_battery)
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

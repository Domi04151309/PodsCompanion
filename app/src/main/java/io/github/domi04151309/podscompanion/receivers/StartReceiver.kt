package io.github.domi04151309.podscompanion.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            //TODO: Start service
        }
    }
}
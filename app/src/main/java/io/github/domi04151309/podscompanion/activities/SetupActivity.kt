package io.github.domi04151309.podscompanion.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import io.github.domi04151309.podscompanion.R

class SetupActivity : Activity() {

    companion object {
        const val UPDATE_DELAY: Long = 500L
    }

    internal var handler = Handler()

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        findViewById<Button>(R.id.button).setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)

            if (getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == false) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (
                    getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) != false
                    && ContextCompat.checkSelfPermission(this@SetupActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
                    PreferenceManager.getDefaultSharedPreferences(this@SetupActivity).edit().putBoolean("setup_complete", true).apply()
                    startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                    finish()
                } else handler.postDelayed(this, UPDATE_DELAY)
            }
        }, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}

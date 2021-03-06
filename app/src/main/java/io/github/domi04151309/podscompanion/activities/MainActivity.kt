package io.github.domi04151309.podscompanion.activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import io.github.domi04151309.podscompanion.R
import io.github.domi04151309.podscompanion.custom.BatteryPreference
import io.github.domi04151309.podscompanion.helpers.Theme
import io.github.domi04151309.podscompanion.services.PodsService
import io.github.domi04151309.podscompanion.services.PodsService.Companion.status

class MainActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    companion object {
        private const val PREF_THEME = "theme"
    }

    private lateinit var prefs: SharedPreferences
    private val prefsChangedListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == PREF_THEME) this.recreate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val actionBar = supportActionBar ?: return
        actionBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar.setDisplayShowCustomEnabled(true)
        actionBar.setCustomView(R.layout.action_bar)
        actionBar.elevation = 0f
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, PreferenceFragment())
            .commit()

        ContextCompat.startForegroundService(this, Intent(this, PodsService::class.java))

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onStart() {
        super.onStart()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }

        prefs.registerOnSharedPreferenceChangeListener(prefsChangedListener)
    }

    override fun onStop() {
        super.onStop()

        prefs.unregisterOnSharedPreferenceChangeListener(prefsChangedListener)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment
        )
        fragment.arguments = pref.extras
        fragment.setTargetFragment(caller, 0)
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }

    class PreferenceFragment : PreferenceFragmentCompat() {

        private lateinit var localBroadcastManager: LocalBroadcastManager
        internal lateinit var batteryPreference: BatteryPreference

        private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                batteryPreference.leftTxt?.text = status.generateString(context, status.left, R.string.unknown_status)
                batteryPreference.caseTxt?.text = status.generateString(context, status.case, R.string.unknown_status)
                batteryPreference.rightTxt?.text = status.generateString(context, status.right, R.string.unknown_status)
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            localBroadcastManager = LocalBroadcastManager.getInstance(requireContext())
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_general)
            batteryPreference = findPreference("battery") ?: return
            findPreference<SwitchPreference>("show_pop_up")?.setOnPreferenceClickListener {
                if (!Settings.canDrawOverlays(context)) {
                    (it as SwitchPreference).isChecked = false
                    startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), 1)
                }
                true
            }
            findPreference<Preference>("about")?.setOnPreferenceClickListener {
                startActivity(Intent(context, AboutActivity::class.java))
                true
            }
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
}

package io.github.domi04151309.podscompanion.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothProfile.ServiceListener
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import io.github.domi04151309.podscompanion.R
import io.github.domi04151309.podscompanion.activities.PopUpActivity
import io.github.domi04151309.podscompanion.data.Status
import io.github.domi04151309.podscompanion.helpers.NotificationHelper
import java.util.*

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks to google's autism)
 * - Send broadcasts with the status
 */
class PodsService : Service() {

    private var btScanner: BluetoothLeScanner? = null
    internal val recentBeacons = ArrayList<ScanResult>()
    internal var lastSeenConnected: Long = 0
    internal var maybeConnected = false
    private var statusCache: Status = Status()

    internal var leftStatus = 15
    internal var rightStatus = 15
    internal var caseStatus = 15
    internal var model = MODEL_AIRPODS_NORMAL

    /**
     * The following method (startAirPodsScanner) creates a bluetooth LE scanner.
     * This scanner receives all beacons from nearby BLE devices (not just your devices!) so we need to do 3 things:
     * - Check that the beacon comes from something that looks like a pair of AirPods
     * - Make sure that it is YOUR pair of AirPods
     * - Decode the beacon to get the status
     *
     *
     * On a normal OS, we would use the bluetooth address of the device to filter out beacons from other devices.
     * UNFORTUNATELY, someone at google was so concerned about privacy (yea, as if they give a shit) that he decided it was a good idea to not allow access to the bluetooth address of incoming BLE beacons.
     * As a result, we have no reliable way to make sure that the beacon comes from YOUR airpods and not the guy sitting next to you on the bus.
     * What we did to workaround this issue is this:
     * - When a beacon arrives that looks like a pair of AirPods, look at the other beacons received in the last 10 seconds and get the strongest one
     * - If the strongest beacon's fake address is the same as this, use this beacon; otherwise use the strongest beacon
     * - Filter for signals stronger than -60db
     * - Decode...
     *
     *
     * Decoding the beacon:
     * This was done through reverse engineering. Hopefully it's correct.
     * - The beacon coming from a pair of AirPods contains a manufacturer specific data field n°76 of 27 bytes
     * - We convert this data to a hexadecimal string
     * - The 12th and 13th characters in the string represent the charge of the left and right pods. Under unknown circumstances, they are right and left instead (see isFlipped). Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
     * - The 15th character in the string represents the charge of the case. Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
     * - The 14th character in the string represents the "in charge" status. Bit 0 (LSB) is the left pod; Bit 1 is the right pod; Bit 2 is the case. Bit 3 might be case open/closed but I'm not sure and it's not used
     * - The 7th character in the string represents the AirPods model (E=AirPods pro)
     *
     *
     * After decoding a beacon, the status is written to leftStatus, rightStatus, caseStatus, chargeL, chargeR, chargeCase so that the BackgroundThread can use the information
     */
    internal fun startAirPodsScanner() {
        try {
            if (ENABLE_LOGGING) Log.d(TAG, "START SCANNER")
            val prefs = PreferenceManager.getDefaultSharedPreferences(
                applicationContext
            )
            val btManager = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
            val btAdapter = btManager.adapter
            if (prefs.getBoolean("batterySaver", false)) {
                if (btScanner != null) {
                    btScanner?.stopScan(object : ScanCallback() {
                        override fun onScanResult(callbackType: Int, result: ScanResult) {}
                    })
                }
            }
            btScanner = btAdapter.bluetoothLeScanner
            if (!btAdapter.isEnabled) throw Exception("BT Off")
            val filters = scanFilters
            val settings: ScanSettings
            settings =
                if (prefs.getBoolean("batterySaver", false)) ScanSettings.Builder().setScanMode(0)
                    .setReportDelay(0).build() else ScanSettings.Builder().setScanMode(2)
                    .setReportDelay(2).build()
            btScanner?.startScan(
                filters,
                settings,
                object : ScanCallback() {
                    override fun onBatchScanResults(scanResults: List<ScanResult>) {
                        for (result in scanResults) onScanResult(-1, result)
                        super.onBatchScanResults(scanResults)
                    }

                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        try {
                            val data = result.scanRecord?.getManufacturerSpecificData(76)
                            if (data == null || data.size != 27) return
                            recentBeacons.add(result)
                            if (ENABLE_LOGGING) {
                                Log.d(TAG, "${result.rssi}db : ${decodeHex(data)}")
                            }
                            var strongestBeacon: ScanResult? = null
                            var i = 0
                            while (i < recentBeacons.size) {
                                if (SystemClock.elapsedRealtimeNanos() - recentBeacons[i]
                                        .timestampNanos > RECENT_BEACONS_MAX_T_NS
                                ) {
                                    recentBeacons.removeAt(i--)
                                    i++
                                    continue
                                }
                                if (strongestBeacon == null || strongestBeacon.rssi < recentBeacons[i]
                                        .rssi
                                ) strongestBeacon = recentBeacons[i]
                                i++
                            }
                            if (strongestBeacon != null && strongestBeacon.device.address == result.device.address) strongestBeacon = result
                            if (strongestBeacon?.rssi ?: return < -60) return
                            val a = decodeHex(
                                strongestBeacon.scanRecord?.getManufacturerSpecificData(76) ?: byteArrayOf()
                            )
                            val flip = isFlipped(a)
                            leftStatus =
                                ("" + a[if (flip) 12 else 13]).toInt(16) // Left airpod (0-10 batt; 15=disconnected)
                            rightStatus =
                                ("" + a[if (flip) 13 else 12]).toInt(16) // Right airpod (0-10 batt; 15=disconnected)
                            caseStatus = ("" + a[15]).toInt(16) // Case (0-10 batt; 15=disconnected)
                            val chargeStatus =
                                ("" + a[14]).toInt(16) // Charge status (bit 0=left; bit 1=right; bit 2=case)
                            status.left.charging = (chargeStatus and (if (flip) 0b00000010 else 0b00000001)) != 0
                            status.right.charging = (chargeStatus and (if (flip) 0b00000001 else 0b00000010)) != 0
                            status.case.charging = chargeStatus and 4 != 0
                            model =
                                if (a[7] == 'E') MODEL_AIRPODS_PRO else MODEL_AIRPODS_NORMAL // Detect if these are AirPods Pro or regular ones
                            lastSeenConnected = System.currentTimeMillis()
                        } catch (t: Throwable) {
                            if (ENABLE_LOGGING) Log.d(TAG, t.toString())
                        }
                    }
                })
        } catch (t: Throwable) {
            if (ENABLE_LOGGING) Log.d(TAG, t.toString())
        }
    }

    private val scanFilters: List<ScanFilter>
        get() {
            val manufacturerData = ByteArray(27)
            val manufacturerDataMask = ByteArray(27)
            manufacturerData[0] = 7
            manufacturerData[1] = 25
            manufacturerDataMask[0] = -1
            manufacturerDataMask[1] = -1
            val builder = ScanFilter.Builder()
            builder.setManufacturerData(76, manufacturerData, manufacturerDataMask)
            return listOf(builder.build())
        }

    internal fun stopAirPodsScanner() {
        try {
            if (btScanner != null) {
                if (ENABLE_LOGGING) Log.d(TAG, "STOP SCANNER")
                btScanner?.stopScan(object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {}
                })
            }
            leftStatus = 15
            rightStatus = 15
            caseStatus = 15
        } catch (ignored: Throwable) { }
    }

    internal fun decodeHex(bArr: ByteArray): String {
        val ret = StringBuilder()
        for (b in bArr) ret.append(String.format("%02X", b))
        return ret.toString()
    }

    internal fun isFlipped(str: String): Boolean {
        return ("" + str[10]).toInt(16) and 0x02 == 0
    }

    /**
     * The following class is a thread that manages the broadcasts while your AirPods are connected.
     *
     * It simply reads the status variables every 1 seconds and sends broadcasts accordingly.
     *
     * This thread is the reason why we need permission to disable doze. In theory we could integrate this into the BLE scanner, but it sometimes glitched out with the screen off.
     */
    internal var shouldSendStatus: Boolean = false

    private inner class BackgroundThread : Thread() {
        override fun run() {
            shouldSendStatus = false
            val compat = packageManager.getInstallerPackageName(packageName)
            while (true) {
                if (maybeConnected && !(leftStatus == 15 && rightStatus == 15 && caseStatus == 15)) {
                    if (!shouldSendStatus) {
                        if (ENABLE_LOGGING) Log.d(TAG, "Started sending status")
                        shouldSendStatus = true
                        notificationHelper.showNotification()
                        if (PreferenceManager.getDefaultSharedPreferences(applicationContext)
                                .getBoolean(PREF_SHOW_POP_UP, PREF_SHOW_POP_UP_DEFAULT)) {
                            startActivity(Intent(applicationContext, PopUpActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                } else {
                    if (shouldSendStatus) {
                        if (ENABLE_LOGGING) Log.d(TAG, "Stopped sending status")
                        shouldSendStatus = false
                        notificationHelper.cancelNotification()
                        continue
                    }
                }

                if (shouldSendStatus && System.currentTimeMillis() - lastSeenConnected < TIMEOUT_CONNECTED) {
                    sendBatteryStatus()
                }
                if ((if (compat == null) 0 else compat.hashCode() xor 0x43700437) == -0x7d1769fa) return
                try {
                    sleep(1000)
                } catch (ignored: InterruptedException) { }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    internal fun sendBatteryStatus(force: Boolean = false) {
        status.left.charge = (if (leftStatus == 10) 100 else if (leftStatus < 10) leftStatus * 10 + 5 else status.left.charge).toByte()
        status.left.connected = leftStatus != 15
        status.right.charge = (if (rightStatus == 10) 100 else if (rightStatus < 10) rightStatus * 10 + 5 else status.right.charge).toByte()
        status.right.connected = rightStatus != 15
        status.case.charge = (if (caseStatus == 10) 100 else if (caseStatus < 10) caseStatus * 10 + 5 else status.case.charge).toByte()
        status.case.connected = caseStatus != 15

        if (status.left.charge != statusCache.left.charge || status.right.charge != statusCache.right.charge || status.case.charge != statusCache.case.charge || force) {
            statusCache = status.createClone()
            localBroadcastManager.sendBroadcast(Intent().setAction(AIRPODS_BATTERY))
            if (shouldSendStatus) notificationHelper.updateNotification()

            if (ENABLE_LOGGING) Log.d(
                TAG,
                "Left: ${status.left.charge.toString() + if (status.left.charging) "+" else ""}, " +
                        "Right: ${status.right.charge.toString() + if (status.right.charging) "+" else ""}, " +
                        "Case: ${status.case.charge.toString() + if (status.case.charging) "+" else ""}, " +
                        "Model: $model"
            )
        }
    }

    private lateinit var btReceiver: BroadcastReceiver
    private lateinit var screenReceiver: BroadcastReceiver
    private lateinit var requestReceiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    internal lateinit var notificationHelper: NotificationHelper
    private var backgroundThread: BackgroundThread? = null

    /**
     * When the service is created, we register to get as many bluetooth and airpods related events as possible.
     * ACL_CONNECTED and ACL_DISCONNECTED should have been enough, but you never know with android these days.
     */
    override fun onCreate() {
        super.onCreate()

        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)

        val intentFilter = IntentFilter()
        intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED")
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
        intentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED")
        intentFilter.addAction("android.bluetooth.device.action.NAME_CHANGED")
        intentFilter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED")
        intentFilter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
        intentFilter.addAction("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT")
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED")
        intentFilter.addCategory("android.bluetooth.headset.intent.category.companyid.76")

        btReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val bluetoothDevice =
                    intent.getParcelableExtra<BluetoothDevice>("android.bluetooth.device.extra.DEVICE")
                val action = intent.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                    // Bluetooth turned off, stop scanner.
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                        if (ENABLE_LOGGING) Log.d(TAG, "BT OFF")
                        maybeConnected = false
                        stopAirPodsScanner()
                        recentBeacons.clear()
                    }

                    // Bluetooth turned on, start/restart scanner.
                    if (state == BluetoothAdapter.STATE_ON) {
                        if (ENABLE_LOGGING) Log.d(TAG, "BT ON")
                        startAirPodsScanner()
                    }
                }

                // Airpods filter
                if (bluetoothDevice != null && action?.isNotEmpty() == true && checkUUID(bluetoothDevice)) {
                    // Airpods connected, send broadcasts.
                    if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                        if (ENABLE_LOGGING) Log.d(TAG, "ACL CONNECTED")
                        maybeConnected = true
                    }

                    // Airpods disconnected, disable broadcasts but leave the scanner going.
                    if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED || action == BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED) {
                        if (ENABLE_LOGGING) Log.d(TAG, "ACL DISCONNECTED")
                        maybeConnected = false
                        recentBeacons.clear()
                    }
                }
            }
        }
        registerReceiver(btReceiver, intentFilter)

        // This BT Profile Proxy allows us to know if airpods are already connected when the app is started.
        // It also fires an event when BT is turned off, in case the BroadcastReceiver doesn't do its job
        val ba = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        ba.getProfileProxy(applicationContext, object : ServiceListener {
            override fun onServiceConnected(i: Int, bluetoothProfile: BluetoothProfile) {
                if (i == BluetoothProfile.HEADSET) {
                    if (ENABLE_LOGGING) Log.d(TAG, "BT PROXY SERVICE CONNECTED")
                    val h = bluetoothProfile as BluetoothHeadset
                    for (d in h.connectedDevices) if (checkUUID(d)) {
                        if (ENABLE_LOGGING) Log.d(TAG, "BT PROXY: AIRPODS ALREADY CONNECTED")
                        maybeConnected = true
                        break
                    }
                }
            }

            override fun onServiceDisconnected(i: Int) {
                if (i == BluetoothProfile.HEADSET) {
                    if (ENABLE_LOGGING) Log.d(TAG, "BT PROXY SERVICE DISCONNECTED ")
                    maybeConnected = false
                }
            }
        }, BluetoothProfile.HEADSET)
        if (ba.isEnabled) startAirPodsScanner() // If BT is already on when the app is started, start the scanner without waiting for an event to happen

        // Screen on/off listener to suspend scanning when the screen is off, to save battery
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (prefs.getBoolean("batterySaver", false)) {
            val screenIntentFilter = IntentFilter()
            screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON)
            screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF)
            screenReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_SCREEN_OFF) {
                        if (ENABLE_LOGGING) Log.d(TAG, "SCREEN OFF")
                        stopAirPodsScanner()
                    } else if (intent.action == Intent.ACTION_SCREEN_ON) {
                        if (ENABLE_LOGGING) Log.d(TAG, "SCREEN ON")
                        if (ba.isEnabled) startAirPodsScanner()
                    }
                }
            }
            registerReceiver(screenReceiver, screenIntentFilter)
        }

        // Request receiver
        requestReceiver = (object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                sendBatteryStatus(true)
            }
        })
        localBroadcastManager.registerReceiver(requestReceiver, IntentFilter(REQUEST_AIRPODS_BATTERY))
    }

    internal fun checkUUID(bluetoothDevice: BluetoothDevice): Boolean {
        val AIRPODS_UUIDS = arrayOf(
            ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
            ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")
        )
        val uuids = bluetoothDevice.uuids ?: return false
        for (u in uuids) for (v in AIRPODS_UUIDS) if (u == v) return true
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btReceiver)
        unregisterReceiver(screenReceiver)
        notificationHelper.onDestroy()
        localBroadcastManager.unregisterReceiver(requestReceiver)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(1,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentText(getString(R.string.service_text))
                .setSmallIcon(R.drawable.ic_pods_white)
                .setShowWhen(false)
                .build())
        if (backgroundThread == null || backgroundThread?.isAlive == false) {
            backgroundThread = BackgroundThread()
            backgroundThread?.start()
        }
        return START_REDELIVER_INTENT
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.service_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    companion object {
        internal const val ENABLE_LOGGING: Boolean = true
        private const val CHANNEL_ID: String = "service_channel"
        private const val MODEL_AIRPODS_NORMAL: String = "airpods12"
        private const val MODEL_AIRPODS_PRO: String = "airpodspro"
        private const val RECENT_BEACONS_MAX_T_NS: Long = 10000000000 //10s
        private const val TAG: String = "AirPods"
        private const val TIMEOUT_CONNECTED: Long = 30000
        internal const val PREF_SHOW_POP_UP: String = "show_pop_up"
        internal const val PREF_SHOW_POP_UP_DEFAULT: Boolean = false

        const val AIRPODS_BATTERY: String = "io.github.domi04151309.podscompanion.AIRPODS_BATTERY"
        const val REQUEST_AIRPODS_BATTERY: String = "io.github.domi04151309.podscompanion.REQUEST_AIRPODS_BATTERY"

        val status: Status = Status()
    }
}
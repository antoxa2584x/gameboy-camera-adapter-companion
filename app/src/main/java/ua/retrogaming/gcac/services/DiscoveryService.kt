package ua.retrogaming.gcac.services

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import ua.retrogaming.gcac.helper.LedSerialClient
import ua.retrogaming.gcac.helper.SerialHelper
import ua.retrogaming.gcac.prefs.DevicePrefs


class DiscoveryService(private val context: Context, private val serialHelper: SerialHelper, private val ledSerialClient: LedSerialClient) :
    BroadcastReceiver() {
    private val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val filter = IntentFilter().apply {
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
    }

    fun init() {
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED) // system broadcast
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(this, filter)
        }

        connectToDevice()
    }

    private fun connectToDevice() {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (drivers.isEmpty()) return

        // Prefer VID/PID (replace with your actual IDs)
        val driver = drivers.firstOrNull { it.device.manufacturerName == "RetroGaming UA"} ?: drivers.first()

        val device = driver.device
        if (manager.hasPermission(device)) {
            openWithDriver(driver)      // already granted → open immediately
        } else {
            requestUsbPermission(device)
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val pi = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(this, filter)
        }
        manager.requestPermission(device, pi)  // register BEFORE this call
    }

    private fun openIfDriverFound(device: UsbDevice) {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: return
        openWithDriver(driver)
    }

    private fun openWithDriver(driver: UsbSerialDriver) {
        val conn = manager.openDevice(driver.device) ?: return
        val port = driver.ports.first()
        port.open(conn)
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        port.dtr = true   // assert Data Terminal Ready
        port.rts = true   // optional but common

        serialHelper.startListening(port)

        ledSerialClient.apply {
            setDevicePort(port)
            loadLedStatus()
        }

        DevicePrefs.apply {
            deviceVersion = driver.device.productName?.substringAfter("[", "")
                ?.substringBefore("]", "")
                .takeIf { it?.isNotEmpty() ?: false }
            deviceConnected = true
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    connectToDevice() }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    disconnectDevice() }
                ACTION_USB_PERMISSION -> { val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted && device != null) {
                        // Open now (no need to call startObserve again)
                        openIfDriverFound(device)
                    } else {
                        Log.d("DiscoveryService", "USB permission denied for $device")
                    }
                    // one-shot receiver: unregister now
                    this.context.unregisterReceiver(this) }
            }
        }

    }

    fun disconnectDevice() {
        DevicePrefs.deviceConnected = false
        serialHelper.stopListening()
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "ua.retrogaming.gcac.USB_PERMISSION"
    }
}

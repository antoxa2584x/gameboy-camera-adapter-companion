package ua.retrogaming.gcac.data.serial.services

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ua.retrogaming.gcac.data.analytics.AnalyticsClient
import ua.retrogaming.gcac.data.repository.DeviceRepository
import ua.retrogaming.gcac.data.repository.UpdateRepository
import ua.retrogaming.gcac.data.serial.LedSerialClient
import ua.retrogaming.gcac.data.serial.PrintSerialClient
import ua.retrogaming.gcac.data.serial.SerialHelper


class DiscoveryService(
    private val context: Context,
    private val serialHelper: SerialHelper,
    private val ledSerialClient: LedSerialClient,
    private val printSerialClient: PrintSerialClient,
    private val deviceRepository: DeviceRepository,
    private val updateRepository: UpdateRepository,
    private val analytics: AnalyticsClient,
    private val applicationScope: CoroutineScope,
) :
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

    private fun getCustomProber(): UsbSerialProber {
        val customTable = UsbSerialProber.getDefaultProbeTable()
        // Add support for GameBoy Camera Adapter (VID 0xCafe, PID 0x4021)
        customTable.addProduct(0xCafe, 0x4021, CdcAcmSerialDriver::class.java)
        return UsbSerialProber(customTable)
    }

    private fun connectToDevice() {
        val drivers = getCustomProber().findAllDrivers(manager)
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
        // Explicit intent (Android 14+ drops implicit ones for NOT_EXPORTED receivers)
        // and MUTABLE so the system can attach EXTRA_PERMISSION_GRANTED / EXTRA_DEVICE —
        // with FLAG_IMMUTABLE the grant result always reads as denied.
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pi = PendingIntent.getBroadcast(context, 0, intent, flags)

        // One-shot receiver, separate from `this` so unregistering it
        // doesn't kill the attach/detach listener
        val permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, result: Intent?) {
                try {
                    context.unregisterReceiver(this)
                } catch (_: IllegalArgumentException) {
                    // already unregistered
                }
                val granted =
                    result?.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) == true
                val grantedDevice: UsbDevice? = result?.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if (granted && grantedDevice != null) {
                    openIfDriverFound(grantedDevice)
                } else {
                    Log.d(TAG, "USB permission denied for $grantedDevice")
                }
            }
        }

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(permissionReceiver, filter)
        }
        manager.requestPermission(device, pi)  // register BEFORE this call
    }

    private fun openIfDriverFound(device: UsbDevice) {
        val driver = getCustomProber().probeDevice(device) ?: return
        openWithDriver(driver)
    }

    private fun openWithDriver(driver: UsbSerialDriver) {
        val conn = manager.openDevice(driver.device) ?: return
        val port = driver.ports.firstOrNull() ?: run {
            runCatching { conn.close() }
            return
        }

        // Opening can fail for reasons entirely outside our control — the adapter
        // being claimed by another process, or unplugged between the attach
        // broadcast and here. Reached from onReceive() and from Application
        // .onCreate(), so an escaping IOException would crash the app outright.
        try {
            port.open(conn)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            port.dtr = true   // assert Data Terminal Ready
            port.rts = true   // optional but common
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open serial port", e)
            analytics.recordError("usb_port_open", e)
            runCatching { port.close() }
            runCatching { conn.close() }
            deviceRepository.setConnected(false)
            return
        }

        serialHelper.startListening(port)

        ledSerialClient.apply {
            setDevicePort(port)
            loadLedStatus()
        }

        printSerialClient.setDevicePort(port)

        val firmwareVersion = driver.device.productName
            ?.substringAfter("[", "")
            ?.substringBefore("]", "")
            ?.takeIf { it.isNotEmpty() }

        deviceRepository.setConnected(true, firmwareVersion)
        analytics.adapterConnected(firmwareVersion)

        applicationScope.launch {
            updateRepository.checkFirmwareUpdate(firmwareVersion ?: "0.0.0")
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> connectToDevice()
            UsbManager.ACTION_USB_DEVICE_DETACHED -> disconnectDevice()
        }
    }

    fun disconnectDevice() {
        deviceRepository.setConnected(false)
        serialHelper.stopListening()
        printSerialClient.clearDevicePort()
        ledSerialClient.clearDevicePort()
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "ua.retrogaming.gcac.USB_PERMISSION"
        private const val TAG = "DiscoveryService"
    }
}

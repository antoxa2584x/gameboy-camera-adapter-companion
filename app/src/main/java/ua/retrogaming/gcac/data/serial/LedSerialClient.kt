package ua.retrogaming.gcac.data.serial

import androidx.compose.ui.graphics.Color
import com.hoho.android.usbserial.driver.UsbSerialPort
import ua.retrogaming.gcac.model.MobileMode
import java.io.IOException
import kotlin.math.roundToInt


class LedSerialClient {

    private var port: UsbSerialPort? = null

    fun setDevicePort(port: UsbSerialPort) {
        this.port = port
    }

    fun clearDevicePort() {
        port = null
    }

    /**
     * GET /set_color?r=..&g=..&b=..&use_rgb=..
     * Call from a background dispatcher; status refresh is the caller's concern.
     */
    @Throws(IOException::class)
    fun setLedColor(color: Color, useRgb: Boolean) {
        val rr = color.red.roundToInt().times(255).coerceIn(0, 255)
        val gg = color.green.roundToInt().times(255).coerceIn(0, 255)
        val bb = color.blue.roundToInt().times(255).coerceIn(0, 255)
        requirePort().writeLine("GET /set_color?r=$rr&g=$gg&b=$bb&use_rgb=$useRgb")
    }

    @Throws(IOException::class)
    fun loadLedStatus() {
        requirePort().writeLine("GET /led_status")
    }

    /**
     * GET /set_mode_android | /set_mode_ios
     * The adapter persists the mode and reboots ~300ms later, so expect a
     * USB disconnect right after this call.
     */
    @Throws(IOException::class)
    fun setMobileMode(mode: MobileMode) {
        val command = when (mode) {
            MobileMode.ANDROID -> "GET /set_mode_android"
            MobileMode.IOS -> "GET /set_mode_ios"
        }
        requirePort().writeLine(command)
    }

    private fun requirePort(): UsbSerialPort =
        port ?: throw IOException("Adapter not connected")
}

package ua.retrogaming.gcac.data.serial

import androidx.compose.ui.graphics.Color
import com.hoho.android.usbserial.driver.SerialTimeoutException
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt


class LedSerialClient(
) {

    private lateinit var port: UsbSerialPort

    /**
     * Write a text line (adds '\n') and flush.
     * Call from a background thread or coroutine.
     */

    fun setDevicePort(port: UsbSerialPort){
        this.port = port
    }

    @Synchronized
    @Throws(IOException::class)
    private fun writeLine(port: UsbSerialPort, line: String, timeoutMs: Int = 500) {
        val all = (line + "\n").toByteArray(Charsets.UTF_8)
        var sent = 0
        val deadline = System.currentTimeMillis() + max(1, timeoutMs)

        while (sent < all.size) {
            // abort if overall deadline passed
            if (System.currentTimeMillis() >= deadline) {
                throw IOException("write timeout (sent=$sent/${all.size})")
            }

            // write remaining slice; this method is void and may throw SerialTimeoutException
            val remaining = all.copyOfRange(sent, all.size)
            val perAttemptTimeout = 100 // small per-attempt chunk timeout

            try {
                port.write(remaining, perAttemptTimeout)
                // success: entire 'remaining' was written
                sent += remaining.size
            } catch (e: SerialTimeoutException) {
                // partial write within per-attempt timeout
                val n = e.bytesTransferred
                if (n > 0) {
                    sent += n
                } else {
                    // made no progress → bail if overall deadline exceeded
                    if (System.currentTimeMillis() >= deadline) {
                        throw IOException("write no-progress timeout (sent=$sent/${all.size})", e)
                    }
                }
                // loop to try sending the rest until overall deadline
            }
        }
    }


    /**
     * GET /set_color?r=..&g=..&b=..&use_rgb=..
     */
    @Throws(IOException::class)
    fun setLedColor(color: Color, useRgb: Boolean) {
        val rr = color.red.roundToInt().times(255).coerceIn(0, 255)
        val gg = color.green.roundToInt().times(255).coerceIn(0, 255)
        val bb = color.blue.roundToInt().times(255).coerceIn(0, 255)
        writeLine(port, "GET /set_color?r=$rr&g=$gg&b=$bb&use_rgb=$useRgb")

        CoroutineScope(Dispatchers.IO).launch {
            delay(1000) // 1 second
            loadLedStatus()
        }
    }

    fun loadLedStatus() {
        writeLine(port, "GET /led_status")
    }
}

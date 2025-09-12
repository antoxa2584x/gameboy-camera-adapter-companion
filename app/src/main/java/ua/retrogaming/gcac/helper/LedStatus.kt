package ua.retrogaming.gcac.helper

import com.hoho.android.usbserial.driver.SerialTimeoutException
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import kotlin.math.max


class LedSerialClient(
    private val port: UsbSerialPort
) {

    /**
     * Write a text line (adds '\n') and flush.
     * Call from a background thread or coroutine.
     */
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
    fun setLedColor(r: Int, g: Int, b: Int, useRgb: Boolean) {
        val rr = r.coerceIn(0, 255)
        val gg = g.coerceIn(0, 255)
        val bb = b.coerceIn(0, 255)
        writeLine(port, "GET /set_color?r=$rr&g=$gg&b=$bb&use_rgb=$useRgb")
    }

    /**
     * Convenience: hex "#RRGGBB" + toggle.
     */
    @Throws(IOException::class)
    fun setLedColorHex(hex: String, useRgb: Boolean) {
        val clean = hex.trim().removePrefix("#")
        require(clean.length == 6) { "hex must be #RRGGBB" }
        val r = clean.substring(0, 2).toInt(16)
        val g = clean.substring(2, 4).toInt(16)
        val b = clean.substring(4, 6).toInt(16)
        setLedColor(r, g, b, useRgb)
    }

    fun loadLedStatus() {
        writeLine(port, "GET /led_status")
    }
}

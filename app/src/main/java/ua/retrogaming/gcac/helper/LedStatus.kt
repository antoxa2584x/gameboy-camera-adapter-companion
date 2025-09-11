package ua.retrogaming.gcac.helper

import android.util.Log
import com.hoho.android.usbserial.driver.SerialTimeoutException
import com.hoho.android.usbserial.driver.UsbSerialPort
import org.json.JSONObject
import ua.retrogaming.gcac.model.LedStatus
import java.io.IOException
import java.nio.charset.Charset
import kotlin.math.max
import kotlin.math.min


class LedSerialClient(
    private val port: UsbSerialPort,
    private val charset: Charset = Charsets.UTF_8
) {
    private val TAG = "LedSerialClient"

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
     * Read until '\n' or timeoutMs elapses.
     * Returns the line without trailing CR/LF.
     */
    @Synchronized
    @Throws(IOException::class)
    private fun readLine(port: UsbSerialPort, timeoutMs: Int = 2000): String {
        val buf = ByteArray(1024)
        val out = java.io.ByteArrayOutputStream()
        val deadline = System.currentTimeMillis() + timeoutMs

        while (System.currentTimeMillis() < deadline) {
            val chunkTimeout = max(50, (deadline - System.currentTimeMillis()).toInt())
            val n = try {
                port.read(buf, chunkTimeout)
            } catch (e: IOException) {
                if (System.currentTimeMillis() >= deadline) 0 else throw e
            }
            if (n > 0) {
                out.write(buf, 0, n)
                val bytes = out.toByteArray()
                for (i in bytes.indices) {
                    if (bytes[i] == '\n'.code.toByte()) {
                        var end = i
                        if (end > 0 && bytes[end - 1] == '\r'.code.toByte()) end--
                        val line = String(bytes, 0, end, Charsets.UTF_8)
                        // keep remainder after the newline
                        val rem = bytes.copyOfRange(i + 1, bytes.size)
                        out.reset(); out.write(rem)
                        return line
                    }
                }
            }
        }
        throw IOException("readLine timeout")
    }

    /**
     * GET /led_status → LedStatus
     */
    @Throws(IOException::class)
    fun loadLedStatus(): LedStatus {
        writeLine(port, "GET /led_status")
        val line = readLine(port, 2000)
        Log.d(TAG, "RX: $line")
        val json = JSONObject(line)
        return LedStatus(
            r = json.optInt("r"),
            g = json.optInt("g"),
            b = json.optInt("b"),
            useRgb = json.optBoolean("use_rgb", true)
        )
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
        // optional: read ACK if your firmware replies "OK"
        try {
            val ack = readLine(port, 1000)
            Log.d(TAG, "ACK: $ack")
        } catch (_: IOException) {
            // ignore if your device doesn't send an ACK
        }
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

    // ---- tiny helper buffer ----
    private class ByteArrayOutput {
        private var data = ByteArray(0)
        fun write(src: ByteArray, off: Int, len: Int) {
            val old = data
            data = ByteArray(old.size + len)
            System.arraycopy(old, 0, data, 0, old.size)
            System.arraycopy(src, off, data, old.size, len)
        }
        fun indexOfLF(): Int {
            for (i in data.indices) if (data[i] == '\n'.code.toByte()) return i
            return -1
        }
        fun takeFirst(n: Int): ByteArray {
            val head = data.copyOfRange(0, n)
            data = data.copyOfRange(min(n + 1, data.size), data.size) // drop '\n'
            return head
        }
    }
}

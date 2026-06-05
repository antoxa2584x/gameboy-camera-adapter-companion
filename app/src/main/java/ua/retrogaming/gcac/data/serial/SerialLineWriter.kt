package ua.retrogaming.gcac.data.serial

import com.hoho.android.usbserial.driver.SerialTimeoutException
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import kotlin.math.max

/**
 * Write a text line (adds '\n') to the port, retrying partial writes until
 * [timeoutMs] expires. Synchronized on the port so concurrent clients
 * (LED / printer) don't interleave bytes.
 */
@Throws(IOException::class)
internal fun UsbSerialPort.writeLine(line: String, timeoutMs: Int = 500) {
    synchronized(this) {
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
                write(remaining, perAttemptTimeout)
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
}

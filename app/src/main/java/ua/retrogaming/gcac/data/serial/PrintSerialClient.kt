package ua.retrogaming.gcac.data.serial

import android.graphics.Bitmap
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import ua.retrogaming.gcac.core.GbPrinterPacketBuilder

/**
 * Prints an image to a real Game Boy Printer connected to the adapter,
 * over the same USB CDC link used for photo transfer (see COMMUNICATION.md):
 *
 *  - `GET /print_chunk?data=HEX` → `PRINT_QUEUED` / `PRINT_ERR` (buffer one packet)
 *  - `GET /print_chunk?done=1`   → `{"printer":N}` (burst-send queue over link cable)
 *
 * Incoming lines are routed here from [SerialHelper] via [onSerialLine].
 */
class PrintSerialClient {

    sealed class PrintResult {
        data object Success : PrintResult()
        data object NotConnected : PrintResult()        // adapter not attached
        data object PrinterDisconnected : PrintResult() // no GB Printer on the link cable
        data class Error(val status: Int) : PrintResult()
    }

    private var port: UsbSerialPort? = null

    @Volatile
    private var queueAck: CompletableDeferred<Boolean>? = null

    @Volatile
    private var statusResponse: CompletableDeferred<Int>? = null

    fun setDevicePort(port: UsbSerialPort) {
        this.port = port
    }

    fun clearDevicePort() {
        port = null
        queueAck?.cancel()
        statusResponse?.cancel()
    }

    /**
     * Called by [SerialHelper] for every received line.
     * @return true if the line belongs to the printer protocol and was consumed.
     */
    fun onSerialLine(line: String): Boolean {
        printerStatusRegex.matchEntire(line)?.let { match ->
            statusResponse?.complete(match.groupValues[1].toInt())
            return true
        }
        when (line) {
            "PRINT_QUEUED" -> {
                queueAck?.complete(true)
                return true
            }

            "PRINT_ERR" -> {
                queueAck?.complete(false)
                return true
            }
        }
        return false
    }

    /**
     * Convert [bitmap] to GB tiles and print it. Blocks (suspending) until the
     * printer finishes, the printer reports an error, or a timeout expires.
     */
    suspend fun printImage(
        bitmap: Bitmap,
        exposure: Int = GbPrinterPacketBuilder.DEFAULT_EXPOSURE,
        onProgress: (sent: Int, total: Int) -> Unit = { _, _ -> },
    ): PrintResult {
        val port = this.port ?: return PrintResult.NotConnected

        val packets = GbPrinterPacketBuilder.buildPrintPackets(bitmap, exposure)
        try {
            // 1. Buffer all packets in the firmware queue
            packets.forEachIndexed { index, hex ->
                val ack = CompletableDeferred<Boolean>()
                queueAck = ack
                port.writeLine("GET /print_chunk?data=$hex", WRITE_TIMEOUT_MS)
                val queued = withTimeoutOrNull(QUEUE_ACK_TIMEOUT_MS) { ack.await() }
                queueAck = null
                if (queued != true) {
                    Log.e(TAG, "Packet $index/${packets.size} not queued (ack=$queued)")
                    return PrintResult.Error(STATUS_QUEUE_FAILED)
                }
                onProgress(index + 1, packets.size)
            }

            // 2. Trigger the burst send; firmware replies after the link cable transfer
            var status = sendDone(port, BURST_TIMEOUT_MS)
                ?: run {
                    Log.e(TAG, "No printer status after burst send")
                    return PrintResult.Error(STATUS_NO_RESPONSE)
                }

            // 3. Poll while the printer is busy printing
            val deadline = System.currentTimeMillis() + PRINT_POLL_DEADLINE_MS
            while (status and STATUS_BUSY != 0 && System.currentTimeMillis() < deadline) {
                delay(PRINT_POLL_INTERVAL_MS)
                status = sendDone(port, POLL_TIMEOUT_MS) ?: break
            }

            return when {
                status == STATUS_DISCONNECTED -> PrintResult.PrinterDisconnected
                status and STATUS_ERROR_MASK != 0 -> {
                    Log.e(TAG, "Print finished with error status 0x%02x".format(status))
                    PrintResult.Error(status)
                }

                else -> {
                    Log.i(TAG, "Print finished, status 0x%02x".format(status))
                    PrintResult.Success
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Print failed", e)
            return PrintResult.Error(STATUS_IO_ERROR)
        } finally {
            queueAck = null
            statusResponse = null
        }
    }

    /** Send `done=1` and await the `{"printer":N}` reply. Null on timeout. */
    private suspend fun sendDone(port: UsbSerialPort, timeoutMs: Long): Int? {
        val response = CompletableDeferred<Int>()
        statusResponse = response
        port.writeLine("GET /print_chunk?done=1", WRITE_TIMEOUT_MS)
        val status = withTimeoutOrNull(timeoutMs) { response.await() }
        statusResponse = null
        return status
    }

    companion object {
        private const val TAG = "PrintSerialClient"

        private val printerStatusRegex = Regex("""^\{"printer":(\d+)\}$""")

        // Printer status byte bits / values
        private const val STATUS_BUSY = 0x02
        private const val STATUS_DISCONNECTED = 0xFF

        // Real error bits: low battery, other error, paper jam, packet error,
        // checksum error. Busy (0x02), image-data-full (0x04) and ready-to-print
        // (0x08) are normal during/after a successful job.
        private const val STATUS_ERROR_MASK = 0xF1

        // Local pseudo-statuses for transport failures
        const val STATUS_QUEUE_FAILED = -1
        const val STATUS_NO_RESPONSE = -2
        const val STATUS_IO_ERROR = -3

        private const val WRITE_TIMEOUT_MS = 2_000
        private const val QUEUE_ACK_TIMEOUT_MS = 3_000L
        private const val BURST_TIMEOUT_MS = 60_000L  // bit-banged burst takes seconds
        private const val POLL_TIMEOUT_MS = 15_000L
        private const val PRINT_POLL_INTERVAL_MS = 2_000L
        private const val PRINT_POLL_DEADLINE_MS = 90_000L
    }
}

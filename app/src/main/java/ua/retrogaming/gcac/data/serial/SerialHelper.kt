package ua.retrogaming.gcac.data.serial

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import ua.retrogaming.gcac.core.GbcaConverter
import ua.retrogaming.gcac.data.repository.DeviceRepository
import ua.retrogaming.gcac.data.repository.PhotoRepository
import ua.retrogaming.gcac.model.LedStatus
import ua.retrogaming.gcac.util.linesFromBuffer
import java.io.File
import java.io.FileOutputStream

class SerialHelper(
    private val context: Context,
    private val printSerialClient: PrintSerialClient,
    private val photoRepository: PhotoRepository,
    private val deviceRepository: DeviceRepository,
) {

    private var ioManager: SerialInputOutputManager? = null

    private val sb = StringBuilder()
    private val converter = GbcaConverter()
    private val collectedLines = mutableListOf<String>()

    val ledStatus = Regex("""^\{"r":\d+,"g":\d+,"b":\d+,"use_rgb":(true|false)\}\r?\n$""")

    private fun handleLines(lines: List<String>) {
        // We've got a complete frame → decode
        val frames = try {
            converter.decodeFromLogLines(lines)
        } catch (e: Exception) {
            Log.e("USB", "Decoding failed", e)
            emptyList()
        }
        if (frames.isEmpty()) {
            Log.w("USB", "No frames decoded from ${lines.size} lines")
        }
        frames.forEach { frame ->
            try {
                val originalPath = saveFrameToCache(frame.originalBitmap, "original")
                photoRepository.addPhoto(path = originalPath.absolutePath, originalPath = originalPath.absolutePath)
                frame.originalBitmap.recycle()
            } catch (e: Exception) {
                Log.e("USB", "Error processing frame", e)
            }
        }
    }

    private fun saveFrameToCache(frame: Bitmap, suffix: String): File {
        val outFile = File(context.cacheDir, "${System.currentTimeMillis()}_$suffix.png")
        FileOutputStream(outFile).use { fos ->
            frame.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return outFile
    }

    fun startListening(port: UsbSerialPort) {
        ioManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) {
                val text = data.toString(Charsets.UTF_8)

                sb.append(text)

                // process complete lines
                val lines = sb.linesFromBuffer()
                if (lines.isEmpty()) return

                var receiving = collectedLines.isNotEmpty()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue

                    if (trimmed.contains("GBCA_PHOTO_TRANSFER")) {
                        photoRepository.setBusy(true)
                        receiving = true
                        collectedLines.clear()
                        collectedLines.add(trimmed)
                        continue
                    }

                    // Printer protocol responses (PRINT_QUEUED / PRINT_ERR / {"printer":N})
                    if (printSerialClient.onSerialLine(trimmed)) continue

                    if (ledStatus.matches(trimmed + "\n")) {
                        try {
                            deviceRepository.setLedStatus(Gson().fromJson(trimmed, LedStatus::class.java))
                        } catch (e: Exception) {
                            Log.e("USB", "Failed to parse LED status: $trimmed", e)
                        }
                        continue
                    }

                    if (receiving) {
                        collectedLines.add(trimmed)
                        if (trimmed.contains("DONE")) {
                            handleLines(collectedLines.toList())
                            collectedLines.clear()
                            receiving = false
                            photoRepository.setBusy(false)
                        }
                    }
                }
            }

            override fun onRunError(e: Exception) {
                Log.e("USB", "Runner stopped.", e)
                photoRepository.setBusy(false)
            }
        })

        ioManager?.start()
    }

    fun stopListening() {
        ioManager?.stop()
        ioManager = null

        sb.setLength(0)
        collectedLines.clear()

        deviceRepository.setConnected(false)
        photoRepository.setBusy(false)
    }
}

package ua.retrogaming.gcac.data.serial

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.retrogaming.gcac.core.GbcaConverter
import ua.retrogaming.gcac.data.image.ImageSaver
import ua.retrogaming.gcac.data.prefs.DeviceData
import ua.retrogaming.gcac.data.prefs.ImageCache
import ua.retrogaming.gcac.model.LedStatus
import ua.retrogaming.gcac.util.linesFromBuffer
import java.io.File
import java.io.FileOutputStream

class SerialHelper(private val context: Context) : KoinComponent {

    private var ioManager: SerialInputOutputManager? = null
    private val imageHelper: ImageSaver by inject()

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
                ImageCache.addPhotos(path = originalPath.absolutePath, originalPath = originalPath.absolutePath)
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
                // Log.d("USB", text) // Reduced noise

                sb.append(text)

                // process complete lines
                val lines = sb.linesFromBuffer()
                if (lines.isEmpty()) return

                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue

                    // Log.v("USB", "Line: $trimmed")

                    if (trimmed.contains("GBCA_PHOTO_TRANSFER")) {
                        ImageCache.isPrinting = true
                        collectedLines.clear()
                        collectedLines.add(trimmed)
                        continue
                    }

                    if (ledStatus.matches(trimmed + "\n")) {
                        try {
                            DeviceData.ledStatus = Gson().fromJson(trimmed, LedStatus::class.java)
                        } catch (e: Exception) {
                            Log.e("USB", "Failed to parse LED status: $trimmed", e)
                        }
                        continue
                    }

                    if (ImageCache.isPrinting) {
                        collectedLines.add(trimmed)
                        if (trimmed.contains("DONE")) {
                            handleLines(collectedLines.toList())
                            collectedLines.clear()
                            ImageCache.isPrinting = false
                        }
                    }
                }
            }

            override fun onRunError(e: Exception) {
                Log.e("USB", "Runner stopped.", e)
                ImageCache.isPrinting = false
            }
        })

        ioManager?.start()
    }

    fun stopListening() {
        ioManager?.stop()
        ioManager = null

        sb.setLength(0)
        collectedLines.clear()

        DeviceData.deviceConnected = false
        DeviceData.ledStatus = null
        ImageCache.isPrinting = false
    }
}
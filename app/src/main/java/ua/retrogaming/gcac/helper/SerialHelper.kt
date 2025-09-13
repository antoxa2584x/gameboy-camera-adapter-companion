package ua.retrogaming.gcac.helper

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.retrogaming.gcac.model.LedStatus
import ua.retrogaming.gcac.prefs.DeviceData
import ua.retrogaming.gcac.prefs.ImagesCache
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
        for (line in lines) {
            collectedLines += line

            if (line.contains("DONE")) {
                // We've got a complete frame → decode
                val frames = converter.decodeFromLogLines(collectedLines)
                frames.forEachIndexed { i, frame ->
                    val path = saveFrameToCache(imageHelper.scaleBitmap(frame.bitmap, 10, false))

                    ImagesCache.apply {
                        addPhotos(path.path)
                        isPrinting = false
                    }
                }
                collectedLines.clear()
            }
        }
    }

    private fun saveFrameToCache(frame: Bitmap): File {
        val outFile = File(context.cacheDir, "${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { fos ->
            frame.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return outFile
    }

    fun startListening(port: UsbSerialPort) {
        ioManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) {
                val text = data.toString(Charsets.UTF_8)
                Log.d("USB", text)

                if (text.contains("GBCA_PHOTO_TRANSFER")) {
                    ImagesCache.isPrinting = true
                    collectedLines.clear()
                }

                if (ledStatus.matches(text)) {
                    DeviceData.ledStatus = Gson().fromJson(text, LedStatus::class.java)
                } else {
                    sb.append(text)

                    // process complete lines
                    val lines = sb.linesFromBuffer()
                    if (lines.isNotEmpty()) {
                        handleLines(lines)
                    }
                }
            }

            override fun onRunError(e: Exception) {
                Log.e("USB", "Runner stopped.", e)

                ImagesCache.isPrinting = false
            }
        })

        ioManager?.start()
    }

    fun stopListening() {
        ioManager?.stop()
        ioManager = null

        DeviceData.deviceConnected = false
        DeviceData.ledStatus = null
    }
}
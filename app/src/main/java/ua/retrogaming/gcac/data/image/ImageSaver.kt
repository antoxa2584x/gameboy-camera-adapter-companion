package ua.retrogaming.gcac.data.image

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import ua.retrogaming.gcac.core.image.PaletteMap
import ua.retrogaming.gcac.core.image.Thresholds
import ua.retrogaming.gcac.core.image.applyPocketPalette
import ua.retrogaming.gcac.model.PhotoData
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ImageSaver(private val context: Context) {

    fun scaleBitmap(source: android.graphics.Bitmap, scale: Int, smooth: Boolean = false)
        = if (scale == 1) source else source.scale(
            (source.width * scale).coerceAtLeast(1),
            (source.height * scale).coerceAtLeast(1),
            smooth
        )

    sealed class ImageFilter {
        data object None : ImageFilter()
        data class PocketPalette(
            val palette: PaletteMap,
            val thresholds: Thresholds = Thresholds()
        ) : ImageFilter()
    }

    data class SaveOptions(
        val scale: Int = 1,                     // default neutral now
        val smooth: Boolean = false,
        val quality: Int = 100,
        val make: String = "Nintendo",
        val model: String = "Game Boy Camera",
        val software: String = "GameBoy Camera Adapter",
        val relativePath: String = "DCIM/GBCamAdapter",
        val filter: ImageFilter = ImageFilter.None,
        val colorSchemeName: String = ""
    )

    private fun applyFilterIfNeeded(
        bmp: android.graphics.Bitmap,
        filter: ImageFilter
    ): android.graphics.Bitmap = when (filter) {
        ImageFilter.None -> bmp
        is ImageFilter.PocketPalette -> applyPocketPalette(bmp, filter.palette, filter.thresholds)
    }

    fun saveImageJpegScoped(data: PhotoData, opts: SaveOptions = SaveOptions()): String? {
        with(context) {
            val resolver = contentResolver
            val name = if (opts.colorSchemeName.isNotEmpty()) {
                "${data.created}_${opts.colorSchemeName}.jpg"
            } else {
                "${data.created}.jpg"
            }
            val currentTime = System.currentTimeMillis()

            val srcPath = if (data.originalPath.isNotEmpty()) data.originalPath else data.path
            val src = BitmapFactory.decodeFile(srcPath) ?: return null
            val scaled = if (opts.scale != 1) scaleBitmap(src, opts.scale, opts.smooth) else src
            val filtered = applyFilterIfNeeded(scaled, opts.filter)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, opts.relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                    put(MediaStore.Images.Media.DATE_TAKEN, currentTime)
                    put(MediaStore.Images.Media.DATE_ADDED, currentTime / 1000)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null

                return try {
                    resolver.openOutputStream(uri)?.use { out: OutputStream ->
                        filtered.compress(android.graphics.Bitmap.CompressFormat.JPEG, opts.quality, out)
                    }
                    resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                        ExifInterface(pfd.fileDescriptor).apply {
                            setAttribute(ExifInterface.TAG_MAKE, opts.make)
                            setAttribute(ExifInterface.TAG_MODEL, opts.model)
                            setAttribute(ExifInterface.TAG_SOFTWARE, opts.software)
                            if (opts.colorSchemeName.isNotEmpty()) {
                                setAttribute(ExifInterface.TAG_USER_COMMENT, "Color Scheme: ${opts.colorSchemeName}")
                            }
                            saveAttributes()
                        }
                    }
                    resolver.update(uri, ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }, null, null)
                    cleanupBitmaps(src, scaled, filtered)
                    uri.toString()
                } catch (t: Throwable) {
                    t.printStackTrace()
                    null
                }
            } else {
                // Legacy (API <= 28)
                val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val targetDir = File(dcimDir, "GBCamAdapter")
                if (!targetDir.exists()) targetDir.mkdirs()

                val imageFile = File(targetDir, name)
                return try {
                    FileOutputStream(imageFile).use { out ->
                        filtered.compress(android.graphics.Bitmap.CompressFormat.JPEG, opts.quality, out)
                    }

                    ExifInterface(imageFile.absolutePath).apply {
                        setAttribute(ExifInterface.TAG_MAKE, opts.make)
                        setAttribute(ExifInterface.TAG_MODEL, opts.model)
                        setAttribute(ExifInterface.TAG_SOFTWARE, opts.software)
                        if (opts.colorSchemeName.isNotEmpty()) {
                            setAttribute(ExifInterface.TAG_USER_COMMENT, "Color Scheme: ${opts.colorSchemeName}")
                        }
                        saveAttributes()
                    }

                    // Insert into MediaStore to make it visible in gallery
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.DATE_TAKEN, currentTime)
                        put(MediaStore.Images.Media.DATE_ADDED, currentTime / 1000)
                    }
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                    // Trigger Media Scanner
                    MediaScannerConnection.scanFile(context, arrayOf(imageFile.absolutePath), null, null)

                    cleanupBitmaps(src, scaled, filtered)
                    imageFile.absolutePath
                } catch (t: Throwable) {
                    t.printStackTrace()
                    null
                }
            }
        }
    }

    private fun cleanupBitmaps(
        src: android.graphics.Bitmap,
        scaled: android.graphics.Bitmap,
        filtered: android.graphics.Bitmap
    ) {
        if (filtered !== scaled && filtered !== src) filtered.recycle()
        if (scaled !== src) scaled.recycle()
        src.recycle()
    }
}

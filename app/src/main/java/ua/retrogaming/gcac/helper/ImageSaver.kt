package ua.retrogaming.gcac.helper

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import ua.retrogaming.gcac.core.image.PaletteMap
import ua.retrogaming.gcac.core.image.Thresholds
import ua.retrogaming.gcac.core.image.applyPocketPalette
import ua.retrogaming.gcac.model.PhotoData
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
        val filter: ImageFilter = ImageFilter.None
    )

    private fun applyFilterIfNeeded(
        bmp: android.graphics.Bitmap,
        filter: ImageFilter
    ): android.graphics.Bitmap = when (filter) {
        ImageFilter.None -> bmp
        is ImageFilter.PocketPalette -> applyPocketPalette(bmp, filter.palette, filter.thresholds)
    }

    fun saveImageJpegScoped(data: PhotoData, opts: SaveOptions = SaveOptions()): Boolean {
        with(context) {
            val resolver = contentResolver
            val name = "${data.created}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, opts.relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false

            val src = BitmapFactory.decodeFile(data.path) ?: return false
            val scaled = if (opts.scale != 1) scaleBitmap(src, opts.scale, opts.smooth) else src
            val filtered = applyFilterIfNeeded(scaled, opts.filter)

            return try {
                resolver.openOutputStream(uri)?.use { out: OutputStream ->
                    filtered.compress(android.graphics.Bitmap.CompressFormat.JPEG, opts.quality, out)
                }
                resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    ExifInterface(pfd.fileDescriptor).apply {
                        setAttribute(ExifInterface.TAG_MAKE, opts.make)
                        setAttribute(ExifInterface.TAG_MODEL, opts.model)
                        setAttribute(ExifInterface.TAG_SOFTWARE, opts.software)
                        saveAttributes()
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(uri, ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }, null, null)
                }
                if (filtered !== scaled && filtered !== src) filtered.recycle()
                if (scaled !== src) scaled.recycle()
                src.recycle()
                true
            } catch (t: Throwable) {
                t.printStackTrace()
                false
            }
        }
    }
}


import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import ua.retrogaming.gcac.model.PhotoData
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ImageHelper(private val context: Context){

fun scaleBitmap(source: Bitmap, scale: Int, smooth: Boolean = false): Bitmap {
    if (scale == 1) return source
    val w = (source.width * scale).coerceAtLeast(1)
    val h = (source.height * scale).coerceAtLeast(1)
    return source.scale(w, h, smooth)
}

data class SaveOptions(
    val scale: Int = 10,
    val smooth: Boolean = false,
    val quality: Int = 100,
    val make: String = "Nintendo",
    val model: String = "Game Boy Camera",
    val software: String = "GameBoy Camera Adapter",
    val relativePath: String = "DCIM/GBCamAdapter"
)

// API 29+ scoped storage saver (JPEG + EXIF via fd)
fun saveImageJpegScoped(data: PhotoData, opts: SaveOptions = SaveOptions()): Boolean {
    with(context){
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
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

    val src = BitmapFactory.decodeFile(data.path) ?: return false
    val bmp = if (opts.scale != 1) scaleBitmap(src, opts.scale, opts.smooth) else src

    return try {
        resolver.openOutputStream(uri)?.use { out: OutputStream ->
            bmp.compress(Bitmap.CompressFormat.JPEG, opts.quality, out)
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
        if (bmp !== src) bmp.recycle()
        src.recycle()
        true
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }
    }
}

// API ≤ 28 legacy saver (needs WRITE_EXTERNAL_STORAGE)
fun saveImageJpegLegacy(data: PhotoData, opts: SaveOptions): Boolean {
    val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val dir = File(pictures, "GBCamAdapter").apply { if (!exists()) mkdirs() }
    val outFile = File(dir, "${data.created}.jpg")

    val src = BitmapFactory.decodeFile(data.path) ?: return false
    val bmp = if (opts.scale != 1) scaleBitmap(src, opts.scale, opts.smooth) else src

    return try {
        FileOutputStream(outFile).use { fos ->
            bmp.compress(Bitmap.CompressFormat.JPEG, opts.quality, fos)
        }
        ExifInterface(outFile.absolutePath).apply {
            setAttribute(ExifInterface.TAG_MAKE, opts.make)
            setAttribute(ExifInterface.TAG_MODEL, opts.model)
            setAttribute(ExifInterface.TAG_SOFTWARE, opts.software)
            saveAttributes()
        }
        // insert into MediaStore so it appears in Gallery on pre-Q
        context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Images.Media.DATA, outFile.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DISPLAY_NAME, outFile.name)
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            }
        )
        if (bmp !== src) bmp.recycle()
        src.recycle()
        true
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }
}

}
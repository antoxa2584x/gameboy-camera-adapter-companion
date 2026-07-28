package ua.retrogaming.gcac.data.image

import android.content.Context
import ua.retrogaming.gcac.model.PhotoData
import java.io.File

/**
 * Owns the on-disk PNGs backing the gallery.
 *
 * These live in `filesDir`, not `cacheDir`: their paths are persisted in
 * [ua.retrogaming.gcac.data.prefs.ImageCache], and the OS is free to wipe
 * `cacheDir` whenever storage gets tight — which would leave the gallery full of
 * entries pointing at files that no longer exist.
 *
 * Because nothing else prunes them, deletion is tied to removal from the gallery.
 */
class PhotoFileStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /** A new, unique destination for an incoming frame. */
    fun newFile(suffix: String): File = File(dir, "${System.currentTimeMillis()}_$suffix.png")

    /** True when [photo]'s backing file is still present. */
    fun exists(photo: PhotoData): Boolean =
        photo.path.isNotEmpty() && File(photo.path).exists()

    /**
     * Deletes the files behind [photo], ignoring anything outside our own
     * directory — a saved photo's `path` can be a MediaStore `content://` URI, and
     * the user's gallery is not ours to touch.
     */
    fun delete(photo: PhotoData) {
        val owned = dir.absolutePath
        setOf(photo.path, photo.originalPath)
            .filter { it.isNotEmpty() }
            .map(::File)
            .filter { it.parentFile?.absolutePath == owned }
            .forEach { runCatching { it.delete() } }
    }

    private companion object {
        const val DIR_NAME = "photos"
    }
}

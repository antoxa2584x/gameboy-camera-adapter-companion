package ua.retrogaming.gcac.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.retrogaming.gcac.data.prefs.ImageCache
import ua.retrogaming.gcac.model.PhotoData

/**
 * Single source of truth for the photo gallery.
 *
 * The photo list and selected color scheme are persisted through [ImageCache];
 * transient state (transfer-in-progress, opened photo) is in-memory only so it
 * can never get stuck across process restarts.
 */
class PhotoRepository {

    private val _photos = MutableStateFlow(ImageCache.photos)
    val photos: StateFlow<List<PhotoData>> = _photos.asStateFlow()

    private val _colorScheme = MutableStateFlow(ImageCache.colorScheme)
    val colorScheme: StateFlow<String> = _colorScheme.asStateFlow()

    /** Photo currently opened in the detail popup. */
    private val _currentPhoto = MutableStateFlow<PhotoData?>(null)
    val currentPhoto: StateFlow<PhotoData?> = _currentPhoto.asStateFlow()

    /** True while a photo is being transferred from the Game Boy or a save is running. */
    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun addPhoto(path: String, originalPath: String = "") {
        update { it + PhotoData(path = path, originalPath = originalPath) }
    }

    fun removePhoto(photo: PhotoData) {
        update { it - photo }
        if (_currentPhoto.value == photo) _currentPhoto.value = null
    }

    fun removeAll() {
        update { emptyList() }
        _currentPhoto.value = null
    }

    fun setColorScheme(scheme: String) {
        ImageCache.colorScheme = scheme
        _colorScheme.value = scheme
    }

    fun setCurrentPhoto(photo: PhotoData?) {
        _currentPhoto.value = photo
    }

    fun setBusy(busy: Boolean) {
        _isBusy.value = busy
    }

    private fun update(transform: (List<PhotoData>) -> List<PhotoData>) {
        val updated = transform(_photos.value)
        ImageCache.photos = updated
        _photos.value = updated
    }
}

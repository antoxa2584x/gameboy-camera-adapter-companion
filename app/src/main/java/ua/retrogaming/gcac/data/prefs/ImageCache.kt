package ua.retrogaming.gcac.data.prefs

import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.gsonpref.gsonNullablePref
import com.chibatching.kotpref.gsonpref.gsonPref
import ua.retrogaming.gcac.model.PhotoData

object ImageCache : KotprefModel() {

    var photos by gsonPref(listOf<PhotoData>())
    var isPrinting by booleanPref(false)
    var currentPhoto by gsonNullablePref <PhotoData>(null)

    var colorScheme by stringPref("grayscale")

    fun addPhotos(path: String, originalPath: String = ""){
        photos = photos.toMutableList().apply {
            add(PhotoData(path = path, originalPath = originalPath))
        }
    }

    fun removePhoto(photo: PhotoData) {
        photos = photos.toMutableList().apply {
            remove(photo)
        }
    }

    fun removeAll() {
        photos = emptyList()
    }
}
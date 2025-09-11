package ua.retrogaming.gcac.prefs

import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.gsonpref.gsonNullablePref
import com.chibatching.kotpref.gsonpref.gsonPref
import ua.retrogaming.gcac.model.PhotoData

object ImagesCache : KotprefModel() {

    val recentItems by stringSetPref(setOf())
    var photos by gsonPref(listOf<PhotoData>())
    var isPrinting by booleanPref(false)
    var currentPhoto by gsonNullablePref <PhotoData>(null)

    fun addPhotos(path: String){
        photos = photos.toMutableList().apply {
            add(PhotoData(path))
        }
    }
}
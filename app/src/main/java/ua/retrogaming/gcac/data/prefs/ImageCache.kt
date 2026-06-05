package ua.retrogaming.gcac.data.prefs

import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.gsonpref.gsonPref
import ua.retrogaming.gcac.model.PhotoData

/**
 * Persisted gallery data. Transient state (busy flag, opened photo) lives in
 * [ua.retrogaming.gcac.data.repository.PhotoRepository].
 */
object ImageCache : KotprefModel() {

    var photos by gsonPref(listOf<PhotoData>())

    var colorScheme by stringPref("grayscale")
}

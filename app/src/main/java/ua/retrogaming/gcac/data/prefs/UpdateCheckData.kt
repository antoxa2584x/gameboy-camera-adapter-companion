package ua.retrogaming.gcac.data.prefs

import com.chibatching.kotpref.KotprefModel


/**
 * Adapter firmware update state (checked on device connect).
 *
 * App updates are not tracked here — those go through Google Play's in-app update
 * API, which owns its own state. See
 * [ua.retrogaming.gcac.data.update.PlayUpdateController].
 */
object UpdateCheckData : KotprefModel() {
    var isUpdateAvailable by booleanPref(false)
    var latestVersion  by stringPref("1.4.5")
    var releaseUrl by stringPref("")
}

package ua.retrogaming.gcac.prefs

import com.chibatching.kotpref.KotprefModel


object UpdateCheckData : KotprefModel() {
    var isUpdateAvailable by booleanPref(false)
    var latestVersion  by stringPref("1.4.5")
    var releaseUrl by stringPref("")
}


package ua.retrogaming.gcac.data.prefs

import com.chibatching.kotpref.KotprefModel


object UpdateCheckData : KotprefModel() {
    // Adapter firmware update (checked on device connect)
    var isUpdateAvailable by booleanPref(false)
    var latestVersion  by stringPref("1.4.5")
    var releaseUrl by stringPref("")

    // Mobile app update (checked on app start)
    var isAppUpdateAvailable by booleanPref(false)
    var appLatestVersion by stringPref("")
    var appReleaseUrl by stringPref("")
    // Version the user dismissed the dialog for — don't nag again until a newer one appears
    var appUpdateSkippedVersion by stringPref("")
}

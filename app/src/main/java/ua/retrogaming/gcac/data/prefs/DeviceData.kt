package ua.retrogaming.gcac.data.prefs

import com.chibatching.kotpref.KotprefModel

/**
 * Persisted device-related settings. Session state (connection, LED, version)
 * lives in [ua.retrogaming.gcac.data.repository.DeviceRepository].
 */
object DeviceData : KotprefModel() {
    var language by stringPref("en")
}

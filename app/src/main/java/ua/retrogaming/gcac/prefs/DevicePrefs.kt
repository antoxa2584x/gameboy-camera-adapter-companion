package ua.retrogaming.gcac.prefs

import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.gsonpref.gsonNullablePref
import ua.retrogaming.gcac.model.LedStatus

object DevicePrefs : KotprefModel() {
    var deviceConnected by booleanPref(false)
    var ledStatus by gsonNullablePref<LedStatus>(null)
    var deviceVersion by nullableStringPref("")
}
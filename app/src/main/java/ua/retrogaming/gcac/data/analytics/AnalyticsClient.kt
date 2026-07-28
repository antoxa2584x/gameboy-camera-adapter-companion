package ua.retrogaming.gcac.data.analytics

import android.content.Context
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import ua.retrogaming.gcac.BuildConfig

/**
 * Single seam onto Firebase Analytics and Crashlytics.
 *
 * Everything Firebase-shaped lives behind this class so the UI, view model and
 * repositories stay free of it — and so collection can be switched off in one
 * place.
 *
 * Nothing recorded here may identify a user or describe photo content: only
 * adapter/firmware facts, outcome enums, and exceptions. Event and parameter
 * names are Firebase-legal (<=40 / <=100 chars, alphanumeric + underscore).
 */
class AnalyticsClient(context: Context) {

    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        // Debug builds would otherwise pollute production dashboards and inflate
        // the crash-free-users metric with breakpoints and forced errors.
        setCollectionEnabled(!BuildConfig.DEBUG)
    }

    /**
     * Toggles both SDKs. Firebase persists this across launches, so it can back a
     * user-facing opt-out without any extra bookkeeping.
     */
    fun setCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }

    // --- Crashlytics ---------------------------------------------------------

    /**
     * Attaches the connected adapter's firmware version to every subsequent
     * report — most misbehaviour here is firmware-specific, and a crash without
     * it is close to unactionable.
     */
    fun setFirmwareVersion(version: String?) {
        crashlytics.setCustomKey(KEY_FIRMWARE, version ?: "unknown")
    }

    /** Breadcrumb, attached to whatever report comes next. */
    fun breadcrumb(message: String) {
        crashlytics.log(message)
    }

    /**
     * Reports a caught, non-fatal error. [where] is a stable tag identifying the
     * call site so reports group sensibly.
     */
    fun recordError(where: String, t: Throwable) {
        crashlytics.setCustomKey(KEY_ERROR_SITE, where)
        crashlytics.recordException(t)
    }

    // --- Analytics -----------------------------------------------------------

    fun adapterConnected(firmwareVersion: String?) {
        setFirmwareVersion(firmwareVersion)
        log(EVENT_ADAPTER_CONNECTED, PARAM_FIRMWARE to (firmwareVersion ?: "unknown"))
    }

    fun photoReceived() = log(EVENT_PHOTO_RECEIVED)

    fun photoSaved(colorScheme: String) =
        log(EVENT_PHOTO_SAVED, PARAM_COLOR_SCHEME to colorScheme)

    /** [result] is an outcome enum name, never a raw error message. */
    fun printFinished(result: String) = log(EVENT_PRINT_FINISHED, PARAM_RESULT to result)

    fun mobileModeChanged(mode: String) = log(EVENT_MOBILE_MODE, PARAM_MODE to mode)

    private fun log(name: String, vararg params: Pair<String, String>) {
        analytics.logEvent(name, bundleOf(*params))
    }

    private companion object {
        const val KEY_FIRMWARE = "firmware_version"
        const val KEY_ERROR_SITE = "error_site"

        const val EVENT_ADAPTER_CONNECTED = "adapter_connected"
        const val EVENT_PHOTO_RECEIVED = "photo_received"
        const val EVENT_PHOTO_SAVED = "photo_saved"
        const val EVENT_PRINT_FINISHED = "print_finished"
        const val EVENT_MOBILE_MODE = "mobile_mode_changed"

        const val PARAM_FIRMWARE = "firmware_version"
        const val PARAM_COLOR_SCHEME = "color_scheme"
        const val PARAM_RESULT = "result"
        const val PARAM_MODE = "mode"
    }
}

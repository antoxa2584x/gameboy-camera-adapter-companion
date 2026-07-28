package ua.retrogaming.gcac.data.push

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import ua.retrogaming.gcac.BuildConfig
import ua.retrogaming.gcac.data.analytics.AnalyticsClient

/**
 * Sets up inbound push at startup.
 *
 * Subscribes to a single topic so an announcement can be sent to the whole
 * install base from the Firebase console — no backend and no token registry.
 * Subscribing is idempotent and persists across launches and token rotation;
 * re-running it every start is how a subscription that failed while offline
 * eventually succeeds.
 */
class PushClient(
    private val context: Context,
    private val analytics: AnalyticsClient,
) {

    fun init() {
        // Create the channel up front so the entry is visible in system settings
        // before the first announcement ever arrives.
        PushNotifier.ensureChannel(context)

        FirebaseMessaging.getInstance()
            .subscribeToTopic(TOPIC_ANNOUNCEMENTS)
            .addOnFailureListener { analytics.recordError("push_topic_subscribe", it) }

        logRegistrationToken()
    }

    /**
     * Logs this install's FCM registration token, so a single device can be
     * targeted from the Firebase console ("Send test message") while testing
     * instead of broadcasting to the whole topic.
     *
     * Fetched on every start rather than hooked to token rotation, so the value
     * is always in the current log rather than only appearing once, on the launch
     * where it happened to be issued.
     *
     * Debug builds only: the token uniquely identifies this install, and release
     * logcat gets swept up into bug reports and device log captures.
     */
    @Suppress("DEPRECATION")
    private fun logRegistrationToken() {
        if (!BuildConfig.DEBUG) return

        // getToken() is deprecated in firebase-messaging 25.x in favour of
        // register(), which returns Task<Void> and deliberately never hands the
        // token to the app. There is no non-deprecated way to read it, and topic
        // delivery does not need one — this exists purely for test sends.
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> Log.d(TAG, "FCM registration token: $token") }
            .addOnFailureListener { Log.w(TAG, "Could not fetch FCM registration token", it) }
    }

    private companion object {
        const val TAG = "PushClient"

        /** Send to this topic to reach every install. */
        const val TOPIC_ANNOUNCEMENTS = "announcements"
    }
}

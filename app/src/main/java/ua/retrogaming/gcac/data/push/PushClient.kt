package ua.retrogaming.gcac.data.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
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
    }

    private companion object {
        /** Send to this topic to reach every install. */
        const val TOPIC_ANNOUNCEMENTS = "announcements"
    }
}

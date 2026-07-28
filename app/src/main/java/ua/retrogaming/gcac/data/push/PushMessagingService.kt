package ua.retrogaming.gcac.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives informational pushes from FCM.
 *
 * Only handles inbound announcements — the app never sends anything, and there is
 * no backend to register tokens with. Broadcasts are addressed by topic (see
 * [PushClient]), so a message reaches every install without per-device state.
 */
class PushMessagingService : FirebaseMessagingService() {

    /**
     * Called for data messages always, and for notification messages only while
     * the app is in the foreground — when it is backgrounded, FCM draws those
     * itself from the `default_notification_*` manifest meta-data. Reading both
     * the notification block and the data payload keeps either send style working.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data[KEY_TITLE]
        val body = message.notification?.body ?: message.data[KEY_BODY]
        PushNotifier.show(this, title, body)
    }

    // No onNewToken override: it is deprecated with no replacement in
    // firebase-messaging 25.x, and topic subscriptions survive token rotation on
    // their own. Nothing here needs the token, which is a device identifier.

    private companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}

package ua.retrogaming.gcac.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ua.retrogaming.gcac.R
import ua.retrogaming.gcac.ui.MainActivity

/**
 * Builds and posts the informational notifications delivered over FCM.
 *
 * Stateless on purpose: [PushMessagingService] is constructed by the framework
 * and can run in a cold process with no Koin graph warmed up, so this must work
 * from nothing but a [Context].
 */
object PushNotifier {

    /**
     * Creates the channel if absent. Cheap and idempotent — re-creating an
     * existing channel is a no-op and never overrides user changes — so it is
     * called before every post rather than only at startup, because a message can
     * arrive in a cold process that never ran [PushClient].
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            channelId(context),
            context.getString(R.string.push_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.push_channel_description)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** Posts an announcement. No-op when the user has notifications switched off. */
    fun show(context: Context, title: String?, body: String?) {
        if (title.isNullOrBlank() && body.isNullOrBlank()) return

        ensureChannel(context)

        val manager = NotificationManagerCompat.from(context)
        // Covers a missing POST_NOTIFICATIONS grant on API 33+ as well as a
        // channel the user has muted; notify() would silently drop either way.
        if (!manager.areNotificationsEnabled()) return

        val launch = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launch,
            // IMMUTABLE because nothing needs to fill anything in, and required
            // to be explicit from API 31.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title ?: context.getString(R.string.app_name))
            .setContentText(body)
            // Announcements are prose and routinely longer than one line.
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        manager.notify(nextId(), notification)
    }

    private fun channelId(context: Context) = context.getString(R.string.push_channel_id)

    /**
     * Time-derived so separate announcements stack instead of replacing one
     * another, and so ids stay distinct across process restarts.
     */
    private fun nextId(): Int = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
}

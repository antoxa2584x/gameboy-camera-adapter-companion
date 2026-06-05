package ua.retrogaming.gcac.model

/**
 * Adapter compatibility mode (see firmware COMMUNICATION.md):
 * - [ANDROID]: USB serial (CDC) + web interface
 * - [IOS]: web interface only
 *
 * The companion app talks over CDC, so a connected adapter is always in
 * [ANDROID] mode; switching to [IOS] reboots the adapter and disconnects it.
 */
enum class MobileMode {
    ANDROID,
    IOS,
}

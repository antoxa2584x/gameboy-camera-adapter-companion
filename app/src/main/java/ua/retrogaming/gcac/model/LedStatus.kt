package ua.retrogaming.gcac.model
data class LedStatus(
    val r: Int = 0,
    val g: Int = 0,
    val b: Int = 0,
    val useRgb: Boolean = false
) {
    // Normalized channels depending on useRgb flag
    val red: Int
        get() = if (!useRgb) r else g
    val green: Int
        get() = if (!useRgb) g else r
    val blue: Int
        get() = b  // same position in both modes

    val hex: String
        get() = "#%02X%02X%02X".format(red, green, blue)
}

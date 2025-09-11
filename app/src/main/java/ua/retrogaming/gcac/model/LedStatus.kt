package ua.retrogaming.gcac.model
data class LedStatus(val r: Int = 0, val g: Int = 0, val b: Int = 0, val useRgb: Boolean = false) {
    val hex: String get() = "#%02X%02X%02X".format(r, g, b)
}
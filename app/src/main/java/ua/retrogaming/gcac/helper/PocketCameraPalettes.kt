package ua.retrogaming.gcac.helper

import android.graphics.Bitmap
import android.graphics.Color
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.roundToInt

/** Predefined palettes (RGBA ints). */
object PocketCameraPalettes {
    val GRAYSCALE = mapOf(
        0 to Color.rgb(255, 255, 255),
        1 to Color.rgb(191, 191, 191),
        2 to Color.rgb(127, 127, 127),
        3 to Color.rgb(63, 63, 63),
    )

    val GAME_BOY = mapOf(
        0 to Color.rgb(208, 217, 60),
        1 to Color.rgb(120, 164, 106),
        2 to Color.rgb(84, 88, 84),
        3 to Color.rgb(36, 70, 36),
    )

    val SUPER_GAME_BOY = mapOf(
        0 to Color.rgb(255, 255, 255),
        1 to Color.rgb(181, 179, 189),
        2 to Color.rgb(84, 83, 103),
        3 to Color.rgb(9, 7, 19),
    )

    val GBC_JPN = mapOf(
        0 to Color.rgb(240, 240, 240),
        1 to Color.rgb(218, 196, 106),
        2 to Color.rgb(112, 88, 52),
        3 to Color.rgb(30, 30, 30),
    )

    val GBC_USA_GOLD = mapOf(
        0 to Color.rgb(240, 240, 240),
        1 to Color.rgb(220, 160, 160),
        2 to Color.rgb(136, 78, 78),
        3 to Color.rgb(30, 30, 30),
    )

    val GBC_USA_EUR = mapOf(
        0 to Color.rgb(240, 240, 240),
        1 to Color.rgb(134, 200, 100),
        2 to Color.rgb(58, 96, 132),
        3 to Color.rgb(30, 30, 30),
    )

    fun findPalletByName(name: String): Map<Int, Int> {
        return when (name) {
            "grayscale" -> GRAYSCALE
            "game-boy" -> GAME_BOY
            "super-game-boy" -> SUPER_GAME_BOY
            "game-boy-color-jpn" -> GBC_JPN
            "game-boy-color-usa-gold" -> GBC_USA_GOLD
            "game-boy-color-usa-eur" -> GBC_USA_EUR
            else -> GRAYSCALE
        }
    }
}

/**
 * Maps a 4-shade (dithered) source image into a target 4-color palette.
 * Bucketing uses luminance so it works even if the image isn't exactly #FFFFFF/#BFBFBF/#7F7F7F/#3F3F3F.
 */
class PocketPaletteTransformation(
    /** Map: shade index 0..3 (0=lightest, 3=darkest) -> ARGB color int */
    private val palette: Map<Int, Int>,
) : Transformation() {

    override val cacheKey: String = buildString {
        append("PocketPaletteTransformation(")
        append(palette.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" })
        append(")")
    }

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Work on a mutable copy
        val bmp = input.copy(Bitmap.Config.ARGB_8888, true)

        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // Luminance thresholds (0..255). 4 buckets => 3 inner thresholds.
        // Tuned for GB Camera–like ranges; tweak if your scans skew darker/lighter.
        val t1 = 210  // between shade 0 and 1
        val t2 = 150  // between 1 and 2
        val t3 = 90   // between 2 and 3

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = Color.alpha(p)
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)

            // Perceived luminance (Rec. 709)
            val y = (0.2126 * r + 0.7152 * g + 0.0722 * b).roundToInt()

            val shade = when {
                y >= t1 -> 0 // lightest
                y >= t2 -> 1
                y >= t3 -> 2
                else    -> 3 // darkest
            }

            val mapped = palette[shade] ?: p
            // Preserve original alpha (most GB images are opaque anyway)
            val out = (a shl 24) or (mapped and 0x00FFFFFF)
            pixels[i] = out
        }

        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }
}

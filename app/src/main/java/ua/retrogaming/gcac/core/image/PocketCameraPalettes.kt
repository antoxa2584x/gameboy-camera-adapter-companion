package ua.retrogaming.gcac.core.image

import android.graphics.Color

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

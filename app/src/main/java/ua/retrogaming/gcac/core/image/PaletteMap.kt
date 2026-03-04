package ua.retrogaming.gcac.core.image

typealias PaletteMap = Map<Int, Int>

data class Thresholds(
    val t1: Int = 200,
    val t2: Int = 120,
    val t3: Int = 40
)

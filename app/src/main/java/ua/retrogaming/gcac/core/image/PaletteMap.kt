package ua.retrogaming.gcac.core.image

typealias PaletteMap = Map<Int, Int>

data class Thresholds(
    val t1: Int = 210,
    val t2: Int = 150,
    val t3: Int = 90
)

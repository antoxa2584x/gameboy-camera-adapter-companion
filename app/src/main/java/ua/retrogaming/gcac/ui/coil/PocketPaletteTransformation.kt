package ua.retrogaming.gcac.ui.coil

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import ua.retrogaming.gcac.core.image.PaletteMap
import ua.retrogaming.gcac.core.image.Thresholds
import ua.retrogaming.gcac.core.image.applyPocketPalette
import ua.retrogaming.gcac.core.image.upscaleBitmapNearest

class PocketPaletteTransformation(
    private val palette: PaletteMap,
    private val thresholds: Thresholds = Thresholds(),
    private val upscale: Int = 1
) : Transformation() {

    override val cacheKey: String = buildString {
        append("PocketPaletteTransformation(")
        append(palette.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" })
        append(")|t=${thresholds.t1}-${thresholds.t2}-${thresholds.t3}|s=$upscale")
    }

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val filtered = applyPocketPalette(input, palette, thresholds)
        return if (upscale > 1) {
            upscaleBitmapNearest(filtered, upscale)
        } else {
            filtered
        }
    }
}

package ua.retrogaming.gcac.core.image

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Returns a new ARGB_8888 bitmap with a 4-bucket luminance mapping applied.
 * Buckets: 0 (lightest) .. 3 (darkest).
 */
fun applyPocketPalette(
    src: Bitmap,
    palette: PaletteMap,
    thresholds: Thresholds = Thresholds()
): Bitmap {
    val w = src.width
    val h = src.height
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)

    val (t1, t2, t3) = thresholds
    for (i in px.indices) {
        val c = px[i]
        val a = Color.alpha(c)
        val r = Color.red(c)
        val g = Color.green(c)
        val b = Color.blue(c)
        val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
        val shade = when {
            y >= t1 -> 0
            y >= t2 -> 1
            y >= t3 -> 2
            else    -> 3
        }
        val mapped = palette[shade] ?: c
        px[i] = (a shl 24) or (mapped and 0x00FFFFFF)
    }
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}

fun upscaleBitmapNearest(src: Bitmap, scale: Int): Bitmap {
    if (scale <= 1) return src
    val w = src.width
    val h = src.height
    val outW = w * scale
    val outH = h * scale
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val srcPixels = IntArray(w * h)
    src.getPixels(srcPixels, 0, w, 0, 0, w, h)
    val outPixels = IntArray(outW * outH)

    for (y in 0 until outH) {
        val srcY = y / scale
        val srcRowOffset = srcY * w
        val outRowOffset = y * outW
        for (x in 0 until outW) {
            val srcX = x / scale
            outPixels[outRowOffset + x] = srcPixels[srcRowOffset + srcX]
        }
    }
    out.setPixels(outPixels, 0, outW, 0, 0, outW, outH)
    return out
}

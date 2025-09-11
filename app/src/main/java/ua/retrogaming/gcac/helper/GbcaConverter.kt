package ua.retrogaming.gcac.helper

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Decodes GB Camera/Printer serial capture that was sent as Base64 lines framed by:
 *   GBCA_PHOTO_TRANSFER[_BASE64]
 *   <many base64 lines (possibly mixed with timestamps)>
 *   DONE
 *
 * Protocol and RLE mirror the provided JS reference.
 */
class GbcaConverter(
    private val cameraWidth: Int = 128,   // pixels (16 tiles)
    private val printerWidth: Int = 160   // pixels (20 tiles)
) {

    // Commands (same as your JS)
    private companion object {
        private const val COMMAND_INIT     = 0x01
        private const val COMMAND_PRINT    = 0x02
        private const val COMMAND_TRANSFER = 0x03
        private const val COMMAND_DATA     = 0x04
    }

    data class Frame(val bitmap: Bitmap, val sourceCmd: Int)

    /**
     * Top-level helper: feed raw log lines (with timestamps) and get all decoded frames.
     */
    fun decodeFromLogLines(lines: List<String>): List<Frame> {
        val blob = collectAndDecodeBase64(lines)
        return parseFrames(blob)
    }

    /**
     * Extract base64 between header and DONE, ignoring timestamps and other non-b64 chars.
     */
    private fun collectAndDecodeBase64(lines: List<String>): ByteArray {
        // between GBCA_PHOTO_TRANSFER[_BASE64] and DONE
        var inPayload = false
        val out = ByteArrayOutputStream(64 * 1024)

        val headerPrefixes = listOf("GBCA_PHOTO_TRANSFER")
        val tokenRe = Regex("""[A-Za-z0-9+/=]+""")

        fun decodeToken(tok: String) {
            // keep only base64 chars
            val clean = tok.filter { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
            if (clean.isEmpty()) return
            // pad to multiple of 4
            val pad = (4 - (clean.length % 4)) % 4
            val padded = if (pad > 0 && !clean.endsWith("=")) clean + "=".repeat(pad) else clean
            try {
                val bytes = Base64.decode(padded, Base64.NO_WRAP)
                out.write(bytes)
            } catch (e: IllegalArgumentException) {
                // token wasn’t valid base64—skip it (or log if you want)
                Log.w("GbcaConverter", "skip bad token len=${padded.length}")
            }
        }

        for (raw in lines) {
            val line = raw.trim().replace("\n", "")
            if (!inPayload) {
                if (headerPrefixes.any { line.contains(it) }) inPayload = true
                continue
            }
            if (line.contains("DONE")) break

            // A single “line” from your logs may have: "<timestamp>  <base64>"
            // or multiple base64 tokens. Decode each token individually and append.
            tokenRe.findAll(line).forEach { m ->
                decodeToken(m.value)
            }
        }
        return out.toByteArray()
    }


    /**
     * Parse the command stream into frames, using JS-like state:
     * - processed[] is the accumulation buffer, ptr is write pointer
     * - bufferStart marks the start of the current printable region
     */
    private fun parseFrames(blob: ByteArray): List<Frame> {
        val frames = mutableListOf<Frame>()
        val processed = ByteArray(maxOf(1_048_576, blob.size))
        var ptr = 0
        var idx = 0
        var bufferStart = 0

        fun slice(start: Int, end: Int) = processed.copyOfRange(start, end)

        while (idx < blob.size) {
            val cmd = blob[idx].toInt() and 0xFF
            idx++

            when (cmd) {
                COMMAND_INIT -> {
                    // no payload
                }

                COMMAND_PRINT -> {
                    if (idx + 2 > blob.size) break
                    val len = (blob[idx].toInt() and 0xFF) or ((blob[idx + 1].toInt() and 0xFF) shl 8)
                    idx += 2
                    if (len != 4 || idx + 4 > blob.size) { idx = blob.size; break }

                    val sheets   = blob[idx].toInt() and 0xFF
                    val margins  = blob[idx + 1].toInt() and 0xFF
                    var palette  = blob[idx + 2].toInt() and 0xFF
                    val exposure = (0x80 + (blob[idx + 3].toInt() and 0xFF)).coerceAtMost(0xFF)
                    idx += 4
                    if (palette == 0) palette = 0xE4
                    // Render region accumulated since last cut
                    val region = slice(bufferStart, ptr)
                    tilesToBitmapOrNull(region, printerWidth)?.let { bmp ->
                        frames += Frame(bmp, COMMAND_PRINT)
                    }
                    bufferStart = ptr
                    // (you can use sheets/margins/palette/exposure if you map palettes)
                }

                COMMAND_TRANSFER -> {
                    if (idx + 2 > blob.size) break
                    val len = (blob[idx].toInt() and 0xFF) or ((blob[idx + 1].toInt() and 0xFF) shl 8)
                    idx += 2
                    val startPtr = ptr
                    ptr = rleDecode(isCompressed = false, src = blob, srcOff = idx, srcLen = len, dst = processed, dstOff = ptr)
                    idx += len
                    val region = slice(startPtr, ptr)
                    tilesToBitmapOrNull(region, cameraWidth)?.let { bmp ->
                        frames += Frame(bmp, COMMAND_TRANSFER)
                    }
                    bufferStart = ptr
                }

                COMMAND_DATA -> {
                    if (idx + 3 > blob.size) break
                    val compression = blob[idx].toInt() and 0xFF
                    val len = (blob[idx + 1].toInt() and 0xFF) or ((blob[idx + 2].toInt() and 0xFF) shl 8)
                    idx += 3
                    ptr = rleDecode(isCompressed = (compression != 0), src = blob, srcOff = idx, srcLen = len, dst = processed, dstOff = ptr)
                    idx += len
                }

                else -> {
                    // Unknown command: stop like the JS does
                    break
                }
            }
        }

        return frames
    }

    /**
     * JS-compatible RLE:
     *  - if (tag & 0x80) -> repeat next byte (tag&0x7F)+2 times
     *  - else copy (tag+1) literal bytes
     * If isCompressed=false, we just memcpy the block.
     */
    private fun rleDecode(
        isCompressed: Boolean,
        src: ByteArray, srcOff: Int, srcLen: Int,
        dst: ByteArray, dstOff: Int
    ): Int {
        var sp = srcOff
        var dp = dstOff
        val end = srcOff + srcLen

        if (!isCompressed) {
            System.arraycopy(src, sp, dst, dp, srcLen)
            return dp + srcLen
        }

        while (sp < end) {
            val tag = src[sp++].toInt() and 0xFF
            if ((tag and 0x80) != 0) {
                val data = src[sp++]
                val count = (tag and 0x7F) + 2
                dst.fill(data, dp, dp + count)
                dp += count
            } else {
                val count = tag + 1
                System.arraycopy(src, sp, dst, dp, count)
                sp += count
                dp += count
            }
        }
        return dp
    }

    /**
     * Convert 2-bpp GB tiles (16 bytes/tile) to grayscale Bitmap.
     * widthPixels must be a multiple of 8 (tiles per row = width/8).
     * Returns null if there isn't at least one full row of tiles.
     */
    private fun tilesToBitmapOrNull(tiles: ByteArray, widthPixels: Int): Bitmap? {
        require(widthPixels % 8 == 0) { "widthPixels must be multiple of 8" }

        val tilesPerRow = widthPixels / 8
        val totalTiles  = tiles.size / 16
        if (totalTiles == 0) return null

        // we only render complete rows of tiles
        val rowsTiles   = totalTiles / tilesPerRow
        if (rowsTiles == 0) return null

        val heightPixels = rowsTiles * 8
        val bmp = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        val argb = IntArray(widthPixels * heightPixels)

        var tileIdx = 0
        for (ty in 0 until rowsTiles) {
            for (tx in 0 until tilesPerRow) {
                val base = tileIdx * 16
                if (base + 16 > tiles.size) break
                for (row in 0 until 8) {
                    val lo = tiles[base + row*2].toInt() and 0xFF
                    val hi = tiles[base + row*2 + 1].toInt() and 0xFF
                    for (bit in 7 downTo 0) {
                        val v = (((hi shr bit) and 1) shl 1) or ((lo shr bit) and 1) // 0..3
                        val gray = (255 - v * 85) and 0xFF  // 0=white .. 3=black
                        val x = tx*8 + (7 - bit)
                        val y = ty*8 + row
                        argb[y*widthPixels + x] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
                    }
                }
                tileIdx++
            }
        }

        bmp.setPixels(argb, 0, widthPixels, 0, 0, widthPixels, heightPixels)
        return bmp
    }
}
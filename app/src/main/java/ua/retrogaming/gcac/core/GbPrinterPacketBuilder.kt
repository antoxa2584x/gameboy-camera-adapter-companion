package ua.retrogaming.gcac.core

import android.graphics.Bitmap

/**
 * Builds Game Boy Printer packets (hex encoded) from a Bitmap, mirroring the
 * adapter firmware's web UI print pipeline (canvasToTileData / sendChunkedData).
 *
 * Packet layout: 88 33 | cmd | compression | lenL lenH | data | chkL chkH | 00 00
 */
object GbPrinterPacketBuilder {

    const val DEFAULT_EXPOSURE = 0x40

    private const val PRINTER_WIDTH_PX = 160
    private const val TILE_SIZE = 8
    private const val BYTES_PER_TILE = 16
    private const val STRIP_SIZE = 640      // 2 tile rows = 16 px, the unit the printer expects
    private const val CHUNK_SIZE = 256      // payload bytes per DATA packet (firmware reassembles strips)

    private const val COMMAND_PRINT = 0x02
    private const val COMMAND_DATA = 0x04

    const val PACKET_INIT = "88330100000001000000"
    const val PACKET_STATUS = "88330f0000000f000000"
    private const val PACKET_DATA_END = "88330400000004000000"

    /**
     * Full packet sequence for one print job:
     * INIT, STATUS, DATA × n, empty DATA (end marker), PRINT, STATUS × 3.
     */
    fun buildPrintPackets(bitmap: Bitmap, exposure: Int = DEFAULT_EXPOSURE): List<String> {
        val tileData = bitmapToTileData(bitmap)
        // Pad to complete 640-byte strips; zero padding = white for the GB Printer
        val totalStrips = (tileData.size + STRIP_SIZE - 1) / STRIP_SIZE
        val data = tileData.copyOf(totalStrips * STRIP_SIZE)

        val packets = mutableListOf(PACKET_INIT, PACKET_STATUS)
        var offset = 0
        while (offset < data.size) {
            val end = minOf(offset + CHUNK_SIZE, data.size)
            packets.add(dataPacket(data.copyOfRange(offset, end)))
            offset = end
        }
        packets.add(PACKET_DATA_END)
        packets.add(printPacket(exposure))
        repeat(3) { packets.add(PACKET_STATUS) }
        return packets
    }

    /**
     * Quantizes an already-cropped printer frame to the 4 GB shades. The
     * result is the exact image sent to the printer; the exposure tone shown
     * on screen is simulated separately in the UI so the print data stays
     * aligned with [bitmapToTileData]'s thresholds.
     */
    fun quantizeForPrinter(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val px = pixels[i]
            val gray = 0.299 * ((px shr 16) and 0xFF) +
                    0.587 * ((px shr 8) and 0xFF) +
                    0.114 * (px and 0xFF)
            // Same thresholds as bitmapToTileData, mapped back to gray levels
            // that land in the same shade buckets when printed
            val v = when {
                gray > 192 -> 255
                gray > 128 -> 170
                gray > 64 -> 85
                else -> 0
            }
            pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Converts a bitmap to 2-bpp GB tile data: centered horizontally on the
     * 160 px printer width, height padded to the next 8 px boundary with white.
     */
    fun bitmapToTileData(bitmap: Bitmap): ByteArray {
        val w = bitmap.width
        val h = bitmap.height
        val tileH = (h + TILE_SIZE - 1) / TILE_SIZE * TILE_SIZE
        val offsetX = (PRINTER_WIDTH_PX - w) / 2

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val out = ByteArray(PRINTER_WIDTH_PX / TILE_SIZE * (tileH / TILE_SIZE) * BYTES_PER_TILE)
        var i = 0
        for (tileY in 0 until tileH step TILE_SIZE) {
            for (tileX in 0 until PRINTER_WIDTH_PX step TILE_SIZE) {
                for (row in 0 until TILE_SIZE) {
                    var lo = 0
                    var hi = 0
                    for (col in 0 until TILE_SIZE) {
                        val y = tileY + row
                        val x = tileX + col - offsetX
                        var shade = 0 // default white
                        if (y < h && x in 0 until w) {
                            val px = pixels[y * w + x]
                            val gray = 0.299 * ((px shr 16) and 0xFF) +
                                    0.587 * ((px shr 8) and 0xFF) +
                                    0.114 * (px and 0xFF)
                            // GB grayscale is inverted intensity: 0=white .. 3=black
                            shade = when {
                                gray > 192 -> 0
                                gray > 128 -> 1
                                gray > 64 -> 2
                                else -> 3
                            }
                        }
                        lo = lo or ((shade and 1) shl (7 - col))
                        hi = hi or (((shade shr 1) and 1) shl (7 - col))
                    }
                    out[i++] = lo.toByte()
                    out[i++] = hi.toByte()
                }
            }
        }
        return out
    }

    private fun dataPacket(payload: ByteArray): String {
        val sb = StringBuilder(payload.size * 2 + 20)
        sb.append("88330400")
        sb.append(hexByte(payload.size and 0xFF)).append(hexByte(payload.size shr 8))
        payload.forEach { sb.append(hexByte(it.toInt() and 0xFF)) }
        val checksum = checksum(COMMAND_DATA, payload)
        sb.append(hexByte(checksum and 0xFF)).append(hexByte(checksum shr 8))
        sb.append("0000")
        return sb.toString()
    }

    private fun printPacket(exposure: Int): String {
        // Payload: sheets=1, margins=0x03, palette=0xE4, exposure (0..0x7F)
        val payload = byteArrayOf(0x01, 0x03, 0xE4.toByte(), exposure.coerceIn(0, 0x7F).toByte())
        val sb = StringBuilder("883302000400")
        payload.forEach { sb.append(hexByte(it.toInt() and 0xFF)) }
        val checksum = checksum(COMMAND_PRINT, payload)
        sb.append(hexByte(checksum and 0xFF)).append(hexByte(checksum shr 8))
        sb.append("0000")
        return sb.toString()
    }

    private fun checksum(command: Int, data: ByteArray): Int {
        var sum = command + (data.size and 0xFF) + (data.size shr 8)
        data.forEach { sum += it.toInt() and 0xFF }
        return sum and 0xFFFF
    }

    private fun hexByte(value: Int): String = "%02x".format(value and 0xFF)
}

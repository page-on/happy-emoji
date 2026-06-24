package com.emoji.app.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.ByteArrayOutputStream

/**
 * Pure Kotlin GIF encoder (GIF89a).
 * Every frame gets its own local color table (no global table).
 * Input bitmaps are forced to ARGB_8888 for consistent pixel reading.
 */
class GifEncoder(
    private val width: Int,
    private val height: Int,
    private val quality: Int = AppConfig.DEFAULT_QUALITY.gifSample,
) {
    private val out = ByteArrayOutputStream(4096)
    private var firstFrame = true
    private var repeat = AppConfig.GIF_LOOP_COUNT

    fun setRepeat(r: Int) { repeat = r }

    fun addFrame(bitmap: Bitmap, delayMs: Int) {
        // Force ARGB_8888 to guarantee consistent IntArray pixel layout
        val safe = toArgb8888(bitmap)
        val scaled = if (safe.width != width || safe.height != height) {
            Bitmap.createScaledBitmap(safe, width, height, true)
        } else safe

        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        // NeuQuant color reduction
        val nq = NeuQuant(pixels, quality)
        nq.buildColormap()
        val colorTab = nq.colormap
        val indexed = ByteArray(width * height)

        // Floyd-Steinberg dithering with wide working buffers.
        // Each channel uses its own IntArray so accumulated error values
        // can temporarily exceed [0,255] without being truncated.
        // Only the clamped value is used for palette lookup; the full
        // (unclamped) error is diffused to neighbours.
        val nPixels = width * height
        val workR = IntArray(nPixels)
        val workG = IntArray(nPixels)
        val workB = IntArray(nPixels)
        for (i in 0 until nPixels) {
            workR[i] = (pixels[i] shr 16) and 0xFF
            workG[i] = (pixels[i] shr 8) and 0xFF
            workB[i] = pixels[i] and 0xFF
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x

                // Clamp for palette lookup only
                val cr = workR[idx].coerceIn(0, 255)
                val cg = workG[idx].coerceIn(0, 255)
                val cb = workB[idx].coerceIn(0, 255)
                val sample = (0xFF shl 24) or (cr shl 16) or (cg shl 8) or cb

                val paletteIdx = nq.lookup(sample)
                indexed[idx] = paletteIdx.toByte()

                // Compute error against the CLAMPED value (the actual output).
                // Using unclamped work values would let negative drift cascade
                // across rows and eventually turn the whole image black.
                val palColor = colorTab[paletteIdx]
                val errR = cr - ((palColor shr 16) and 0xFF)
                val errG = cg - ((palColor shr 8) and 0xFF)
                val errB = cb - (palColor and 0xFF)

                // Floyd-Steinberg distribution: 7/16 right, 3/16 bottom-left,
                // 5/16 bottom, 1/16 bottom-right
                if (x + 1 < width) {
                    workR[idx + 1] += errR * 7 / 16
                    workG[idx + 1] += errG * 7 / 16
                    workB[idx + 1] += errB * 7 / 16
                }
                if (y + 1 < height) {
                    if (x - 1 >= 0) {
                        workR[idx + width - 1] += errR * 3 / 16
                        workG[idx + width - 1] += errG * 3 / 16
                        workB[idx + width - 1] += errB * 3 / 16
                    }
                    workR[idx + width] += errR * 5 / 16
                    workG[idx + width] += errG * 5 / 16
                    workB[idx + width] += errB * 5 / 16
                    if (x + 1 < width) {
                        workR[idx + width + 1] += errR * 1 / 16
                        workG[idx + width + 1] += errG * 1 / 16
                        workB[idx + width + 1] += errB * 1 / 16
                    }
                }
            }
        }

        // Write GIF structures (first frame: header+LSD+Netscape)
        if (firstFrame) {
            writeHeaderAndLSD()
            writeNetscapeExt()
            firstFrame = false
        }

        // Graphic Control Extension
        val delay = (delayMs / AppConfig.GIF_DELAY_DIVISOR).coerceAtLeast(1)
        out.write(0x21); out.write(0xF9); out.write(4)
        out.write(0x04) // disposal=none, user input=no, transparent=no
        writeShortLE(delay)
        out.write(0); out.write(0)

        // Image Descriptor
        out.write(0x2C)
        writeShortLE(0); writeShortLE(0)
        writeShortLE(width); writeShortLE(height)
        out.write(0x87) // local color table, 256 colors

        // Local Color Table (always)
        for (i in 0 until 256) {
            val c = if (i < colorTab.size) colorTab[i] else 0
            out.write((c shr 16) and 0xFF) // R
            out.write((c shr 8) and 0xFF)  // G
            out.write(c and 0xFF)          // B
        }

        // LZW encode
        val lzw = LZWEncoder(width, height, indexed, 8)
        lzw.encode(out)

        // Cleanup
        if (scaled !== safe) scaled.recycle()
        if (safe !== bitmap) safe.recycle()
    }

    fun finish(): ByteArray {
        out.write(0x3B) // Trailer
        return out.toByteArray()
    }

    /** Converts any bitmap to ARGB_8888 to guarantee consistent pixel access */
    private fun toArgb8888(src: Bitmap): Bitmap {
        if (src.config == Bitmap.Config.ARGB_8888 && src.isMutable) return src
        val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawBitmap(src, 0f, 0f, null)
        return bmp
    }

    private fun writeHeaderAndLSD() {
        out.write("GIF89a".toByteArray())
        writeShortLE(width)
        writeShortLE(height)
        out.write(0x70) // No global color table
        out.write(0)    // Background color index
        out.write(0)    // Pixel aspect ratio
    }

    private fun writeNetscapeExt() {
        out.write(0x21); out.write(0xFF); out.write(11)
        out.write("NETSCAPE2.0".toByteArray())
        out.write(3); out.write(1)
        writeShortLE(repeat)
        out.write(0)
    }

    private fun writeShortLE(value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }
}

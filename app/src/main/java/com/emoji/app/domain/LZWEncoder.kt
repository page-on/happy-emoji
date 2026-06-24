package com.emoji.app.domain

import java.io.ByteArrayOutputStream

/**
 * LZWEncoder — LZW compression for GIF pixel data.
 * Correctly ported from the JS reference implementation.
 */
class LZWEncoder(
    private val width: Int,
    private val height: Int,
    private val pixels: ByteArray,
    private val colorDepth: Int,
) {
    private val EOF = -1
    private val BITS = 12
    private val HSIZE = 5003

    private val masks = intArrayOf(0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535)

    private var curAccum = 0
    private var curBits = 0
    private var aCount = 0
    private var freeEnt = 0
    private var clearFlg = false
    private var gInitBits = 0      // original initial bits, never changes after compress() sets it
    private var codeSize = 0       // current code size in bits (= n_bits in JS)
    private var clearCode = 0
    private var eofCode = 0

    private var remaining = width * height
    private var curPixel = 0

    private val accum = ByteArray(256)
    private val htab = IntArray(HSIZE) { -1 }
    private val codetab = IntArray(HSIZE)

    fun encode(outs: ByteArrayOutputStream) {
        val initCodeSize = maxOf(2, colorDepth)
        outs.write(initCodeSize)
        remaining = width * height
        curPixel = 0
        compress(initCodeSize + 1, outs)
        outs.write(0)
    }

    private fun compress(initBitsParam: Int, outs: ByteArrayOutputStream) {
        gInitBits = initBitsParam
        clearFlg = false
        codeSize = gInitBits
        clearCode = 1 shl (gInitBits - 1)
        eofCode = clearCode + 1
        freeEnt = clearCode + 2
        aCount = 0

        var ent = nextPixel()
        var hshift = 0
        var fcode = HSIZE
        while (fcode < 65536) { hshift++; fcode *= 2 }
        hshift = 8 - hshift
        val hsizeReg = HSIZE
        clHash(hsizeReg)
        output(clearCode, outs)

        var c: Int
        outerLoop@ while (nextPixel().also { c = it } != EOF) {
            fcode = (c shl BITS) + ent
            var i = (c shl hshift) xor ent
            if (htab[i] == fcode) {
                ent = codetab[i]
                continue@outerLoop
            } else if (htab[i] >= 0) {
                var disp = hsizeReg - i
                if (i == 0) disp = 1
                do {
                    i -= disp
                    if (i < 0) i += hsizeReg
                    if (htab[i] == fcode) {
                        ent = codetab[i]
                        continue@outerLoop
                    }
                } while (htab[i] >= 0)
            }
            output(ent, outs)
            ent = c
            if (freeEnt < (1 shl BITS)) {
                codetab[i] = freeEnt++
                htab[i] = fcode
            } else {
                clBlock(outs)
            }
        }
        output(ent, outs)
        output(eofCode, outs)
    }

    private fun clBlock(outs: ByteArrayOutputStream) {
        clHash(HSIZE)
        freeEnt = clearCode + 2
        clearFlg = true
        output(clearCode, outs)
    }

    private fun clHash(hsize: Int) {
        for (i in 0 until hsize) htab[i] = -1
    }

    private fun nextPixel(): Int {
        if (remaining == 0) return EOF
        remaining--
        return pixels[curPixel++].toInt() and 0xFF
    }

    private fun output(code: Int, outs: ByteArrayOutputStream) {
        curAccum = curAccum and masks[curBits]
        curAccum = if (curBits > 0) (curAccum or (code shl curBits)) else code
        curBits += codeSize

        while (curBits >= 8) {
            charOut(curAccum and 0xFF, outs)
            curAccum = curAccum shr 8
            curBits -= 8
        }

        // After output, check if we need to grow code size or clear
        // JS: if (free_ent > maxcode || clear_flg)
        val maxCode = maxCode(codeSize)
        if (freeEnt > maxCode || clearFlg) {
            if (clearFlg) {
                codeSize = gInitBits          // reset to initial bit width
                clearFlg = false
            } else if (codeSize < BITS) {
                codeSize++                     // grow only if below BITS
            }
        }

        if (code == eofCode) {
            while (curBits > 0) {
                charOut(curAccum and 0xFF, outs)
                curAccum = curAccum shr 8
                curBits -= 8
            }
            flushChar(outs)
        }
    }

    private fun charOut(c: Int, outs: ByteArrayOutputStream) {
        accum[aCount++] = c.toByte()
        if (aCount >= 254) flushChar(outs)
    }

    private fun flushChar(outs: ByteArrayOutputStream) {
        if (aCount > 0) {
            outs.write(aCount)
            outs.write(accum, 0, aCount)
            aCount = 0
        }
    }

    private fun maxCode(nBits: Int): Int = (1 shl nBits) - 1
}

package com.emoji.app.domain

/**
 * NeuQuant — Neural Network color quantization.
 * Ported from JS demo_unified.html (Float64Array).
 * Uses Double precision (64-bit) to match JS Float64Array behavior.
 * Reduces true-color pixels to a 256-color palette.
 */
class NeuQuant(private val pixels: IntArray, private val sampleFac: Int) {

    companion object {
        private const val N_CYCLES = 100
        const val NET_SIZE = 256
        private const val MAX_NET_POS = NET_SIZE - 1
        private const val NET_BIAS_SHIFT = 4
        private const val INT_BIAS_SHIFT = 16
        private const val INT_BIAS = 1 shl INT_BIAS_SHIFT
        private const val GAMMA_SHIFT = 10
        private const val GAMMA = 1 shl GAMMA_SHIFT
        private const val BETA_SHIFT = 10
        private const val BETA = INT_BIAS shr BETA_SHIFT
        private const val BETAGAMMA = INT_BIAS shl (GAMMA_SHIFT - BETA_SHIFT)
        private const val INIT_RAD = NET_SIZE shr 3
        private const val RADIUS_BIAS_SHIFT = 6
        private const val RADIUS_BIAS = 1 shl RADIUS_BIAS_SHIFT
        private const val INIT_RADIUS = INIT_RAD * RADIUS_BIAS
        private const val RADIUS_DEC = 30
        private const val ALPHA_BIAS_SHIFT = 10
        private const val IN_ALPHA = 1 shl ALPHA_BIAS_SHIFT
        private const val RAD_BIAS_SHIFT = 8
        private const val RAD_BIAS = 1 shl RAD_BIAS_SHIFT
        private const val ALPHA_RAD_BIAS_SHIFT = ALPHA_BIAS_SHIFT + RAD_BIAS_SHIFT
        private const val ALPHA_RAD_BIAS = 1 shl ALPHA_RAD_BIAS_SHIFT
        private const val PRIME1 = 499
        private const val PRIME2 = 491
        private const val PRIME3 = 487
        private const val PRIME4 = 503
    }

    // Double precision to match JS Float64Array
    private val network = Array(NET_SIZE) { DoubleArray(4) }
    private val netIndex = IntArray(256)
    private val bias = IntArray(NET_SIZE)
    private val freq = IntArray(NET_SIZE)
    private val radPower = IntArray(NET_SIZE) // over-sized to prevent OOB

    var colormap = IntArray(NET_SIZE)
        private set

    init {
        for (i in 0 until NET_SIZE) {
            val v = (i.toDouble() * (1 shl (NET_BIAS_SHIFT + 8))) / NET_SIZE.toDouble()
            network[i][0] = v
            network[i][1] = v
            network[i][2] = v
            freq[i] = INT_BIAS / NET_SIZE
            bias[i] = 0
        }
    }

    fun buildColormap() {
        learn()
        unbiasNet()
        inxBuild()
        val index = IntArray(NET_SIZE)
        for (i in 0 until NET_SIZE) {
            index[network[i][3].toInt()] = i
        }
        var k = 0
        for (l in 0 until NET_SIZE) {
            val j = index[l]
            val b = network[j][0].toInt().coerceIn(0, 255)
            val g = network[j][1].toInt().coerceIn(0, 255)
            val r = network[j][2].toInt().coerceIn(0, 255)
            colormap[k++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    fun lookup(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return inxSearch(b, g, r)
    }

    private fun unbiasNet() {
        for (i in 0 until NET_SIZE) {
            network[i][0] = network[i][0] / (1 shl NET_BIAS_SHIFT)
            network[i][1] = network[i][1] / (1 shl NET_BIAS_SHIFT)
            network[i][2] = network[i][2] / (1 shl NET_BIAS_SHIFT)
            network[i][3] = i.toDouble()
        }
    }

    private fun inxBuild() {
        var prevCol = 0
        var startPos = 0
        for (i in 0 until NET_SIZE) {
            var smallPos = i
            var smallVal = network[i][1].toInt()
            for (j in i + 1 until NET_SIZE) {
                if (network[j][1].toInt() < smallVal) {
                    smallPos = j
                    smallVal = network[j][1].toInt()
                }
            }
            val q = network[smallPos]
            network[smallPos] = network[i]
            network[i] = q

            if (smallVal != prevCol) {
                netIndex[prevCol] = (startPos + i) shr 1
                for (j in prevCol + 1 until smallVal) netIndex[j] = i
                prevCol = smallVal
                startPos = i
            }
        }
        netIndex[prevCol] = (startPos + MAX_NET_POS) shr 1
        for (j in prevCol + 1 until 256) netIndex[j] = MAX_NET_POS
    }

    private fun inxSearch(b: Int, g: Int, r: Int): Int {
        var bestD = Int.MAX_VALUE
        var best = -1
        var i = netIndex[g]
        var j = i - 1

        while (i < NET_SIZE || j >= 0) {
            if (i < NET_SIZE) {
                val p = network[i]
                var dist = p[1].toInt() - g
                if (dist >= bestD) i = NET_SIZE
                else {
                    i++
                    if (dist < 0) dist = -dist
                    var a = p[0].toInt() - b; if (a < 0) a = -a; dist += a
                    if (dist < bestD) {
                        a = p[2].toInt() - r; if (a < 0) a = -a; dist += a
                        if (dist < bestD) {
                            bestD = dist
                            best = p[3].toInt()
                        }
                    }
                }
            }
            if (j >= 0) {
                val p = network[j]
                var dist = g - p[1].toInt()
                if (dist >= bestD) j = -1
                else {
                    j--
                    if (dist < 0) dist = -dist
                    var a = p[0].toInt() - b; if (a < 0) a = -a; dist += a
                    if (dist < bestD) {
                        a = p[2].toInt() - r; if (a < 0) a = -a; dist += a
                        if (dist < bestD) {
                            bestD = dist
                            best = p[3].toInt()
                        }
                    }
                }
            }
        }
        return best.coerceAtLeast(0)
    }

    private fun learn() {
        val pixelCount = pixels.size
        val alphaDec = 30 + (sampleFac - 1) / 3
        val samplePixels = pixelCount / sampleFac
        var delta = (samplePixels / N_CYCLES).coerceAtLeast(1)
        var alpha = IN_ALPHA
        var radius = INIT_RADIUS
        var rad = radius shr RADIUS_BIAS_SHIFT
        if (rad <= 1) rad = 0
        for (i in 0 until rad) {
            radPower[i] = (alpha.toDouble() * ((rad * rad - i * i).toDouble() * RAD_BIAS.toDouble() / (rad * rad).toDouble())).toInt()
        }

        val step = when {
            pixelCount < PRIME1 -> 1
            pixelCount % PRIME1 != 0 -> PRIME1
            pixelCount % PRIME2 != 0 -> PRIME2
            pixelCount % PRIME3 != 0 -> PRIME3
            else -> PRIME4
        }

        var pix = 0
        var i = 0
        while (i < samplePixels) {
            val pixel = pixels[pix]
            val b = (pixel and 0xFF) shl NET_BIAS_SHIFT
            val g = ((pixel shr 8) and 0xFF) shl NET_BIAS_SHIFT
            val r = ((pixel shr 16) and 0xFF) shl NET_BIAS_SHIFT

            val j = contest(b, g, r)
            alterSingle(alpha, j, b, g, r)
            if (rad != 0) alterNeigh(rad, j, b, g, r)

            pix += step
            if (pix >= pixelCount) pix -= pixelCount
            i++
            if (delta == 0) delta = 1
            if (i % delta == 0) {
                alpha -= alpha / alphaDec
                radius -= radius / RADIUS_DEC
                rad = radius shr RADIUS_BIAS_SHIFT
                if (rad <= 1) rad = 0
                for (k in 0 until rad) {
                    radPower[k] = (alpha.toDouble() * ((rad * rad - k * k).toDouble() * RAD_BIAS.toDouble() / (rad * rad).toDouble())).toInt()
                }
            }
        }
    }

    private fun contest(b: Int, g: Int, r: Int): Int {
        var bestD = Int.MIN_VALUE
        var bestBiasD = bestD
        var bestPos = -1
        var bestBiasPos = bestPos

        for (i in 0 until NET_SIZE) {
            val n = network[i]
            val dist = kotlin.math.abs(n[0].toInt() - b) +
                       kotlin.math.abs(n[1].toInt() - g) +
                       kotlin.math.abs(n[2].toInt() - r)
            if (dist < bestD || bestPos < 0) { bestD = dist; bestPos = i }
            val biasDist = dist - (bias[i] shr (INT_BIAS_SHIFT - NET_BIAS_SHIFT))
            if (biasDist < bestBiasD || bestBiasPos < 0) { bestBiasD = biasDist; bestBiasPos = i }
            val betaFreq = freq[i] shr BETA_SHIFT
            freq[i] -= betaFreq
            bias[i] += betaFreq shl GAMMA_SHIFT
        }
        freq[bestPos] += BETA
        bias[bestPos] -= BETAGAMMA
        return bestBiasPos
    }

    private fun alterSingle(alpha: Int, i: Int, b: Int, g: Int, r: Int) {
        val n = network[i]
        n[0] -= (alpha.toDouble() * (n[0] - b)) / IN_ALPHA.toDouble()
        n[1] -= (alpha.toDouble() * (n[1] - g)) / IN_ALPHA.toDouble()
        n[2] -= (alpha.toDouble() * (n[2] - r)) / IN_ALPHA.toDouble()
    }

    private fun alterNeigh(rad: Int, i: Int, b: Int, g: Int, r: Int) {
        val lo = (i - rad).coerceAtLeast(0)
        val hi = (i + rad).coerceAtMost(NET_SIZE)
        var j = i + 1
        var k = i - 1
        var m = 1
        while (j < hi || k > lo) {
            val a = if (m < radPower.size) radPower[m] else 0
            m++
            if (j < hi) {
                val p = network[j++]
                p[0] -= (a.toDouble() * (p[0] - b)) / ALPHA_RAD_BIAS.toDouble()
                p[1] -= (a.toDouble() * (p[1] - g)) / ALPHA_RAD_BIAS.toDouble()
                p[2] -= (a.toDouble() * (p[2] - r)) / ALPHA_RAD_BIAS.toDouble()
            }
            if (k > lo) {
                val p = network[k--]
                p[0] -= (a.toDouble() * (p[0] - b)) / ALPHA_RAD_BIAS.toDouble()
                p[1] -= (a.toDouble() * (p[1] - g)) / ALPHA_RAD_BIAS.toDouble()
                p[2] -= (a.toDouble() * (p[2] - r)) / ALPHA_RAD_BIAS.toDouble()
            }
        }
    }
}

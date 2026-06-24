package com.emoji.app.domain.cache

/**
 * ③ FontMetricsCache — cache text measurement results.
 * Key: (text, fontName, fontSize), Value: Pair(width, height)
 * Invalidation: cleared on export completion
 */
class FontMetricsCache {

    data class FontKey(val text: String, val fontName: String, val fontSize: Float)

    private val cache = mutableMapOf<FontKey, Pair<Float, Float>>()

    @Synchronized
    fun get(text: String, fontName: String, fontSize: Float): Pair<Float, Float>? =
        cache[FontKey(text, fontName, fontSize)]

    @Synchronized
    fun put(text: String, fontName: String, fontSize: Float, width: Float, height: Float) {
        cache[FontKey(text, fontName, fontSize)] = Pair(width, height)
    }

    @Synchronized
    fun clear() { cache.clear() }
}

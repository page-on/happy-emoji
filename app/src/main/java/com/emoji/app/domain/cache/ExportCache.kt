package com.emoji.app.domain.cache

import com.emoji.app.domain.AppConfig
import java.security.MessageDigest

/**
 * ⑤ ExportCache — MD5-based cache for export results.
 * Key: MD5(mediaType + URIs join + trimStart + trimEnd + speed + quality + textJson)
 * Value: GIF ByteArray
 * Eviction: cold start clears; max AppConfig.EXPORT_CACHE_MAX_ENTRIES entries
 */
object ExportCache {

    private val maxEntries = AppConfig.EXPORT_CACHE_MAX_ENTRIES

    private val cache = object : LinkedHashMap<String, ByteArray>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean {
            return size > maxEntries
        }
    }

    fun cacheKey(vararg parts: Any): String {
        return parts.joinToString("|") { it.toString() }.md5()
    }

    @Synchronized
    fun get(key: String): ByteArray? = cache[key]

    @Synchronized
    fun put(key: String, data: ByteArray) {
        cache[key] = data
    }

    @Synchronized
    fun clear() { cache.clear() }

    private fun String.md5(): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

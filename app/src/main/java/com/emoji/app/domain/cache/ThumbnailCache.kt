package com.emoji.app.domain.cache

import android.graphics.Bitmap
import android.net.Uri
import com.emoji.app.domain.AppConfig

/**
 * ② ThumbnailCache — LRU cache for photo thumbnails.
 * Key: (photoUri, thumbnailSize), Value: Bitmap
 * Coil's built-in memory cache is used as the primary mechanism;
 * this serves as a secondary explicit cache for preloading.
 */
class ThumbnailCache(private val maxSize: Int = AppConfig.THUMBNAIL_CACHE_MAX) {

    private val cache = object : LinkedHashMap<String, Bitmap>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > maxSize
        }
    }

    private fun key(uri: Uri, size: Int): String = "${uri}@${size}px"

    @Synchronized
    fun get(uri: Uri, size: Int): Bitmap? = cache[key(uri, size)]

    @Synchronized
    fun put(uri: Uri, size: Int, bitmap: Bitmap) {
        cache[key(uri, size)] = bitmap
    }

    @Synchronized
    fun clear() {
        cache.values.forEach { it.recycle() }
        cache.clear()
    }
}

package com.emoji.app.domain.cache

import android.graphics.Bitmap
import android.net.Uri
import com.emoji.app.domain.AppConfig

/**
 * ① VideoFrameCache — LRU cache for extracted video frames.
 * Key: (videoUri, timestampSec), Value: Bitmap
 * Eviction: LRU, max AppConfig.VIDEO_FRAME_CACHE_MAX frames
 * Invalidation: clear on video URI change; shrink on app background
 */
class VideoFrameCache(private val maxSize: Int = AppConfig.VIDEO_FRAME_CACHE_MAX) {

    private val cache = object : LinkedHashMap<String, Bitmap>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > maxSize
        }
    }

    fun key(uri: Uri, tSec: Double): String = "${uri}@${"%.3f".format(tSec)}"

    @Synchronized
    fun get(key: String): Bitmap? = cache[key]

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        cache[key] = bitmap
    }

    @Synchronized
    fun clear() {
        cache.values.forEach { it.recycle() }
        cache.clear()
    }

    @Synchronized
    fun shrinkToBackground() {
        val toRemove = cache.size - AppConfig.VIDEO_FRAME_CACHE_BACKGROUND
        if (toRemove > 0) {
            val entriesToRemove = cache.entries.take(toRemove)
            entriesToRemove.forEach { it.value.recycle() }
            entriesToRemove.forEach { cache.remove(it.key) }
        }
    }

    val size: Int get() = synchronized(this) { cache.size }
}

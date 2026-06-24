package com.emoji.app.domain

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.emoji.app.domain.cache.VideoFrameCache
import com.emoji.app.domain.model.SampleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/**
 * FrameExtractor — extracts frames from video using MediaMetadataRetriever.
 * Supports speed adjustment, frame overflow handling (truncate/uniform),
 * and caching via VideoFrameCache.
 */
class FrameExtractor(
    private val context: Context,
    private val cache: VideoFrameCache = VideoFrameCache(),
) {
    /**
     * Extract frames for GIF export.
     * @param videoUri The video content URI
     * @param trimStartSec Start time in seconds
     * @param trimEndSec End time in seconds
     * @param speed Playback speed multiplier
     * @param fps Target frames per second
     * @param maxFrames Maximum number of frames to export
     * @param sampleMode Overflow mode (TRUNCATE or UNIFORM)
     * @param targetWidth Output width for scaling
     * @param onProgress Progress callback (current, total)
     * @return List of bitmaps ready for text rendering
     */
    suspend fun extractFrames(
        videoUri: Uri,
        trimStartSec: Double,
        trimEndSec: Double,
        speed: Float,
        fps: Int = AppConfig.FPS,
        maxFrames: Int = AppConfig.MAX_FRAMES,
        sampleMode: SampleMode = SampleMode.TRUNCATE,
        targetWidth: Int = AppConfig.DEFAULT_QUALITY.width,
        onProgress: ((Int, Int) -> Unit)? = null,
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)

        val durationSec = trimEndSec - trimStartSec
        val rawFrameCount = ceil(durationSec * fps / speed).toInt().coerceAtLeast(1)
        val totalFrames = rawFrameCount.coerceAtMost(maxFrames)
        val overflow = rawFrameCount > maxFrames

        val frameDelta = speed / fps.toDouble()

        val frames = mutableListOf<Bitmap>()

        for (i in 0 until totalFrames) {
            val t = when {
                overflow && sampleMode == SampleMode.UNIFORM -> {
                    trimStartSec + (i.toDouble() / (totalFrames - 1).coerceAtLeast(1)) * durationSec
                }
                else -> trimStartSec + i * frameDelta
            }

            // Try cache first
            val cacheKey = cache.key(videoUri, t)
            var bitmap = cache.get(cacheKey)

            if (bitmap == null) {
                val timeUs = (t * 1_000_000).toLong()
                bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap != null) {
                    // Scale to target width
                    val scale = targetWidth.toFloat() / bitmap.width
                    val targetHeight = (bitmap.height * scale).toInt()
                    val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                    if (scaled != bitmap) bitmap.recycle()
                    cache.put(cacheKey, scaled)
                    bitmap = scaled
                }
            }

            if (bitmap != null) {
                frames.add(bitmap)
            }

            onProgress?.invoke(i + 1, totalFrames)
        }

        retriever.release()
        frames
    }

    fun clearCache() {
        cache.clear()
    }
}

package com.emoji.app.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.emoji.app.domain.cache.ExportCache
import com.emoji.app.domain.model.MediaType
import com.emoji.app.domain.model.PhotoText
import com.emoji.app.domain.model.SampleMode
import com.emoji.app.viewmodel.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ExportEngine — orchestrates the full GIF export pipeline.
 *
 * Video path: extract frames → render text → encode → cache → save
 * Photo path: load photos → render text → encode → cache → save
 */
class ExportEngine(private val context: Context) {

    private val frameExtractor = FrameExtractor(context)
    private val textRenderer = TextRenderer()

    /**
     * Export video to GIF.
     */
    suspend fun exportVideo(
        state: AppState,
        onProgress: (String, Float) -> Unit,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val uri = state.videoUri ?: return@withContext Result.failure(Exception("No video selected"))

            // Check export cache
            val cacheKey = ExportCache.cacheKey(
                state.mediaType, uri,
                "%.2f".format(state.trimStartSec), "%.2f".format(state.trimEndSec),
                state.speed, state.quality, state.textContent, state.textFont,
                state.textColor, state.textX, state.textY, state.textSizePx
            )
            val cached = ExportCache.get(cacheKey)
            if (cached != null) {
                EmojiLogger.info("视频转GIF", "导出", "命中导出缓存", "size" to "${cached.size / 1024}KB")
                return@withContext Result.success(cached)
            }

            val totalFrames = kotlin.math.ceil(
                (state.trimEndSec - state.trimStartSec) * AppConfig.FPS / state.speed
            ).toInt().coerceAtLeast(1).coerceAtMost(AppConfig.MAX_FRAMES)

            val overflow = kotlin.math.ceil(
                (state.trimEndSec - state.trimStartSec) * AppConfig.FPS / state.speed
            ).toInt() > AppConfig.MAX_FRAMES

            val sampleMode = if (overflow) state.sampleMode else SampleMode.TRUNCATE

            onProgress("提取帧...", 0f)

            // Extract frames
            val frames = frameExtractor.extractFrames(
                videoUri = uri,
                trimStartSec = state.trimStartSec,
                trimEndSec = state.trimEndSec,
                speed = state.speed,
                fps = AppConfig.FPS,
                maxFrames = AppConfig.MAX_FRAMES,
                sampleMode = sampleMode,
                targetWidth = state.quality.width,
                onProgress = { current, total ->
                    onProgress("提取帧 $current/$total", current.toFloat() / total * 0.5f)
                }
            )

            // Render text on each frame
            onProgress("渲染文字...", 0.5f)
            val renderedFrames = frames.mapIndexed { i, frame ->
                textRenderer.renderText(
                    bitmap = frame,
                    text = state.textContent,
                    fontName = state.textFont,
                    textColor = state.textColor,
                    weight = state.textWeight,
                    fontSizePx = state.textSizePx,
                    textX = state.textX,
                    textY = state.textY,
                )
                onProgress("渲染 ${i + 1}/${frames.size}", 0.5f + (i + 1).toFloat() / frames.size * 0.2f)
                frame
            }

            // Encode GIF
            onProgress("编码 GIF...", 0.7f)
            val encoder = GifEncoder(
                width = state.quality.width,
                height = renderedFrames.firstOrNull()?.height ?: state.quality.width,
                quality = state.quality.gifSample,
            )

            renderedFrames.forEachIndexed { i, frame ->
                encoder.addFrame(frame, (1000 / AppConfig.FPS))
                onProgress("编码中 ${((i + 1).toFloat() / renderedFrames.size * 100).toInt()}%",
                    0.7f + (i + 1).toFloat() / renderedFrames.size * 0.3f)
            }

            val gifData = encoder.finish()

            // Cache the result
            ExportCache.put(cacheKey, gifData)

            // Clean up
            renderedFrames.forEach { it.recycle() }
            textRenderer.clearMetricsCache()

            EmojiLogger.info("视频转GIF", "导出", "导出完成",
                "size" to "${gifData.size / 1024}KB",
                "frames" to renderedFrames.size,
                "quality" to state.quality.label
            )

            Result.success(gifData)
        } catch (e: Exception) {
            EmojiLogger.error("视频转GIF", "导出", "导出失败",
                "errorMessage" to (e.message ?: "unknown"),
                "errorCode" to (e.hashCode().toString())
            )
            Result.failure(e)
        }
    }

    /**
     * Export photos to GIF.
     */
    suspend fun exportPhotos(
        state: AppState,
        onProgress: (String, Float) -> Unit,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (state.photos.isEmpty()) return@withContext Result.failure(Exception("No photos selected"))

            val isSingle = state.photos.size == 1
            val totalFrames = state.photos.size

            // Check export cache
            val cacheKey = ExportCache.cacheKey(
                state.mediaType, state.photos.joinToString(","),
                state.frameDelay, state.photoTexts.joinToString(";") { it?.content ?: "" },
                state.textFont, state.textColor, state.textX, state.textY, state.textSizePx
            )
            val cached = ExportCache.get(cacheKey)
            if (cached != null) {
                EmojiLogger.info("图片转GIF", "导出", "命中导出缓存", "size" to "${cached.size / 1024}KB")
                return@withContext Result.success(cached)
            }

            onProgress("加载照片...", 0f)

            // Determine output size from first photo
            val firstUri = state.photos.first()
            val firstBitmap = loadBitmapFromUri(firstUri)
                ?: return@withContext Result.failure(Exception("Failed to load photo"))

            val outputWidth = AppConfig.PHOTO_OUTPUT_WIDTH
            val scale = outputWidth.toFloat() / firstBitmap.width
            val outputHeight = (firstBitmap.height * scale).toInt().coerceAtLeast(1)
            firstBitmap.recycle()

            val encoder = GifEncoder(
                width = outputWidth,
                height = outputHeight,
                quality = AppConfig.DEFAULT_QUALITY.gifSample,
            )

            for (i in 0 until totalFrames) {
                onProgress("合成帧 ${i + 1}/$totalFrames", (i + 1).toFloat() / totalFrames * 0.7f)

                val bitmap = loadBitmapFromUri(state.photos[i])
                    ?: continue

                // Scale to output size
                val scaled = Bitmap.createScaledBitmap(bitmap, outputWidth, outputHeight, true)
                if (scaled != bitmap) bitmap.recycle()

                // Get text for this frame
                val photoText: PhotoText? = if (isSingle) {
                    state.photoTexts.getOrNull(0) ?: PhotoText(
                        content = state.textContent,
                        fontName = state.textFont,
                        color = state.textColor,
                        weight = state.textWeight,
                        x = state.textX,
                        y = state.textY,
                        size = state.textSizePx,
                    )
                } else {
                    // Use inheritance
                    var pt: PhotoText? = null
                    for (j in i downTo 0) {
                        pt = state.photoTexts.getOrNull(j)
                        if (pt != null) break
                    }
                    pt ?: PhotoText(
                        content = "",
                        fontName = state.textFont,
                        color = state.textColor,
                        weight = state.textWeight,
                        x = state.textX,
                        y = state.textY,
                        size = state.textSizePx,
                    )
                }

                // Render text
                if (photoText != null && photoText.content.isNotEmpty()) {
                    textRenderer.renderText(
                        bitmap = scaled,
                        text = photoText.content,
                        fontName = photoText.fontName,
                        textColor = photoText.color,
                        weight = photoText.weight,
                        fontSizePx = photoText.size,
                        textX = photoText.x,
                        textY = photoText.y,
                    )
                }

                val delayMs = if (isSingle) AppConfig.GIF_DELAY_DIVISOR * 10 else state.frameDelay
                encoder.addFrame(scaled, delayMs)
                scaled.recycle()
            }

            onProgress("编码 GIF...", 0.85f)
            val gifData = encoder.finish()

            // Cache
            ExportCache.put(cacheKey, gifData)
            textRenderer.clearMetricsCache()

            EmojiLogger.info("图片转GIF", "导出", "导出完成",
                "size" to "${gifData.size / 1024}KB",
                "frames" to totalFrames,
                "format" to "GIF"
            )

            Result.success(gifData)
        } catch (e: Exception) {
            EmojiLogger.error("图片转GIF", "导出", "导出失败",
                "errorMessage" to (e.message ?: "unknown")
            )
            Result.failure(e)
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            EmojiLogger.error("System", "StorageCheck", "加载图片失败", "uri" to uri.toString())
            null
        }
    }
}

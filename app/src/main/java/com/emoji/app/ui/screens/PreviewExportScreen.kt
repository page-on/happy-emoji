package com.emoji.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.emoji.app.data.MediaRepository
import com.emoji.app.domain.EmojiLogger
import com.emoji.app.domain.ExportEngine
import com.emoji.app.domain.AppConfig
import com.emoji.app.domain.model.MediaType
import com.emoji.app.domain.model.PhotoText
import com.emoji.app.ui.components.NativeTypeface
import com.emoji.app.ui.components.QualitySelector
import com.emoji.app.ui.theme.BackgroundDark
import com.emoji.app.ui.theme.BorderGray
import com.emoji.app.ui.theme.Purple500
import com.emoji.app.ui.theme.SuccessGreen
import com.emoji.app.ui.theme.SurfaceDark
import com.emoji.app.ui.theme.TextHint
import com.emoji.app.ui.theme.TextPrimary
import com.emoji.app.ui.theme.TextSecondary
import com.emoji.app.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun PreviewExportScreen(
    viewModel: SharedViewModel,
) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val isVideo = state.mediaType == MediaType.VIDEO
    val isSingle = viewModel.isSinglePhoto
    val isMulti = viewModel.isMultiPhoto
    val photos = state.photos
    var videoAspectRatio by remember { mutableStateOf(1f) }
    val engine = remember { ExportEngine(ctx) }
    val repo = remember { MediaRepository }

    // Auto-export on entering this screen (if not already exported)
    LaunchedEffect(Unit) {
        if (state.gifBlob != null || state.exporting) return@LaunchedEffect
        viewModel.setExporting(true)
        viewModel.setExportProgress("准备导出...", 0f)
        val appState = viewModel.state.value
        val result = if (isVideo) engine.exportVideo(appState) { text, fraction ->
            viewModel.setExportProgress(text, fraction)
        } else engine.exportPhotos(appState) { text, fraction ->
            viewModel.setExportProgress(text, fraction)
        }
        viewModel.setExporting(false)
        result.fold(
            onSuccess = { gifData ->
                viewModel.setGifBlob(gifData)
                repo.saveGifToGallery(ctx, gifData)
                EmojiLogger.info(
                    if (isVideo) "视频转GIF" else "图片转GIF",
                    "导出", "导出完成",
                    "size" to "${gifData.size / 1024}KB"
                )
            },
            onFailure = { e ->
                EmojiLogger.error(
                    if (isVideo) "视频转GIF" else "图片转GIF",
                    "导出", "导出失败",
                    "errorMessage" to (e.message ?: "unknown")
                )
            }
        )
    }

    // Get video aspect ratio from a single frame
    LaunchedEffect(state.videoUri) {
        if (isVideo && state.videoUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val r = android.media.MediaMetadataRetriever()
                    r.setDataSource(ctx, state.videoUri!!)
                    val raw = r.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST)
                    r.release()
                    if (raw != null) {
                        videoAspectRatio = raw.width.toFloat() / raw.height.toFloat()
                    }
                } catch (_: Exception) { videoAspectRatio = 16f / 9f }
            }
        }
    }

    var slideIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(isMulti, state.frameDelay) {
        if (isMulti && photos.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(state.frameDelay.toLong())
                slideIdx = (slideIdx + 1) % photos.size
            }
        }
    }

    // --- Text overlay bitmap for preview (matching TextEditScreen) ---
    var previewTextBmp by remember { mutableStateOf<Bitmap?>(null) }
    var previewAreaW by remember { mutableFloatStateOf(1f) }
    var previewAreaH by remember { mutableFloatStateOf(1f) }

    /** Resolve text config for the current slide index (multi-photo uses per-frame photoTexts). */
    fun resolveSlideText(): PhotoText? {
        if (!isMulti || state.photos.isEmpty()) {
            return if (state.textContent.isNotEmpty()) PhotoText(
                content = state.textContent, fontName = state.textFont, color = state.textColor,
                weight = state.textWeight, x = state.textX, y = state.textY, size = state.textSizePx
            ) else null
        }
        // Multi-photo: find nearest non-null text backwards from slideIdx
        for (j in slideIdx downTo 0) {
            val pt = state.photoTexts.getOrNull(j)
            if (pt != null && pt.content.isNotEmpty()) return pt
        }
        return null
    }

    fun rebuildOverlay() {
        val slideText = resolveSlideText()
        if (slideText == null) { previewTextBmp = null; return }
        val w = previewAreaW.roundToInt().coerceAtLeast(1)
        val h = previewAreaH.roundToInt().coerceAtLeast(1)
        if (w <= 10 || h <= 10) return
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val tf = NativeTypeface.forLabel(slideText.fontName)
        val fontSize = slideText.size * w.toFloat() / AppConfig.BASE_CANVAS_WIDTH_PX
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = fontSize
            color = slideText.color
            textAlign = Paint.Align.CENTER
            isFakeBoldText = NativeTypeface.isBold(slideText.fontName)
            setShadowLayer(4f, 2f, 2f, 0x77000000.toInt())
        }
        canvas.drawText(slideText.content, w.toFloat() * slideText.x, h.toFloat() * slideText.y + fontSize * 0.35f, paint)
        paint.clearShadowLayer()
        canvas.drawText(slideText.content, w.toFloat() * slideText.x, h.toFloat() * slideText.y + fontSize * 0.35f, paint)
        previewTextBmp = bmp
    }

    LaunchedEffect(state.textContent, state.textFont, state.textColor, state.textSizePx, state.textX, state.textY, slideIdx) {
        rebuildOverlay()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark).verticalScroll(rememberScrollState())
    ) {
        // === Preview area ===
        Box(
            modifier = Modifier.fillMaxWidth()
                .aspectRatio(if (isVideo) videoAspectRatio else 1f)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp)).background(Color.Black)
                .onSizeChanged { previewAreaW = it.width.toFloat(); previewAreaH = it.height.toFloat(); rebuildOverlay() },
            contentAlignment = Alignment.Center,
        ) {
            if (isVideo && state.videoUri != null) {
                // Hardware video preview (same as VideoTrimScreen)
                var mp by remember { mutableStateOf<MediaPlayer?>(null) }
                val videoUri = state.videoUri!!
                AndroidView(
                    factory = { context ->
                        TextureView(context).also { tv ->
                            tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, w: Int, h: Int) {
                                    MediaPlayer().apply {
                                        setDataSource(context, videoUri)
                                        setSurface(android.view.Surface(st))
                                        setOnPreparedListener {
                                            val pb = PlaybackParams().apply { speed = state.speed }
                                            try { setPlaybackParams(pb) } catch (_: Exception) {}
                                            seekTo((state.trimStartSec * 1000).toInt())
                                            start()
                                        }
                                        setOnCompletionListener {
                                            seekTo((state.trimStartSec * 1000).toInt())
                                            start()
                                        }
                                        prepareAsync()
                                        mp = this
                                    }
                                }
                                override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, w: Int, h: Int) {}
                                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean { mp?.release(); mp = null; return true }
                                override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                DisposableEffect(Unit) { onDispose { mp?.release(); mp = null } }
            } else if (!isVideo && photos.isNotEmpty()) {
                val idx = if (isSingle) 0 else slideIdx.coerceIn(0, photos.size - 1)
                AsyncImage(model = photos[idx], contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Text("暂无预览", color = TextHint, fontSize = 14.sp)
            }
            // Text overlay (rendered as bitmap, native Typeface)
            if (previewTextBmp != null) {
                Image(bitmap = previewTextBmp!!.asImageBitmap(), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }

        // === Info + progress ===
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceDark).padding(8.dp)) {
                if (isVideo) {
                    Text("视频: ${formatTime(state.trimStartSec)}~${formatTime(state.trimEndSec)} · ${"%.1f".format(state.speed)}x · ${state.quality.label}(${state.quality.width}px)",
                        fontSize = 12.sp, color = TextSecondary)
                } else {
                    Text("${photos.size}张 · ${if (isSingle) "单帧GIF（静图）" else "多帧GIF · ${state.frameDelay}ms"} · ${AppConfig.PHOTO_OUTPUT_WIDTH}px",
                        fontSize = 12.sp, color = TextSecondary)
                }
            }

            if (isVideo) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceDark).padding(6.dp)) {
                    QualitySelector(currentQuality = state.quality, onQualitySelected = { viewModel.setQuality(it) })
                }
            }

            if (state.exporting) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceDark).padding(8.dp)) {
                    Column {
                        Text(state.exportProgressText, fontSize = 12.sp, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(progress = state.exportProgressFraction,
                            modifier = Modifier.fillMaxWidth().height(4.dp), color = Purple500)
                    }
                }
            }

            if (state.gifBlob != null && !state.exporting) {
                Text("✅ 导出完成！${state.gifBlob!!.size / 1024}KB", color = SuccessGreen,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // === Bottom button ===
        Button(
            onClick = { viewModel.reset() },
            enabled = !state.exporting,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp).height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple500,
                disabledContainerColor = SurfaceDark,
                disabledContentColor = TextHint,
            ),
        ) { Text("返回首页", fontSize = 14.sp) }
    }
}

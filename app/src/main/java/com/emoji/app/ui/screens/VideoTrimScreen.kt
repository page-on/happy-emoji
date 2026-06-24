package com.emoji.app.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidView
import com.emoji.app.domain.AppConfig
import com.emoji.app.domain.model.SampleMode
import com.emoji.app.ui.components.SpeedSelector
import com.emoji.app.ui.theme.BackgroundDark
import com.emoji.app.ui.theme.BorderGray
import com.emoji.app.ui.theme.ErrorRed
import com.emoji.app.ui.theme.InfoBlue
import com.emoji.app.ui.theme.Purple500
import com.emoji.app.ui.theme.SurfaceDark
import com.emoji.app.ui.theme.TextHint
import com.emoji.app.ui.theme.TextPrimary
import com.emoji.app.ui.theme.TextSecondary
import com.emoji.app.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun formatTime(sec: Double): String {
    val m = (sec / 60).toInt()
    return "${m}:${"%.1f".format(sec % 60).padStart(4, '0')}"
}

@Composable
fun VideoTrimScreen(
    viewModel: SharedViewModel,
    videoUri: Uri,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val dur = state.videoDurationSec
    val maxTrim = AppConfig.TRIM_MAX_DURATION_SEC.toDouble()
    val trimDur = state.trimEndSec - state.trimStartSec
    val rawFrames = (trimDur * AppConfig.FPS / state.speed).toInt().coerceAtLeast(1)
    val exportFrames = rawFrames.coerceAtMost(AppConfig.MAX_FRAMES)
    val overflow = rawFrames > AppConfig.MAX_FRAMES
    val estKB = (exportFrames * state.quality.width * 0.018).toInt()

    var isPreviewing by remember { mutableStateOf(false) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var videoAspect by remember { mutableFloatStateOf(1f) }

    // Fix: ensure video duration and trim are properly initialized.
    // MaterialPickScreen's METADATA_KEY_DURATION may return null on some devices/formats.
    LaunchedEffect(videoUri) {
        if (dur <= 0.0 || state.trimEndSec <= 0.0) {
            withContext(Dispatchers.IO) {
                try {
                    val r = MediaMetadataRetriever()
                    r.setDataSource(ctx, videoUri)
                    val durStr = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    r.release()
                    val realDur = (durStr?.toLongOrNull() ?: 10_000) / 1000.0
                    // Re-call setVideo with the correct duration; this also resets trim correctly
                    viewModel.setVideo(videoUri, realDur.coerceAtMost(AppConfig.VIDEO_MAX_DURATION_SEC.toDouble()))
                } catch (_: Exception) {
                    viewModel.setVideo(videoUri, maxTrim)
                }
            }
        }
    }

    // Static frame for when video is NOT previewing (single extract, fine)
    var staticFrame by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(videoUri, state.trimStartSec) {
        withContext(Dispatchers.IO) {
            try {
                val r = MediaMetadataRetriever()
                r.setDataSource(ctx, videoUri)
                val raw = r.getFrameAtTime(
                    (state.trimStartSec * 1_000_000).toLong(),
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                r.release()
                if (raw != null) {
                    videoAspect = raw.width.toFloat() / raw.height.toFloat()
                    if (raw.width > 480) {
                        val h = (480f / raw.width * raw.height).toInt()
                        val s = Bitmap.createScaledBitmap(raw, 480, h, true)
                        if (s != raw) raw.recycle()
                        staticFrame = s
                    } else { staticFrame = raw }
                }
            } catch (_: Exception) { staticFrame = null }
        }
    }

    // Stop preview on any user interaction
    LaunchedEffect(state.trimStartSec, state.trimEndSec, state.speed) {
        if (isPreviewing) isPreviewing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // === Preview area: TextureView + MediaPlayer for hardware-smooth playback ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspect)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (isPreviewing) {
                // Smooth hardware video playback via MediaPlayer
                var mp by remember { mutableStateOf<MediaPlayer?>(null) }
                AndroidView(
                    factory = { context ->
                        TextureView(context).also { tv ->
                            tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, w: Int, h: Int) {
                                    val player = MediaPlayer().apply {
                                        setDataSource(context, videoUri)
                                        setSurface(android.view.Surface(st))
                                        setOnPreparedListener {
                                            val pb = PlaybackParams()
                                            pb.speed = state.speed
                                            try { setPlaybackParams(pb) } catch (_: Exception) {}
                                            seekTo((state.trimStartSec * 1000).toInt())
                                            start()
                                        }
                                        setOnCompletionListener {
                                            seekTo((state.trimStartSec * 1000).toInt())
                                            start()
                                        }
                                        isLooping = false
                                        prepareAsync()
                                    }
                                    mp = player
                                }
                                override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, w: Int, h: Int) {}
                                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                                    mp?.release(); mp = null; return true
                                }
                                override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                DisposableEffect(Unit) {
                    onDispose { mp?.release(); mp = null }
                }
            } else {
                // Static frame when not playing
                if (staticFrame != null) {
                    androidx.compose.foundation.Image(
                        bitmap = staticFrame!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    )
                } else {
                    Text("🎬 加载中...", color = TextHint, fontSize = 14.sp)
                }
            }
        }

        // === Controls ===
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark).padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("裁剪范围（最长${AppConfig.TRIM_MAX_DURATION_SEC}秒）", fontSize = 11.sp, color = TextSecondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("起始: ${formatTime(state.trimStartSec)}", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("时长: ${"%.1f".format(trimDur)}s", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("结束: ${formatTime(state.trimEndSec)}", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    // Slider track — BoxWithConstraints gives real width on first frame
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.DarkGray)
                    ) {
                        val trackDp = maxWidth
                        val trackPx = with(density) { trackDp.toPx() }
                        trackWidthPx = trackPx
                        val startF = (state.trimStartSec / dur).toFloat().coerceIn(0f, 1f)
                        val endF = (state.trimEndSec / dur).toFloat().coerceIn(0f, 1f)
                        val startDp = trackDp * startF
                        val endDp = trackDp * endF
                        val selW = (endDp - startDp).coerceAtLeast(0.dp)

                        Box(Modifier.width(startDp).fillMaxHeight().background(Color.Black.copy(0.55f)))
                        Box(Modifier.offset(x = endDp).width((trackDp - endDp).coerceAtLeast(0.dp)).fillMaxHeight().background(Color.Black.copy(0.55f)))
                        Box(Modifier.offset(x = startDp).width(selW).fillMaxHeight().border(3.dp, Purple500))

                        Box(Modifier.offset(x = (startDp - 10.dp).coerceAtLeast(0.dp), y = (-4).dp).width(20.dp).height(56.dp).background(Purple500, RoundedCornerShape(4.dp)).pointerInput(Unit) {
                            detectHorizontalDragGestures { _, drag ->
                                val d = drag / trackWidthPx * dur
                                var ns = (state.trimStartSec + d).coerceIn(0.0, state.trimEndSec - 0.1)
                                var ne = state.trimEndSec
                                if (ne - ns > maxTrim) ne = (ns + maxTrim).coerceAtMost(dur)
                                viewModel.setTrim(ns, ne)
                            }
                        }, contentAlignment = Alignment.BottomCenter) {
                            Text(formatTime(state.trimStartSec), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-24).dp))
                        }
                        Box(Modifier.offset(x = endDp - 10.dp, y = (-4).dp).width(20.dp).height(56.dp).background(Purple500, RoundedCornerShape(4.dp)).pointerInput(Unit) {
                            detectHorizontalDragGestures { _, drag ->
                                val d = drag / trackWidthPx * dur
                                var ne = (state.trimEndSec + d).coerceIn(state.trimStartSec + 0.1, dur)
                                var ns = state.trimStartSec
                                if (ne - ns > maxTrim) ns = (ne - maxTrim).coerceAtLeast(0.0)
                                viewModel.setTrim(ns, ne)
                            }
                        }, contentAlignment = Alignment.BottomCenter) {
                            Text(formatTime(trimDur), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-24).dp))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("倍速:", fontSize = 11.sp, color = TextSecondary)
                        Spacer(Modifier.width(6.dp))
                        SpeedSelector(currentSpeed = state.speed, onSpeedSelected = { viewModel.setSpeed(it) })
                    }

                    val ec = when { overflow && state.sampleMode == SampleMode.UNIFORM -> InfoBlue; overflow -> ErrorRed; else -> TextSecondary }
                    Text(
                        when {
                            overflow && state.sampleMode == SampleMode.UNIFORM -> "预计 $exportFrames 帧（均匀采样自 $rawFrames 帧）/ 约 ${estKB}KB"
                            overflow -> "预计 $exportFrames 帧（已截断）/ 约 ${estKB}KB"
                            else -> "预计 $exportFrames 帧 / 约 ${estKB}KB"
                        }, fontSize = 11.sp, color = ec
                    )
                    if (overflow) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (state.sampleMode == SampleMode.UNIFORM) {
                                OutlinedButton({ viewModel.setSampleMode(SampleMode.TRUNCATE) }, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, BorderGray), colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text("取消采样", fontSize = 11.sp, color = InfoBlue) }
                            } else {
                                OutlinedButton({}, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, BorderGray), colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text("缩短选段", fontSize = 11.sp, color = ErrorRed) }
                                OutlinedButton({ viewModel.setSampleMode(SampleMode.UNIFORM) }, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, BorderGray), colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text("均匀采样", fontSize = 11.sp, color = InfoBlue) }
                            }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceDark).padding(10.dp), contentAlignment = Alignment.Center) {
                OutlinedButton(
                    onClick = { isPreviewing = !isPreviewing },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Purple500),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple500),
                ) { Text(if (isPreviewing) "■ 停止预览" else "▶ 预览片段", fontSize = 14.sp) }
            }
            Spacer(Modifier.height(4.dp))
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ viewModel.setStep(0); onNavigateBack() }, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Purple500), colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple500)) { Text("上一步", fontSize = 14.sp) }
            Button({ viewModel.setStep(2); onNavigateNext() }, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple500)) { Text("下一步：添加文字", fontSize = 14.sp) }
        }
    }
}

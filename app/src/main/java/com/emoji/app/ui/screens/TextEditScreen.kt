package com.emoji.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.emoji.app.domain.AppConfig
import com.emoji.app.domain.model.MediaType
import com.emoji.app.domain.model.PhotoText
import com.emoji.app.ui.components.ColorSelector
import com.emoji.app.ui.components.FontSelector
import com.emoji.app.ui.components.FontSizeSlider
import com.emoji.app.ui.components.NativeTypeface
import com.emoji.app.ui.theme.BackgroundDark
import com.emoji.app.ui.theme.BorderGray
import com.emoji.app.ui.theme.Purple500
import com.emoji.app.ui.theme.SurfaceDark
import com.emoji.app.ui.theme.TextHint
import com.emoji.app.ui.theme.TextPrimary
import com.emoji.app.ui.theme.TextSecondary
import com.emoji.app.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun TextEditScreen(
    viewModel: SharedViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val isVideo = state.mediaType == MediaType.VIDEO
    val isSinglePhoto = viewModel.isSinglePhoto
    val isMultiPhoto = viewModel.isMultiPhoto
    val photos = state.photos

    var previewWidth by remember { mutableFloatStateOf(1f) }
    var previewHeight by remember { mutableFloatStateOf(1f) }
    val colorPresets = AppConfig.COLOR_PRESETS.map { Color(it) }
    var videoFrameBmp by remember { mutableStateOf<Bitmap?>(null) }
    var overlayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Video middle-frame preview
    LaunchedEffect(state.videoUri, state.trimStartSec, state.trimEndSec) {
        if (isVideo && state.videoUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val mid = (state.trimStartSec + state.trimEndSec) / 2.0
                    val r = MediaMetadataRetriever()
                    r.setDataSource(ctx, state.videoUri!!)
                    videoFrameBmp = r.getFrameAtTime((mid * 1_000_000).toLong(), MediaMetadataRetriever.OPTION_CLOSEST)
                    r.release()
                } catch (_: Exception) { videoFrameBmp = null }
            }
        }
    }

    // === Render text overlay as Bitmap with native Typeface ===
    fun renderTextOverlay(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val tf = NativeTypeface.forLabel(state.textFont)
        val fontSize = (state.textSizePx * w.toFloat() / AppConfig.BASE_CANVAS_WIDTH_PX)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = fontSize
            color = state.textColor
            textAlign = Paint.Align.CENTER
            isFakeBoldText = NativeTypeface.isBold(state.textFont)
            setShadowLayer(4f, 2f, 2f, 0x77000000.toInt())
        }
        canvas.drawText(state.textContent, w.toFloat() * state.textX, h.toFloat() * state.textY + fontSize * 0.35f, paint)
        paint.clearShadowLayer()
        canvas.drawText(state.textContent, w.toFloat() * state.textX, h.toFloat() * state.textY + fontSize * 0.35f, paint)
        return bmp
    }

    LaunchedEffect(state.textContent, state.textFont, state.textColor,
        state.textSizePx, state.textX, state.textY, previewWidth, previewHeight) {
        val w = previewWidth.roundToInt().coerceAtLeast(1)
        val h = previewHeight.roundToInt().coerceAtLeast(1)
        overlayBitmap = if (state.textContent.isNotEmpty() && w > 10 && h > 10) {
            withContext(Dispatchers.Default) { try { renderTextOverlay(w, h) } catch (_: Exception) { null } }
        } else null
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark).verticalScroll(rememberScrollState())
    ) {
        // Preview area
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(12.dp)
                .clip(RoundedCornerShape(10.dp)).background(Color.Black)
                .onSizeChanged { previewWidth = it.width.toFloat(); previewHeight = it.height.toFloat() },
            contentAlignment = Alignment.Center,
        ) {
            if (isVideo) {
                if (videoFrameBmp != null) {
                    Image(bitmap = videoFrameBmp!!.asImageBitmap(), contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else Text("🎬", fontSize = 48.sp, color = TextHint)
            } else {
                val idx = if (isSinglePhoto) 0 else state.currentPhotoIdx
                if (photos.isNotEmpty() && idx < photos.size) {
                    AsyncImage(model = photos[idx], contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }

            // Text overlay (native typeface bitmap with drag + double-tap)
            if (overlayBitmap != null) {
                Image(bitmap = overlayBitmap!!.asImageBitmap(), contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.setTextPosition(
                                    (state.textX + dragAmount.x / previewWidth).coerceIn(0.05f, 0.95f),
                                    (state.textY + dragAmount.y / previewHeight).coerceIn(0.05f, 0.95f))
                                if (isMultiPhoto) saveCurrentPhotoText(viewModel)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                viewModel.setTextPosition(0.5f, 0.5f)
                                if (isMultiPhoto) saveCurrentPhotoText(viewModel)
                            })
                        },
                    contentScale = ContentScale.Fit)
            } else if (state.textContent.isEmpty()) {
                Text("输入文字", color = Color(state.textColor).copy(alpha = 0.4f),
                    fontSize = state.textSizePx.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                        .offset { IntOffset(
                            ((state.textX - 0.5f) * previewWidth).roundToInt(),
                            ((state.textY - 0.5f) * previewHeight).roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.setTextPosition(
                                    (state.textX + dragAmount.x / previewWidth).coerceIn(0.05f, 0.95f),
                                    (state.textY + dragAmount.y / previewHeight).coerceIn(0.05f, 0.95f))
                                if (isMultiPhoto) saveCurrentPhotoText(viewModel)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                viewModel.setTextPosition(0.5f, 0.5f)
                                if (isMultiPhoto) saveCurrentPhotoText(viewModel)
                            })
                        })
            }
        }

        // Photo navigator
        if (isMultiPhoto) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { saveCurrentPhotoText(viewModel)
                        if (state.currentPhotoIdx > 0) { viewModel.setCurrentPhotoIdx(state.currentPhotoIdx - 1); loadCurrentPhotoText(viewModel) } },
                    enabled = state.currentPhotoIdx > 0, shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BorderGray),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark),
                    modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp),
                ) { Text("←", color = TextPrimary, fontSize = 16.sp) }
                Text(" ${state.currentPhotoIdx + 1} / ${photos.size} ", color = TextSecondary, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp))
                OutlinedButton(
                    onClick = { saveCurrentPhotoText(viewModel)
                        if (state.currentPhotoIdx < photos.size - 1) { viewModel.setCurrentPhotoIdx(state.currentPhotoIdx + 1); loadCurrentPhotoText(viewModel) } },
                    enabled = state.currentPhotoIdx < photos.size - 1, shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BorderGray),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark),
                    modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp),
                ) { Text("→", color = TextPrimary, fontSize = 16.sp) }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Controls
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp)).background(SurfaceDark).padding(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text("文字内容（最多${AppConfig.TEXT_MAX_LENGTH}字）", fontSize = 11.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = state.textContent,
                        onValueChange = { v ->
                            viewModel.setTextContent(v.take(AppConfig.TEXT_MAX_LENGTH))
                            if (isMultiPhoto) saveCurrentPhotoText(viewModel) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入表情包配文...", color = TextHint) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            cursorColor = Purple500, focusedBorderColor = Purple500, unfocusedBorderColor = BorderGray,
                            focusedContainerColor = BackgroundDark, unfocusedContainerColor = BackgroundDark))
                    Text("${state.textContent.length}/${AppConfig.TEXT_MAX_LENGTH}",
                        fontSize = 11.sp, color = TextHint, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                }
                Column {
                    Text("字体", fontSize = 11.sp, color = TextSecondary); Spacer(Modifier.height(4.dp))
                    FontSelector(currentFontName = state.textFont, onFontSelected = { f ->
                        viewModel.setTextFont(f.label); viewModel.setTextWeight(f.weight)
                        if (isMultiPhoto) saveCurrentPhotoText(viewModel) })
                }
                Column {
                    Text("颜色", fontSize = 11.sp, color = TextSecondary); Spacer(Modifier.height(4.dp))
                    ColorSelector(colors = colorPresets, currentColor = state.textColor, onColorSelected = { c ->
                        viewModel.setTextColor(c)
                        if (isMultiPhoto) saveCurrentPhotoText(viewModel) })
                }
                FontSizeSlider(currentSizePx = state.textSizePx, onSizeChanged = { s ->
                    viewModel.setTextSize(s)
                    if (isMultiPhoto) saveCurrentPhotoText(viewModel) })
                Text("💡 拖拽文字调整位置 · 双击居中", fontSize = 12.sp, color = TextHint,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(8.dp))

        // Bottom buttons
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (isMultiPhoto) saveCurrentPhotoText(viewModel)
                    viewModel.setStep(when { isVideo -> 1; isSinglePhoto -> 0; else -> 1 }); onNavigateBack() },
                modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Purple500),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple500),
            ) { Text("上一步", fontSize = 14.sp) }
            Button(
                onClick = { if (isMultiPhoto) saveCurrentPhotoText(viewModel)
                    viewModel.setStep(when { isVideo -> 3; isSinglePhoto -> 2; else -> 3 }); onNavigateNext() },
                modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple500),
            ) { Text("导出GIF", fontSize = 14.sp) }
        }
    }
}

private fun saveCurrentPhotoText(viewModel: SharedViewModel) {
    val s = viewModel.state.value
    viewModel.savePhotoText(s.currentPhotoIdx, PhotoText(
        s.textContent, s.textFont, s.textColor, s.textWeight, s.textX, s.textY, s.textSizePx))
}
private fun loadCurrentPhotoText(viewModel: SharedViewModel) {
    val pt = viewModel.getPhotoText(viewModel.state.value.currentPhotoIdx) ?: return
    viewModel.setTextContent(pt.content); viewModel.setTextFont(pt.fontName)
    viewModel.setTextColor(pt.color); viewModel.setTextWeight(pt.weight)
    viewModel.setTextPosition(pt.x, pt.y); viewModel.setTextSize(pt.size)
}

package com.emoji.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.emoji.app.domain.AppConfig
import com.emoji.app.ui.theme.BackgroundDark
import com.emoji.app.ui.theme.Purple500
import com.emoji.app.ui.theme.SurfaceDark
import com.emoji.app.ui.theme.TextHint
import com.emoji.app.ui.theme.TextSecondary
import com.emoji.app.viewmodel.SharedViewModel

private const val DRAG_SPEED = 1.0f

@Composable
fun PhotoOrderScreen(
    viewModel: SharedViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val thumbDp = AppConfig.THUMBNAIL_SIZE_DP

    LaunchedEffect(state.photos.size) {
        if (state.photos.size == 1) { viewModel.setStep(1); onNavigateNext() }
    }

    val totalFrames = state.photos.size
    val totalSec = totalFrames * state.frameDelay / 1000.0
    val estKB = (totalFrames * AppConfig.PHOTO_OUTPUT_WIDTH * 0.018).toInt()

    // ── Simple, robust drag state ──
    var draggedUri by remember { mutableStateOf<Uri?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) } // raw finger offset from drag-start position
    val centers = remember { mutableStateMapOf<Uri, Offset>() } // root-relative, updated live except during drag
    val isAnyDragging = draggedUri != null

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark)
    ) {
        Text(
            "帧排序（${state.photos.size}/${AppConfig.PHOTO_MAX_COUNT} 张）— 长按拖拽排序，点击 ✕ 删除",
            fontSize = 11.sp, color = TextHint,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = thumbDp.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.photos, key = { it }) { uri ->
                val isDragging = draggedUri == uri
                val idx = state.photos.indexOf(uri)

                Box(
                    modifier = Modifier
                        .size(thumbDp.dp)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.08f; scaleY = 1.08f
                            }
                        }
                        .onGloballyPositioned { coords ->
                            if (!isDragging) {
                                val pos = coords.positionInRoot()
                                val sz = coords.size
                                centers[uri] = Offset(pos.x + sz.width / 2f, pos.y + sz.height / 2f)
                            }
                        }
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDragging) Purple500.copy(alpha = 0.6f) else Color.DarkGray)
                        .pointerInput(uri) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedUri = uri
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += Offset(amount.x * DRAG_SPEED, amount.y * DRAG_SPEED)
                                },
                                onDragEnd = {
                                    val dUri = draggedUri ?: return@detectDragGesturesAfterLongPress
                                    // Find which item's center is closest to the dragged item's current position
                                    val myC = centers[dUri] ?: Offset.Zero
                                    val fingerC = Offset(myC.x + dragOffset.x, myC.y + dragOffset.y)
                                    var bestIdx = -1
                                    var bestDist = Float.MAX_VALUE
                                    for ((u, c) in centers) {
                                        if (u == dUri) continue
                                        val dx = fingerC.x - c.x; val dy = fingerC.y - c.y
                                        val d = dx * dx + dy * dy
                                        if (d < bestDist) { bestDist = d; bestIdx = state.photos.indexOf(u).takeIf { it >= 0 } ?: continue }
                                    }
                                    val fromIdx = state.photos.indexOf(dUri)
                                    if (bestIdx >= 0 && fromIdx >= 0 && bestIdx != fromIdx) {
                                        viewModel.swapPhotos(fromIdx, bestIdx)
                                    }
                                    draggedUri = null; dragOffset = Offset.Zero
                                },
                                onDragCancel = { draggedUri = null; dragOffset = Offset.Zero }
                            )
                        }
                ) {
                    AsyncImage(model = uri, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (!isAnyDragging) {
                        IconButton(
                            onClick = { viewModel.removePhoto(idx) },
                            modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
                                .background(Color.Black.copy(0.6f), RoundedCornerShape(10.dp))
                        ) { Text("✕", color = Color.White, fontSize = 10.sp) }
                    }
                }
            }
        }

        // Frame delay slider
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(8.dp)).background(SurfaceDark).padding(6.dp)
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("帧间隔", fontSize = 10.sp, color = TextHint)
                    Text("${state.frameDelay}ms", fontSize = 12.sp, color = Purple500)
                }
                Slider(
                    value = state.frameDelay.toFloat(),
                    onValueChange = { viewModel.setFrameDelay(it.toInt()) },
                    valueRange = AppConfig.FRAME_DELAY_MIN_MS.toFloat()..AppConfig.FRAME_DELAY_MAX_MS.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = Purple500, activeTrackColor = Purple500),
                )
                Text("${totalFrames}帧 / ${"%.1f".format(totalSec)}s / ~${estKB}KB", fontSize = 10.sp, color = TextSecondary)
            }
        }

        // Bottom buttons
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.setStep(0); onNavigateBack() },
                modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Purple500),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple500),
            ) { Text("上一步", fontSize = 13.sp) }
            Button(
                onClick = { viewModel.setStep(2); onNavigateNext() },
                modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple500),
            ) { Text("下一步：添加文字", fontSize = 13.sp) }
        }
    }
}

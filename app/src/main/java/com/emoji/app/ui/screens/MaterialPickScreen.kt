package com.emoji.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.app.domain.AppConfig
import com.emoji.app.domain.EmojiLogger
import com.emoji.app.domain.model.MediaType
import com.emoji.app.ui.theme.BackgroundDark
import com.emoji.app.ui.theme.BorderGray
import com.emoji.app.ui.theme.Purple500
import com.emoji.app.ui.theme.TextHint
import com.emoji.app.ui.theme.TextPrimary
import com.emoji.app.viewmodel.SharedViewModel

@Composable
fun MaterialPickScreen(viewModel: SharedViewModel, onNavigateNext: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
                val durSec = durMs / 1000.0
                retriever.release()
                if (durSec > AppConfig.VIDEO_MAX_DURATION_SEC) {
                    EmojiLogger.warn("视频转GIF", "选取视频", "视频过长", "duration" to "%.1f".format(durSec), "result" to "reject")
                } else {
                    viewModel.setMediaType(MediaType.VIDEO)
                    viewModel.setVideo(uri, durSec)
                    viewModel.setStep(1); onNavigateNext()
                }
            } catch (_: Exception) {}
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = AppConfig.PHOTO_MAX_COUNT)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val clamped = if (uris.size > AppConfig.PHOTO_MAX_COUNT) uris.take(AppConfig.PHOTO_MAX_COUNT) else uris
            viewModel.setMediaType(MediaType.PHOTO)
            viewModel.setPhotos(clamped)
            viewModel.setStep(1); onNavigateNext()
        }
    }

    val isVideo = state.mediaType == MediaType.VIDEO
    val hasMedia = (isVideo && state.videoUri != null) || (!isVideo && state.photos.isNotEmpty())

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Central illustration — NOT clipped
        Box(
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isVideo) "🎬" else "🖼", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (hasMedia) (if (isVideo) "视频已选取" else "已选 ${state.photos.size} 张") else "请先选择素材",
                    color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                // Two upload buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UploadCard(
                        emoji = "🎬", title = "选取视频", sub = "MP4 / MOV", modifier = Modifier.weight(1f),
                        onClick = { videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
                    )
                    UploadCard(
                        emoji = "🖼", title = "图片转GIF", sub = "1~20 张", modifier = Modifier.weight(1f),
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    )
                }
            }
        }

        // Bottom button
        Button(
            onClick = { if (hasMedia) { viewModel.setStep(1); onNavigateNext() } },
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            enabled = hasMedia,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple500, disabledContainerColor = BorderGray),
        ) {
            Text(
                when {
                    isVideo && hasMedia -> "下一步：裁剪时间"
                    !isVideo && hasMedia && state.photos.size == 1 -> "下一步：添加文字"
                    !isVideo && hasMedia -> "下一步：排序编辑"
                    else -> "请先选择素材"
                },
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun UploadCard(emoji: String, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, BorderGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 32.sp); Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 14.sp, color = TextPrimary)
            Text(sub, fontSize = 11.sp, color = TextHint)
        }
    }
}

package com.emoji.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Demo-color-matched dark palette
val Purple500 = Color(0xFF7C4DFF)
val Purple700 = Color(0xFF6A3DE8)
val BackgroundDark = Color(0xFF1A1A2E)
val SurfaceDark = Color(0xFF2A2A3E)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFFAAAAAA)
val TextHint = Color(0xFF888888)
val ErrorRed = Color(0xFFF44336)
val SuccessGreen = Color(0xFF4CAF50)
val InfoBlue = Color(0xFF448AFF)
val BorderGray = Color(0xFF444444)

private val EmojiDarkColors = darkColorScheme(
    primary = Purple500,
    onPrimary = Color.White,
    primaryContainer = Purple700,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderGray,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun HappyEmojiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmojiDarkColors,
        content = content
    )
}

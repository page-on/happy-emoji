package com.emoji.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.app.ui.theme.BorderGray
import com.emoji.app.ui.theme.Purple500
import com.emoji.app.ui.theme.SurfaceDark
import com.emoji.app.ui.theme.TextSecondary

data class FontOption(val label: String, val cssFont: String, val weight: String = "700")

val FONT_OPTIONS = listOf(
    FontOption("黑体", "黑体", "700"),
    FontOption("楷体", "楷体", "700"),
    FontOption("仿宋", "仿宋", "700"),
    FontOption("默认", "默认", "700"),
    FontOption("圆体", "圆体", "700"),
    FontOption("手写涂鸦", "手写涂鸦", "700"),
    FontOption("萌萌圆体", "萌萌圆体", "400"),
    FontOption("粗黑搞怪", "粗黑搞怪", "900"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FontSelector(
    currentFontName: String,
    onFontSelected: (FontOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FONT_OPTIONS.forEach { font ->
            val sel = currentFontName == font.label
            val isBold = NativeTypeface.isBold(font.label)
            OutlinedButton(
                onClick = { onFontSelected(font) },
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(1.dp, if (sel) Purple500 else BorderGray),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (sel) Purple500 else SurfaceDark,
                    contentColor = if (sel) Color.White else TextSecondary,
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    font.label,
                    fontSize = if (sel) 13.sp else 12.sp,
                    fontFamily = fontFamilyFor(font.label),
                    fontWeight = if (sel) FontWeight.Bold else if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = fontStyleFor(font.label),
                )
            }
        }
    }
}

/**
 * Best-effort Compose FontFamily for each font label.
 * Since Compose only has 4 built-in families, variety is limited here.
 * The actual text overlay on the preview uses NativeTypeface (Android Canvas) for full variety.
 */
private fun fontFamilyFor(label: String): FontFamily = when (label) {
    "楷体", "仿宋" -> FontFamily.Serif
    "手写涂鸦" -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}
private fun fontStyleFor(label: String): FontStyle = when (label) {
    "手写涂鸦" -> FontStyle.Italic
    else -> FontStyle.Normal
}

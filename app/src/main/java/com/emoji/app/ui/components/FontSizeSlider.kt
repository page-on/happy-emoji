package com.emoji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.app.domain.AppConfig
import com.emoji.app.ui.theme.Purple500
import com.emoji.app.ui.theme.TextHint
import com.emoji.app.ui.theme.TextSecondary

@Composable
fun FontSizeSlider(
    currentSizePx: Int,
    onSizeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minPx: Int = AppConfig.FONT_SIZE_MIN_PX,
    maxPx: Int = AppConfig.FONT_SIZE_MAX_PX,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("字号", fontSize = 11.sp, color = TextSecondary)
            Text("${currentSizePx}px", fontSize = 12.sp, color = Purple500)
        }
        Slider(
            value = currentSizePx.toFloat(),
            onValueChange = { onSizeChanged(it.toInt()) },
            valueRange = minPx.toFloat()..maxPx.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Purple500,
                activeTrackColor = Purple500,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text("${minPx}px", fontSize = 11.sp, color = TextHint)
            Text("${maxPx}px", fontSize = 11.sp, color = TextHint)
        }
    }
}

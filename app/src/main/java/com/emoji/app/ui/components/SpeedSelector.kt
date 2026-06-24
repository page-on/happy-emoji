package com.emoji.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.app.domain.AppConfig
import com.emoji.app.ui.theme.BorderGray
import com.emoji.app.ui.theme.Purple500
import com.emoji.app.ui.theme.SurfaceDark
import com.emoji.app.ui.theme.TextSecondary

fun speedLabel(speed: Float): String = when (speed) {
    0.25f -> "1/4x"; 0.5f -> "1/2x"; 1.0f -> "原速"; 1.5f -> "1.5x"; 2.0f -> "2x"
    else -> "${speed}x"
}

@Composable
fun SpeedSelector(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
    options: List<Float> = AppConfig.SPEED_OPTIONS,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { speed ->
            val sel = currentSpeed == speed
            OutlinedButton(
                onClick = { onSpeedSelected(speed) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (sel) Purple500 else BorderGray),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (sel) Purple500 else SurfaceDark, contentColor = if (sel) Color.White else TextSecondary),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            ) { Text(speedLabel(speed), fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
        }
    }
}

package com.emoji.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

@Composable
fun QualitySelector(
    currentQuality: AppConfig.Quality,
    onQualitySelected: (AppConfig.Quality) -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppConfig.Quality.values().forEach { q ->
            val selected = currentQuality == q
            OutlinedButton(
                onClick = { onQualitySelected(q) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(
                    1.dp,
                    if (selected) Purple500 else BorderGray
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) Purple500 else SurfaceDark,
                    contentColor = if (selected) Color.White else TextSecondary,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp, vertical = 4.dp,
                ),
            ) {
                Text(
                    text = "${q.label}\n${q.width}px",
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

package com.emoji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.app.ui.theme.Purple500
import com.emoji.app.ui.theme.SuccessGreen
import com.emoji.app.ui.theme.SurfaceDark
import com.emoji.app.ui.theme.TextHint

@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until totalSteps) {
            val bg = when { i < currentStep -> SuccessGreen; i == currentStep -> Purple500; else -> SurfaceDark }
            val fg = if (i <= currentStep) Color.White else TextHint
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                Text("${i + 1}", color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (i < totalSteps - 1) {
                Box(
                    modifier = Modifier.padding(horizontal = 2.dp).size(width = 28.dp, height = 2.dp)
                        .clip(CircleShape).background(if (i < currentStep) SuccessGreen else SurfaceDark)
                )
            }
        }
    }
}

fun totalStepsForPhoto(photoCount: Int): Int = if (photoCount == 1) 3 else 4
fun totalStepsForVideo(): Int = 4

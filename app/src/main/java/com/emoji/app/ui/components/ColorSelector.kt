package com.emoji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emoji.app.ui.theme.Purple500

/** Color dot size matching demo: 28dp */
@Composable
fun ColorSelector(
    colors: List<Color>,
    currentColor: Int,   // ARGB int
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dotSize: Dp = 28.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colors.forEach { c ->
            val selected = c.hashCode() == currentColor
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(c)
                    .then(
                        if (selected) Modifier.border(2.dp, Color.White, CircleShape)
                        else Modifier.border(2.dp, Color.Transparent, CircleShape)
                    )
                    .clickable { onColorSelected(c.hashCode()) }
            )
        }
    }
}

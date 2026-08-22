package com.example.ui.screens.security.lock

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimary

/**
 * Animated Dot Indicators showing filled PIN progress with bouncy scaling and error shake offset.
 */
@Composable
fun PasscodeDotIndicators(
    enteredLength: Int,
    shakeOffsetPx: Float,
    modifier: Modifier = Modifier,
    totalDots: Int = 4
) {
    Row(
        modifier = modifier.offset(x = shakeOffsetPx.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalDots) {
            val filled = enteredLength > i
            val dotScale by animateFloatAsState(
                targetValue = if (filled) 1.25f else 1.0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessHigh,
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                label = "dotScale_$i"
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(if (filled) EmeraldPrimary else Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 1.2.dp,
                        color = if (filled) EmeraldPrimary else Color.White.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveformBar(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnimation")
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val duration = (400 + (i % 5) * 150)
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = if (isRecording) 0.95f else 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(if (isRecording) animatedHeight else 0.2f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isRecording) activeColor else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

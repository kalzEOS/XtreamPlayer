package com.example.xtreamplayer.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xtreamplayer.ui.theme.AppTheme

@Composable
fun NextEpisodeOverlay(
    nextEpisodeTitle: String,
    countdownSeconds: Int,
    remainingSeconds: Int,
    controlsVisible: Boolean,
    modifier: Modifier = Modifier
) {
    // Animated vertical offset - move up when controls are visible
    val bottomOffset = animateDpAsState(
        targetValue = if (controlsVisible) 100.dp else 24.dp,
        animationSpec = tween(durationMillis = 200),
        label = "nextEpisodeOffset"
    ).value

    // Calculate progress (1.0 = full, 0.0 = empty)
    val progress = if (countdownSeconds > 0) {
        remainingSeconds.toFloat() / countdownSeconds.toFloat()
    } else {
        0f
    }

    Box(
        modifier = modifier
            .padding(end = 24.dp, bottom = bottomOffset)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = AppTheme.colors.borderStrong,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Circular countdown indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(48.dp),
                    color = AppTheme.colors.accent,
                    trackColor = AppTheme.colors.surface,
                    strokeWidth = 3.dp
                )
                Text(
                    text = "$remainingSeconds",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppTheme.fontFamily
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Up Next",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily
                )
                Text(
                    text = nextEpisodeTitle,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = AppTheme.fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Press Back to dismiss",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = AppTheme.fontFamily
                )
            }
        }
    }
}

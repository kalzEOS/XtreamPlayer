package com.example.xtreamplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.xtreamplayer.ui.theme.AppTheme
import com.example.xtreamplayer.ui.theme.BackgroundGradientStyle

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val colors = AppTheme.colors
    val backgroundBrush =
        when (colors.backgroundGradientStyle) {
            BackgroundGradientStyle.DIAGONAL ->
                Brush.linearGradient(colors = colors.backgroundGradientColors)
            BackgroundGradientStyle.VERTICAL ->
                Brush.verticalGradient(colors = colors.backgroundGradientColors)
        }
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(backgroundBrush)
    ) {
        Box(
            modifier =
                Modifier.fillMaxHeight()
                    .width(260.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    colors.accent.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                        )
                    )
        )
        content()
    }
}

package com.example.xtreamplayer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.xtreamplayer.ui.theme.AppTheme

private val FocusBorderColor: Color
    @Composable get() = AppTheme.colors.focus
private val GlowColor = Color(0x40FFFFFF)

@Composable
fun FocusableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(50)

    // Sweep animation progress (-1 = not visible, 0 = left edge, 1 = right edge, 2 = finished)
    val sweepProgress = remember { Animatable(-1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            sweepProgress.snapTo(-0.3f)
            sweepProgress.animateTo(
                targetValue = 1.3f,
                animationSpec = tween(durationMillis = 250)
            )
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .drawWithContent {
                drawContent()
                val progress = sweepProgress.value
                if (progress in -0.3f..1.3f) {
                    val sweepWidth = size.width * 0.4f
                    val sweepCenter = size.width * progress
                    val gradientBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GlowColor,
                            Color.Transparent
                        ),
                        startX = sweepCenter - sweepWidth / 2,
                        endX = sweepCenter + sweepWidth / 2
                    )
                    drawRect(brush = gradientBrush)
                }
            }
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            shape = shape,
            modifier = Modifier.then(
                if (isFocused) {
                    Modifier.border(2.dp, FocusBorderColor, shape)
                } else {
                    Modifier
                }
            )
        ) {
            content()
        }
    }
}

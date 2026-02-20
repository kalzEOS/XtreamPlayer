package com.example.xtreamplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xtreamplayer.ui.theme.AppTheme

@Composable
internal fun TopBar(title: String, showBack: Boolean, onBack: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showBack) {
            TopBarButton(label = "BACK", onActivate = onBack)
        }
        Text(
            text = title,
            color = AppTheme.colors.textPrimary,
            fontSize = 20.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.weight(1f)
        )
        TopBarButton(label = "SETTINGS", onActivate = onSettings)
    }
}

@Composable
internal fun TopBarButton(
    label: String,
    onActivate: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onMoveLeft: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor =
        when {
            !enabled -> AppTheme.colors.border
            isFocused -> AppTheme.colors.focus
            else -> AppTheme.colors.borderStrong
        }
    val buttonBrush =
        if (!enabled) {
            Brush.horizontalGradient(
                colors = listOf(AppTheme.colors.surfaceAlt, AppTheme.colors.surface)
            )
        } else if (isFocused) {
            Brush.horizontalGradient(colors = listOf(AppTheme.colors.accent, AppTheme.colors.accentAlt))
        } else {
            Brush.horizontalGradient(colors = listOf(AppTheme.colors.accentMutedAlt, AppTheme.colors.surfaceAlt))
        }
    androidx.compose.foundation.layout.Box(
        modifier =
            modifier.height(40.dp)
                .width(140.dp)
                .focusable(interactionSource = interactionSource, enabled = enabled)
                .onKeyEvent {
                    if (!enabled) {
                        return@onKeyEvent false
                    }
                    if (it.type != KeyEventType.KeyDown) {
                        false
                    } else if (it.key == Key.DirectionLeft && onMoveLeft != null) {
                        onMoveLeft()
                        true
                    } else if (it.key == Key.DirectionDown && onMoveDown != null) {
                        onMoveDown()
                        true
                    } else if (it.key == Key.DirectionUp && onMoveUp != null) {
                        onMoveUp()
                        true
                    } else if (it.key == Key.Enter ||
                        it.key == Key.NumPadEnter ||
                        it.key == Key.DirectionCenter
                    ) {
                        onActivate()
                        true
                    } else {
                        false
                    }
                }
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onActivate
                )
                .background(brush = buttonBrush, shape = shape)
                .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color =
                when {
                    !enabled -> AppTheme.colors.textTertiary
                    isFocused -> AppTheme.colors.textOnAccent
                    else -> AppTheme.colors.textPrimary
                },
            fontSize = 14.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}

package com.example.xtreamplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xtreamplayer.ui.theme.AppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun DialogActionButton(
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = AppTheme.colors

    Box(
        modifier =
            modifier
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource)
                .onKeyEvent {
                    if (it.type != KeyEventType.KeyDown) {
                        false
                    } else when (it.key) {
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocused) colors.accent else colors.accentMutedAlt)
                .border(
                    1.dp,
                    if (isFocused) colors.focus else colors.border,
                    RoundedCornerShape(8.dp)
                )
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isFocused) colors.textOnAccent else colors.textPrimary,
            fontSize = 16.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RepeatableFocusableButton(
    onStep: () -> Boolean,
    enabled: Boolean,
    colors: ButtonColors,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    val repeatModifier =
        modifier.onPreviewKeyEvent { event ->
            if (!enabled) return@onPreviewKeyEvent false
            val isOkKey =
                event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter
            if (!isOkKey) return@onPreviewKeyEvent false

            when (event.type) {
                KeyEventType.KeyDown -> {
                    if (repeatJob == null) {
                        onStep()
                        repeatJob =
                            scope.launch {
                                delay(350)
                                while (isActive) {
                                    if (!onStep()) {
                                        break
                                    }
                                    delay(80)
                                }
                            }
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    repeatJob?.cancel()
                    repeatJob = null
                    true
                }
                else -> false
            }
        }

    FocusableButton(
        onClick = { if (enabled) onStep() },
        enabled = enabled,
        colors = colors,
        shape = shape,
        modifier = repeatModifier
    ) {
        content()
    }
}

@Composable
fun DialogCloseButton(
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Close",
    onNavigateUp: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = AppTheme.colors

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource)
                .onKeyEvent {
                    if (it.type != KeyEventType.KeyDown) {
                        false
                    } else when (it.key) {
                        Key.DirectionUp -> {
                            if (onNavigateUp != null) {
                                onNavigateUp()
                                true
                            } else {
                                false
                            }
                        }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                )
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocused) colors.accent else colors.accentMutedAlt)
                .border(
                    1.dp,
                    if (isFocused) colors.focus else colors.border,
                    RoundedCornerShape(8.dp)
                )
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isFocused) colors.textOnAccent else colors.textPrimary,
            fontSize = 16.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}

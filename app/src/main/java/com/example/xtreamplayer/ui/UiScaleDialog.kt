package com.example.xtreamplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.xtreamplayer.ui.theme.AppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun UiScaleDialog(
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val minPercent = 70
    val maxPercent = 130
    var localPercent by remember {
        mutableIntStateOf((currentScale * 100).roundToInt().coerceIn(minPercent, maxPercent))
    }
    val minusFocusRequester = remember { FocusRequester() }
    val plusFocusRequester = remember { FocusRequester() }
    val resetFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    var pendingFocus by remember { mutableStateOf<UiScaleFocusTarget?>(UiScaleFocusTarget.MINUS) }

    LaunchedEffect(currentScale) {
        localPercent = (currentScale * 100).roundToInt().coerceIn(minPercent, maxPercent)
    }

    LaunchedEffect(pendingFocus) {
        when (pendingFocus) {
            UiScaleFocusTarget.MINUS -> minusFocusRequester.requestFocus()
            UiScaleFocusTarget.PLUS -> plusFocusRequester.requestFocus()
            UiScaleFocusTarget.RESET -> resetFocusRequester.requestFocus()
            null -> Unit
        }
        pendingFocus = null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "UI Scale",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Adjust overall interface size.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RepeatableFocusableButton(
                        onStep = {
                            val newPercent = (localPercent - 1).coerceAtLeast(minPercent)
                            if (newPercent == localPercent) return@RepeatableFocusableButton false
                            localPercent = newPercent
                            onScaleChange(newPercent / 100f)
                            pendingFocus = UiScaleFocusTarget.MINUS
                            true
                        },
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentMutedAlt),
                        modifier = Modifier.focusRequester(minusFocusRequester)
                    ) {
                        Text("-", color = colors.textPrimary, fontSize = 18.sp)
                    }

                    Text(
                        text = "${localPercent}%",
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    RepeatableFocusableButton(
                        onStep = {
                            val newPercent = (localPercent + 1).coerceAtMost(maxPercent)
                            if (newPercent == localPercent) return@RepeatableFocusableButton false
                            localPercent = newPercent
                            onScaleChange(newPercent / 100f)
                            pendingFocus = UiScaleFocusTarget.PLUS
                            true
                        },
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentMutedAlt),
                        modifier = Modifier.focusRequester(plusFocusRequester)
                    ) {
                        Text("+", color = colors.textPrimary, fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DialogActionButton(
                        label = "Reset",
                        focusRequester = resetFocusRequester,
                        onClick = {
                            localPercent = 100
                            onScaleChange(1f)
                            pendingFocus = UiScaleFocusTarget.RESET
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DialogActionButton(
                        label = "Close",
                        focusRequester = closeFocusRequester,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private enum class UiScaleFocusTarget {
    MINUS,
    PLUS,
    RESET
}

@Composable
private fun DialogActionButton(
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = AppTheme.colors

    Box(
        modifier = modifier
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
private fun RepeatableFocusableButton(
    onStep: () -> Boolean,
    enabled: Boolean,
    colors: ButtonColors,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
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
                        val didStep = onStep()
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

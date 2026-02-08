package com.example.xtreamplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.xtreamplayer.ui.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun FontScaleDialog(
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val minPercent = 70
    val maxPercent = 140
    var localPercent by remember {
        mutableIntStateOf((currentScale * 100).roundToInt().coerceIn(minPercent, maxPercent))
    }
    val minusFocusRequester = remember { FocusRequester() }
    val plusFocusRequester = remember { FocusRequester() }
    val resetFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    var lastFocusTarget by remember { mutableStateOf(FontScaleFocusTarget.MINUS) }
    var focusRequestNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentScale) {
        localPercent = (currentScale * 100).roundToInt().coerceIn(minPercent, maxPercent)
    }

    suspend fun requestFocusWithFrames(target: FocusRequester) {
        repeat(3) {
            withFrameNanos { }
            runCatching { target.requestFocus() }.onSuccess { return }
        }
    }

    LaunchedEffect(lastFocusTarget, focusRequestNonce, currentScale) {
        val requester = when (lastFocusTarget) {
            FontScaleFocusTarget.MINUS -> minusFocusRequester
            FontScaleFocusTarget.PLUS -> plusFocusRequester
            FontScaleFocusTarget.RESET -> resetFocusRequester
        }
        requestFocusWithFrames(requester)
    }

    AppDialog(
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
                    text = "Font Scale",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Adjust text size only (UI layout unchanged).",
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
                            lastFocusTarget = FontScaleFocusTarget.MINUS
                            focusRequestNonce++
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
                            lastFocusTarget = FontScaleFocusTarget.PLUS
                            focusRequestNonce++
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
                            lastFocusTarget = FontScaleFocusTarget.RESET
                            focusRequestNonce++
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

private enum class FontScaleFocusTarget {
    MINUS,
    PLUS,
    RESET
}

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.xtreamplayer.ui.theme.AppTheme

@Composable
fun NextEpisodeThresholdDialog(
    currentSeconds: Int,
    onSecondsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val maxSeconds = 300  // 5 minutes max
    val minSeconds = 0
    var localSeconds by remember { mutableIntStateOf(currentSeconds.coerceIn(minSeconds, maxSeconds)) }
    val closeFocusRequester = remember { FocusRequester() }
    val minusFocusRequester = remember { FocusRequester() }

    LaunchedEffect(currentSeconds) {
        localSeconds = currentSeconds.coerceIn(minSeconds, maxSeconds)
    }

    LaunchedEffect(Unit) {
        minusFocusRequester.requestFocus()
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
                    text = "Next Episode Prompt",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Show the 'Up Next' overlay this many seconds before the episode ends.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Decrement by 10
                    FocusableButton(
                        onClick = {
                            val newSeconds = (localSeconds - 10).coerceAtLeast(minSeconds)
                            localSeconds = newSeconds
                            onSecondsChange(newSeconds)
                        },
                        enabled = localSeconds > minSeconds,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        modifier = Modifier.focusRequester(minusFocusRequester)
                    ) {
                        Text("-10", color = colors.textPrimary, fontSize = 14.sp)
                    }

                    // Decrement by 1
                    FocusableButton(
                        onClick = {
                            val newSeconds = (localSeconds - 1).coerceAtLeast(minSeconds)
                            localSeconds = newSeconds
                            onSecondsChange(newSeconds)
                        },
                        enabled = localSeconds > minSeconds,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface)
                    ) {
                        Text("-1", color = colors.textPrimary, fontSize = 14.sp)
                    }

                    // Display current value
                    Text(
                        text = "${localSeconds}s",
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // Increment by 1
                    FocusableButton(
                        onClick = {
                            val newSeconds = (localSeconds + 1).coerceAtMost(maxSeconds)
                            localSeconds = newSeconds
                            onSecondsChange(newSeconds)
                        },
                        enabled = localSeconds < maxSeconds,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface)
                    ) {
                        Text("+1", color = colors.textPrimary, fontSize = 14.sp)
                    }

                    // Increment by 10
                    FocusableButton(
                        onClick = {
                            val newSeconds = (localSeconds + 10).coerceAtMost(maxSeconds)
                            localSeconds = newSeconds
                            onSecondsChange(newSeconds)
                        },
                        enabled = localSeconds < maxSeconds,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface)
                    ) {
                        Text("+10", color = colors.textPrimary, fontSize = 14.sp)
                    }
                }

                // Quick presets row
                Text(
                    text = "Quick presets:",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pair of (seconds, label) - this lets you define custom labels
                    listOf(
                        30 to "30s",
                        60 to "1m",
                        90 to "1.5m",
                        120 to "2m"
                    ).forEach { (seconds, label) ->
                        FocusableButton(
                            onClick = {
                                localSeconds = seconds
                                onSecondsChange(seconds)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (localSeconds == seconds) colors.accent else colors.surface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                color = if (localSeconds == seconds) colors.background else colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (localSeconds == seconds) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FocusableButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.borderStrong,
                        contentColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .focusRequester(closeFocusRequester)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

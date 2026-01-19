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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.xtreamplayer.settings.AppThemeOption
import com.example.xtreamplayer.ui.theme.AppTheme

@Composable
fun ThemeSelectionDialog(
    themes: List<AppThemeOption>,
    currentTheme: AppThemeOption,
    onThemeSelected: (AppThemeOption) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val closeFocusRequester = remember { FocusRequester() }
    val itemFocusRequesters =
        remember(themes.size) { List(themes.size) { FocusRequester() } }
    val selectedIndex = themes.indexOf(currentTheme).coerceAtLeast(0)

    LaunchedEffect(themes.size, selectedIndex) {
        if (themes.isNotEmpty()) {
            itemFocusRequesters.getOrNull(selectedIndex)?.requestFocus()
                ?: itemFocusRequesters.firstOrNull()?.requestFocus()
        } else {
            closeFocusRequester.requestFocus()
        }
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
            Column {
                Text(
                    text = "Theme",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (themes.isEmpty()) {
                    Text(
                        text = "No themes available",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        itemsIndexed(themes) { index, theme ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            var keyDownArmed by remember { mutableStateOf(false) }
                            var keyClickHandled by remember { mutableStateOf(false) }
                            val isSelected = theme == currentTheme
                            val borderColor =
                                when {
                                    isFocused && isSelected -> colors.focus
                                    isFocused -> colors.accentAlt
                                    isSelected -> colors.accentSelected
                                    else -> colors.borderStrong
                                }
                            val backgroundColor =
                                when {
                                    isFocused && isSelected -> colors.panelBackground
                                    isFocused -> colors.surfaceAlt
                                    isSelected -> colors.panelBackground
                                    else -> colors.surface
                                }
                            LaunchedEffect(isFocused) {
                                if (!isFocused) {
                                    keyDownArmed = false
                                    keyClickHandled = false
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(backgroundColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .focusRequester(itemFocusRequesters[index])
                                    .focusable(interactionSource = interactionSource)
                                    .onKeyEvent {
                                        val isSelectKey =
                                            it.key == Key.Enter ||
                                                it.key == Key.NumPadEnter ||
                                                it.key == Key.DirectionCenter
                                        when (it.type) {
                                            KeyEventType.KeyDown -> {
                                                if (isSelectKey) {
                                                    keyDownArmed = true
                                                    true
                                                } else {
                                                    false
                                                }
                                            }
                                            KeyEventType.KeyUp -> {
                                                if (isSelectKey && keyDownArmed) {
                                                    keyDownArmed = false
                                                    keyClickHandled = true
                                                    onThemeSelected(theme)
                                                    onDismiss()
                                                    true
                                                } else {
                                                    false
                                                }
                                            }
                                            else -> false
                                        }
                                    }
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        if (keyClickHandled) {
                                            keyClickHandled = false
                                        } else {
                                            onThemeSelected(theme)
                                            onDismiss()
                                        }
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(colors.success, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Text(
                                    text = theme.label,
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        text = "Close",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

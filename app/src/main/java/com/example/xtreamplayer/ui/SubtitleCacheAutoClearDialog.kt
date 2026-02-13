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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.xtreamplayer.settings.SubtitleCacheAutoClearOption
import com.example.xtreamplayer.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SubtitleCacheAutoClearDialog(
    currentIntervalMs: Long,
    onIntervalChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val options = remember { SubtitleCacheAutoClearOption.entries.toList() }
    val coroutineScope = rememberCoroutineScope()
    val closeFocusRequester = remember { FocusRequester() }
    val itemFocusRequesters = remember(options.size) { List(options.size) { FocusRequester() } }
    val selectedIndex =
        options.indexOfFirst { it.intervalMs == currentIntervalMs }
            .takeIf { it >= 0 }
            ?: 0
    val listState = rememberLazyListState()

    LaunchedEffect(options.size, selectedIndex, listState) {
        if (options.isNotEmpty()) {
            val targetIndex = selectedIndex.coerceIn(0, options.lastIndex)
            listState.scrollToItem(targetIndex)
            delay(16)
            itemFocusRequesters.getOrNull(targetIndex)?.requestFocus()
                ?: itemFocusRequesters.firstOrNull()?.requestFocus()
        } else {
            closeFocusRequester.requestFocus()
        }
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
            Column {
                Text(
                    text = "Auto Clear Subtitles Cache",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (options.isEmpty()) {
                    Text(
                        text = "No options available",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        itemsIndexed(options) { index, option ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            var keyDownArmed by remember { mutableStateOf(false) }
                            var keyClickHandled by remember { mutableStateOf(false) }
                            val isSelected = option.intervalMs == currentIntervalMs
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
                                                    onIntervalChange(option.intervalMs)
                                                    onDismiss()
                                                    coroutineScope.launch {
                                                        delay(120)
                                                        keyClickHandled = false
                                                    }
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
                                            onIntervalChange(option.intervalMs)
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
                                    text = option.label,
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DialogCloseButton(
                    focusRequester = closeFocusRequester,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

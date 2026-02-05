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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.xtreamplayer.ui.theme.AppFont
import com.example.xtreamplayer.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FontSelectionDialog(
    fonts: List<AppFont>,
    currentFont: AppFont,
    onFontSelected: (AppFont) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val coroutineScope = rememberCoroutineScope()
    val closeFocusRequester = remember { FocusRequester() }
    val itemFocusRequesters =
        remember(fonts.size) { List(fonts.size) { FocusRequester() } }
    val selectedIndex = fonts.indexOf(currentFont).coerceAtLeast(0)

    LaunchedEffect(fonts.size, selectedIndex) {
        if (fonts.isNotEmpty()) {
            itemFocusRequesters.getOrNull(selectedIndex)?.requestFocus()
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
                    text = "Font",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = currentFont.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (fonts.isEmpty()) {
                    Text(
                        text = "No fonts available",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = currentFont.fontFamily
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(fonts) { index, font ->
                            FontOption(
                                font = font,
                                isSelected = font == currentFont,
                                focusRequester = itemFocusRequesters[index],
                                onSelect = {
                                    onFontSelected(font)
                                    coroutineScope.launch {
                                        delay(100)
                                        onDismiss()
                                    }
                                },
                                onNavigateUp = {
                                    if (index > 0) {
                                        itemFocusRequesters[index - 1].requestFocus()
                                    }
                                },
                                onNavigateDown = {
                                    if (index < fonts.lastIndex) {
                                        itemFocusRequesters[index + 1].requestFocus()
                                    } else {
                                        closeFocusRequester.requestFocus()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CloseButton(
                    focusRequester = closeFocusRequester,
                    onDismiss = onDismiss,
                    onNavigateUp = {
                        if (fonts.isNotEmpty()) {
                            itemFocusRequesters.last().requestFocus()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FontOption(
    font: AppFont,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onSelect: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = AppTheme.colors

    val backgroundColor = when {
        isFocused -> colors.accent
        isSelected -> colors.accentMutedAlt
        else -> colors.surface
    }

    val borderColor = when {
        isFocused -> colors.focus
        isSelected -> colors.accent
        else -> colors.border
    }

    val textColor = if (isFocused) colors.textOnAccent else colors.textPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    false
                } else when (it.key) {
                    Key.DirectionUp -> {
                        onNavigateUp()
                        true
                    }
                    Key.DirectionDown -> {
                        onNavigateDown()
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        onSelect()
                        true
                    }
                    else -> false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            )
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = font.label,
            color = textColor,
            fontSize = 16.sp,
            fontFamily = font.fontFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun CloseButton(
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = AppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    false
                } else when (it.key) {
                    Key.DirectionUp -> {
                        onNavigateUp()
                        true
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
            text = "Close",
            color = if (isFocused) colors.textOnAccent else colors.textPrimary,
            fontSize = 16.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}

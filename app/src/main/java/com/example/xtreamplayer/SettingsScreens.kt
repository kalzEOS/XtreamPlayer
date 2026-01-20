package com.example.xtreamplayer

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.settings.SettingsState
import com.example.xtreamplayer.ui.theme.AppTheme

@Composable
fun SettingsScreen(
    settings: SettingsState,
    activeListName: String,
    contentItemFocusRequester: FocusRequester,
    onMoveLeft: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onOpenNextEpisodeThreshold: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onOpenThemeSelector: () -> Unit,
    onToggleRememberLogin: () -> Unit,
    onToggleAutoSignIn: () -> Unit,
    onOpenSubtitlesApiKey: () -> Unit,
    onManageLists: () -> Unit,
    onRefreshContent: () -> Unit,
    onSignOut: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val scrollState = rememberScrollState()
    val colors = AppTheme.colors
    val apiKeyLabel = if (settings.openSubtitlesApiKey.isNotBlank()) "Configured" else "Not set"
    val thresholdLabel = "${settings.nextEpisodeThresholdSeconds}s"
    val actions = listOf(
        SettingsAction("Auto-play next", flagLabel(settings.autoPlayNext), onToggleAutoPlay),
        SettingsAction("Next episode prompt", thresholdLabel, onOpenNextEpisodeThreshold),
        SettingsAction("Subtitles", flagLabel(settings.subtitlesEnabled), onToggleSubtitles),
        SettingsAction("Theme", settings.appTheme.label, onOpenThemeSelector),
        SettingsAction("OpenSubtitles API key", apiKeyLabel, onOpenSubtitlesApiKey),
        SettingsAction("Remember login", flagLabel(settings.rememberLogin), onToggleRememberLogin),
        SettingsAction("Auto sign-in", flagLabel(settings.autoSignIn), onToggleAutoSignIn),
        SettingsAction("Manage lists", null, onManageLists),
        SettingsAction("Refresh content", null, onRefreshContent),
        SettingsAction("Sign out", null, onSignOut)
    )

    // Focus is managed by user navigation - no auto-focus on screen load

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 12.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.background,
                            colors.backgroundAlt
                        )
                    )
                )
                .border(1.dp, colors.border, shape)
                .padding(20.dp)
        ) {
            Text(
                text = "SETTINGS",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsInfoRow(label = "Active list", value = activeListName)
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                actions.forEachIndexed { index, action ->
                    SettingsActionRow(
                        label = action.label,
                        value = action.value,
                        focusRequester = if (index == 0) contentItemFocusRequester else null,
                        onMoveLeft = onMoveLeft,
                        onActivate = action.onActivate
                    )
                }
            }
        }
    }
}

@Composable
fun ManageListsScreen(
    savedConfig: AuthConfig?,
    activeConfig: AuthConfig?,
    contentItemFocusRequester: FocusRequester,
    onMoveLeft: () -> Unit,
    onBack: () -> Unit,
    onEditList: () -> Unit,
    onSignOut: () -> Unit,
    onForgetList: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val scrollState = rememberScrollState()
    val colors = AppTheme.colors
    val actions = buildList {
        add(SettingsAction("Edit list details", null, onEditList))
        add(SettingsAction("Sign out", null, onSignOut))
        if (savedConfig != null) {
            add(SettingsAction("Forget saved list", null, onForgetList))
        }
        add(SettingsAction("Back", null, onBack))
    }

    // Focus is managed by user navigation - no auto-focus on screen load

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 12.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.background,
                            colors.backgroundAlt
                        )
                    )
                )
                .border(1.dp, colors.border, shape)
                .padding(20.dp)
        ) {
            Text(
                text = "MANAGE LISTS",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsInfoRow(
                label = "Active list",
                value = activeConfig?.listName ?: "Not set"
            )
            SettingsInfoRow(
                label = "Saved list",
                value = savedConfig?.listName ?: "Not set"
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                actions.forEachIndexed { index, action ->
                    SettingsActionRow(
                        label = action.label,
                        value = action.value,
                        focusRequester = if (index == 0) contentItemFocusRequester else null,
                        onMoveLeft = onMoveLeft,
                        onActivate = action.onActivate
                    )
                }
            }
        }
    }
}

private data class SettingsAction(
    val label: String,
    val value: String?,
    val onActivate: () -> Unit
)

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif
        )
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    value: String?,
    focusRequester: FocusRequester?,
    onMoveLeft: () -> Unit,
    onActivate: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val colors = AppTheme.colors

    LaunchedEffect(isFocused) {
        if (isFocused) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    val backgroundBrush = if (isFocused) {
        Brush.horizontalGradient(
            colors = listOf(
                colors.accent,
                colors.accentAlt
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                colors.accentMutedAlt,
                colors.surfaceAlt
            )
        )
    }
    val borderColor = if (isFocused) colors.focus else colors.border
    val textColor = if (isFocused) colors.textOnAccent else colors.textPrimary
    val valueColor = if (isFocused) colors.textOnAccent else colors.textSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    false
                } else if (it.key == Key.DirectionLeft) {
                    onMoveLeft()
                    true
                } else if (
                    it.key == Key.Enter ||
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
                interactionSource = interactionSource,
                indication = null,
                onClick = onActivate
            )
            .background(backgroundBrush, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold
            )
            if (value != null) {
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
        }
    }
}

private fun flagLabel(enabled: Boolean): String {
    return if (enabled) "ON" else "OFF"
}

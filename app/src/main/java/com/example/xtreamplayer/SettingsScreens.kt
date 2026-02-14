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
import androidx.compose.foundation.layout.heightIn
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
import com.example.xtreamplayer.settings.subtitleAutoClearLabel
import com.example.xtreamplayer.ui.theme.AppFont
import com.example.xtreamplayer.ui.theme.AppTheme
import com.example.xtreamplayer.settings.uiScaleDisplayPercent
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: SettingsState,
    activeListName: String,
    appVersionLabel: String,
    contentItemFocusRequester: FocusRequester,
    focusAppearanceOnReturn: Boolean,
    focusManageListsOnReturn: Boolean,
    onMoveLeft: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onOpenNextEpisodeThreshold: () -> Unit,
    onOpenSubtitleCacheAutoClear: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onOpenAppearance: () -> Unit,
    onToggleRememberLogin: () -> Unit,
    onToggleAutoSignIn: () -> Unit,
    onOpenSubtitlesApiKey: () -> Unit,
    onManageLists: () -> Unit,
    onRefreshContent: () -> Unit,
    onToggleCheckUpdatesOnStartup: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onClearCache: () -> Unit,
    onSignOut: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val scrollState = rememberScrollState()
    val colors = AppTheme.colors
    val apiKeyLabel = if (settings.openSubtitlesApiKey.isNotBlank()) "Configured" else "Not set"
    val thresholdLabel = "${settings.nextEpisodeThresholdSeconds}s"
    val subtitleCacheAutoClear = subtitleAutoClearLabel(settings.subtitleCacheAutoClearIntervalMs)
    val playbackActions = listOf(
        SettingsAction("Auto-play next", flagLabel(settings.autoPlayNext), onToggleAutoPlay),
        SettingsAction("Next episode prompt", thresholdLabel, onOpenNextEpisodeThreshold),
        SettingsAction("Subtitles", flagLabel(settings.subtitlesEnabled), onToggleSubtitles),
        SettingsAction("Auto clear subtitle cache", subtitleCacheAutoClear, onOpenSubtitleCacheAutoClear),
        SettingsAction("OpenSubtitles API key", apiKeyLabel, onOpenSubtitlesApiKey)
    )
    val appearanceActions = listOf(
        SettingsAction("Appearance", null, onOpenAppearance)
    )
    val accountActions = listOf(
        SettingsAction("Remember login", flagLabel(settings.rememberLogin), onToggleRememberLogin),
        SettingsAction("Auto sign-in", flagLabel(settings.autoSignIn), onToggleAutoSignIn),
        SettingsAction("Manage lists", null, onManageLists)
    )
    val libraryActions = listOf(
        SettingsAction("Sync library", null, onRefreshContent)
    )
    val aboutActions = listOf(
        SettingsAction(
            "Check for updates on startup",
            flagLabel(settings.checkUpdatesOnStartup),
            onToggleCheckUpdatesOnStartup
        ),
        SettingsAction("Check for updates", appVersionLabel, onCheckForUpdates)
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
                fontFamily = settings.appFont.fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsInfoRow(label = "Active list", value = activeListName, fontFamily = settings.appFont.fontFamily)
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var actionIndex = 0
                val focusTargetLabel =
                    when {
                        focusManageListsOnReturn -> "Manage lists"
                        focusAppearanceOnReturn -> "Appearance"
                        else -> null
                    }
                @Composable
                fun renderAction(action: SettingsAction) {
                    val shouldFocusThisAction =
                        if (focusTargetLabel != null) {
                            action.label == focusTargetLabel
                        } else {
                            actionIndex == 0
                        }
                    SettingsActionRow(
                        label = action.label,
                        value = action.value,
                        focusRequester = if (shouldFocusThisAction) contentItemFocusRequester else null,
                        onMoveLeft = onMoveLeft,
                        onActivate = action.onActivate,
                        fontFamily = settings.appFont.fontFamily
                    )
                    actionIndex += 1
                }

                SettingsSectionHeader("Playback")
                playbackActions.forEach { action -> renderAction(action) }

                SettingsSectionHeader("Appearance")
                appearanceActions.forEach { action -> renderAction(action) }

                SettingsSectionHeader("Library")
                libraryActions.forEach { action -> renderAction(action) }
                SettingsActionRow(
                    label = "Clear cache",
                    value = null,
                    focusRequester = null,
                    onMoveLeft = onMoveLeft,
                    onActivate = onClearCache,
                    fontFamily = settings.appFont.fontFamily,
                    secondaryText = "If playback or syncing acts up, clear cached data here, then run Sync library above."
                )

                SettingsSectionHeader("Account")
                accountActions.forEach { action -> renderAction(action) }
                SettingsActionRow(
                    label = "Sign out",
                    value = null,
                    focusRequester = null,
                    onMoveLeft = onMoveLeft,
                    onActivate = onSignOut,
                    fontFamily = settings.appFont.fontFamily
                )

                SettingsSectionHeader("App")
                aboutActions.forEach { action -> renderAction(action) }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = AppTheme.colors.textTertiary,
        fontSize = 11.sp,
        fontFamily = AppTheme.fontFamily,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
fun ManageListsScreen(
    savedConfig: AuthConfig?,
    activeConfig: AuthConfig?,
    settings: SettingsState,
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
                fontFamily = settings.appFont.fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsInfoRow(
                label = "Active list",
                value = activeConfig?.listName ?: "Not set",
                fontFamily = settings.appFont.fontFamily
            )
            SettingsInfoRow(
                label = "Saved list",
                value = savedConfig?.listName ?: "Not set",
                fontFamily = settings.appFont.fontFamily
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
                        onActivate = action.onActivate,
                        fontFamily = settings.appFont.fontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun AppearanceScreen(
    settings: SettingsState,
    contentItemFocusRequester: FocusRequester,
    onMoveLeft: () -> Unit,
    onBack: () -> Unit,
    onOpenThemeSelector: () -> Unit,
    onOpenFontSelector: () -> Unit,
    onOpenUiScale: () -> Unit,
    onOpenFontScale: () -> Unit,
    onToggleClockFormat: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val scrollState = rememberScrollState()
    val colors = AppTheme.colors
    val uiScaleLabel = "${uiScaleDisplayPercent(settings.uiScale)}%"
    val fontScaleLabel = "${(settings.fontScale * 100).roundToInt()}%"
    val actions = listOf(
        SettingsAction("Theme", settings.appTheme.label, onOpenThemeSelector),
        SettingsAction("Font", settings.appFont.label, onOpenFontSelector),
        SettingsAction("UI Scale", uiScaleLabel, onOpenUiScale),
        SettingsAction("Font Scale", fontScaleLabel, onOpenFontScale),
        SettingsAction("Clock format", settings.clockFormat.label, onToggleClockFormat),
        SettingsAction("Back", null, onBack)
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
                text = "APPEARANCE",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontFamily = settings.appFont.fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
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
                        onActivate = action.onActivate,
                        fontFamily = settings.appFont.fontFamily
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
private fun SettingsInfoRow(label: String, value: String, fontFamily: FontFamily = FontFamily.Serif) {
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
            fontFamily = fontFamily
        )
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontFamily = fontFamily,
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
    onActivate: () -> Unit,
    fontFamily: FontFamily = FontFamily.Serif,
    secondaryText: String? = null
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
    val secondaryColor =
        if (isFocused) colors.textOnAccent.copy(alpha = 0.85f) else colors.textSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (secondaryText != null) 72.dp else 56.dp)
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
            .padding(horizontal = 16.dp, vertical = if (secondaryText != null) 8.dp else 0.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 16.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.SemiBold
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        color = secondaryColor,
                        fontSize = 12.sp,
                        fontFamily = fontFamily
                    )
                }
            }
            if (value != null) {
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 13.sp,
                    fontFamily = fontFamily,
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

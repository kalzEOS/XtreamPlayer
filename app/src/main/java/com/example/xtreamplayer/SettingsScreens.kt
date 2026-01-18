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
import androidx.compose.ui.graphics.Color
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

@Composable
fun SettingsScreen(
    settings: SettingsState,
    activeListName: String,
    contentItemFocusRequester: FocusRequester,
    onMoveLeft: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleMatchFrameRate: () -> Unit,
    onCyclePlaybackQuality: () -> Unit,
    onCycleAudioLanguage: () -> Unit,
    onToggleRememberLogin: () -> Unit,
    onToggleAutoSignIn: () -> Unit,
    onToggleParentalPin: () -> Unit,
    onCycleParentalRating: () -> Unit,
    onOpenSubtitlesApiKey: () -> Unit,
    onManageLists: () -> Unit,
    onRefreshContent: () -> Unit,
    onSignOut: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val scrollState = rememberScrollState()
    val apiKeyLabel = if (settings.openSubtitlesApiKey.isNotBlank()) "Configured" else "Not set"
    val actions = listOf(
        SettingsAction("Auto-play next", flagLabel(settings.autoPlayNext), onToggleAutoPlay),
        SettingsAction("Subtitles", flagLabel(settings.subtitlesEnabled), onToggleSubtitles),
        SettingsAction("Match frame rate", flagLabel(settings.matchFrameRate), onToggleMatchFrameRate),
        SettingsAction("Playback quality", settings.playbackQuality.label, onCyclePlaybackQuality),
        SettingsAction("Audio language", settings.audioLanguage.label, onCycleAudioLanguage),
        SettingsAction("OpenSubtitles API key", apiKeyLabel, onOpenSubtitlesApiKey),
        SettingsAction("Remember login", flagLabel(settings.rememberLogin), onToggleRememberLogin),
        SettingsAction("Auto sign-in", flagLabel(settings.autoSignIn), onToggleAutoSignIn),
        SettingsAction("Parental PIN", flagLabel(settings.parentalPinEnabled), onToggleParentalPin),
        SettingsAction("Parental rating", settings.parentalRating.label, onCycleParentalRating),
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
                            Color(0xFF0F1626),
                            Color(0xFF141C2E)
                        )
                    )
                )
                .border(1.dp, Color(0xFF1E2738), shape)
                .padding(20.dp)
        ) {
            Text(
                text = "SETTINGS",
                color = Color.White,
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
                            Color(0xFF0F1626),
                            Color(0xFF141C2E)
                        )
                    )
                )
                .border(1.dp, Color(0xFF1E2738), shape)
                .padding(20.dp)
        ) {
            Text(
                text = "MANAGE LISTS",
                color = Color.White,
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif
        )
        Text(
            text = value,
            color = Color(0xFFE6ECF7),
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

    LaunchedEffect(isFocused) {
        if (isFocused) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    val backgroundBrush = if (isFocused) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF4F8CFF),
                Color(0xFF7FCBFF)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2B3240),
                Color(0xFF222833)
            )
        )
    }
    val borderColor = if (isFocused) Color(0xFFB6D9FF) else Color(0xFF1E2533)
    val textColor = if (isFocused) Color(0xFF0C1730) else Color(0xFFE6ECF7)
    val valueColor = if (isFocused) Color(0xFF0C1730) else Color(0xFF94A3B8)

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

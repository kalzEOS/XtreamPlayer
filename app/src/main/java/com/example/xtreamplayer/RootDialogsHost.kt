package com.example.xtreamplayer

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.example.xtreamplayer.settings.SettingsState
import com.example.xtreamplayer.settings.SettingsViewModel
import com.example.xtreamplayer.settings.SubtitleAppearanceSettings
import com.example.xtreamplayer.ui.ApiKeyInputDialog
import com.example.xtreamplayer.ui.FontScaleDialog
import com.example.xtreamplayer.ui.FontSelectionDialog
import com.example.xtreamplayer.ui.SubtitleAppearanceDialog
import com.example.xtreamplayer.ui.SubtitleCacheAutoClearDialog
import com.example.xtreamplayer.ui.ThemeSelectionDialog
import com.example.xtreamplayer.ui.UiScaleDialog
import com.example.xtreamplayer.ui.VodBufferDialog
import com.example.xtreamplayer.ui.theme.AppFont

@Composable
internal fun RootDialogsHost(
    context: Context,
    settings: SettingsState,
    settingsViewModel: SettingsViewModel,
    appRecoveryManager: AppRecoveryManager,
    showThemeDialogState: MutableState<Boolean>,
    showFontDialogState: MutableState<Boolean>,
    showUiScaleDialogState: MutableState<Boolean>,
    showFontScaleDialogState: MutableState<Boolean>,
    showNextEpisodeThresholdDialogState: MutableState<Boolean>,
    showVodBufferDialogState: MutableState<Boolean>,
    showSubtitleAppearanceDialogState: MutableState<Boolean>,
    subtitleAppearancePreviewState: MutableState<SubtitleAppearanceSettings?>,
    showSubtitleCacheAutoClearDialogState: MutableState<Boolean>,
    showApiKeyDialogState: MutableState<Boolean>,
    showPlaybackRecoveryDialogState: MutableState<Boolean>
) {
    var showThemeDialog by showThemeDialogState
    var showFontDialog by showFontDialogState
    var showUiScaleDialog by showUiScaleDialogState
    var showFontScaleDialog by showFontScaleDialogState
    var showNextEpisodeThresholdDialog by showNextEpisodeThresholdDialogState
    var showVodBufferDialog by showVodBufferDialogState
    var showSubtitleAppearanceDialog by showSubtitleAppearanceDialogState
    var subtitleAppearancePreview by subtitleAppearancePreviewState
    var showSubtitleCacheAutoClearDialog by showSubtitleCacheAutoClearDialogState
    var showApiKeyDialog by showApiKeyDialogState
    var showPlaybackRecoveryDialog by showPlaybackRecoveryDialogState

    if (showThemeDialog) {
        ThemeSelectionDialog(
            themes = com.example.xtreamplayer.settings.AppThemeOption.entries.toList(),
            currentTheme = settings.appTheme,
            onThemeSelected = { settingsViewModel.setAppTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showFontDialog) {
        FontSelectionDialog(
            fonts = AppFont.entries.toList(),
            currentFont = settings.appFont,
            onFontSelected = { settingsViewModel.setAppFont(it) },
            onDismiss = { showFontDialog = false }
        )
    }

    if (showNextEpisodeThresholdDialog) {
        com.example.xtreamplayer.ui.NextEpisodeThresholdDialog(
            currentSeconds = settings.nextEpisodeThresholdSeconds,
            onSecondsChange = { settingsViewModel.setNextEpisodeThreshold(it) },
            onDismiss = { showNextEpisodeThresholdDialog = false }
        )
    }

    if (showVodBufferDialog) {
        VodBufferDialog(
            currentSeconds = settings.vodBufferSeconds,
            onSecondsChange = { settingsViewModel.setVodBufferSeconds(it) },
            onDismiss = { showVodBufferDialog = false }
        )
    }

    if (showSubtitleCacheAutoClearDialog) {
        SubtitleCacheAutoClearDialog(
            currentIntervalMs = settings.subtitleCacheAutoClearIntervalMs,
            onIntervalChange = { settingsViewModel.setSubtitleCacheAutoClearInterval(it) },
            onDismiss = { showSubtitleCacheAutoClearDialog = false }
        )
    }

    if (showSubtitleAppearanceDialog) {
        SubtitleAppearanceDialog(
            initialSettings = settings.subtitleAppearance,
            onPreview = { updated ->
                subtitleAppearancePreview = updated
            },
            onApply = { updated ->
                subtitleAppearancePreview = null
                settingsViewModel.setSubtitleAppearance(updated)
                settingsViewModel.flushPendingSubtitleAppearance()
            },
            onDismiss = {
                subtitleAppearancePreview = null
                showSubtitleAppearanceDialog = false
            }
        )
    }

    if (showApiKeyDialog) {
        ApiKeyInputDialog(
            currentKey = settings.openSubtitlesApiKey,
            currentUserAgent = settings.openSubtitlesUserAgent,
            onSave = { apiKey, userAgent ->
                settingsViewModel.setOpenSubtitlesApiKey(apiKey)
                settingsViewModel.setOpenSubtitlesUserAgent(userAgent)
                Toast.makeText(
                    context,
                    "OpenSubtitles settings saved",
                    Toast.LENGTH_SHORT
                ).show()
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }

    if (showPlaybackRecoveryDialog) {
        PlaybackRecoveryDialog(
            onCancel = { showPlaybackRecoveryDialog = false },
            onRestartApp = {
                showPlaybackRecoveryDialog = false
                appRecoveryManager.restartApp(
                    "user_requested_restart_after_stale_playback_dialog"
                )
            },
            onOpenAppSettings = {
                showPlaybackRecoveryDialog = false
                openAppSettings(context)
            }
        )
    }

    if (showUiScaleDialog) {
        UiScaleDialog(
            currentScale = settings.uiScale,
            onScaleChange = { settingsViewModel.setUiScale(it) },
            onDismiss = { showUiScaleDialog = false }
        )
    }

    if (showFontScaleDialog) {
        FontScaleDialog(
            currentScale = settings.fontScale,
            onScaleChange = { settingsViewModel.setFontScale(it) },
            onDismiss = { showFontScaleDialog = false }
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        "package:${context.packageName}".toUri()
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

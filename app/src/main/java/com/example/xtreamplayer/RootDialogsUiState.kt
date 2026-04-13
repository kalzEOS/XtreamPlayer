package com.example.xtreamplayer

import androidx.compose.runtime.mutableStateOf
import com.example.xtreamplayer.settings.SubtitleAppearanceSettings

class RootDialogsUiState {
    val showThemeDialog = mutableStateOf(false)
    val showFontDialog = mutableStateOf(false)
    val showUiScaleDialog = mutableStateOf(false)
    val showFontScaleDialog = mutableStateOf(false)
    val showNextEpisodeThresholdDialog = mutableStateOf(false)
    val showVodBufferDialog = mutableStateOf(false)
    val showSubtitleAppearanceDialog = mutableStateOf(false)
    val subtitleAppearancePreview = mutableStateOf<SubtitleAppearanceSettings?>(null)
    val showSubtitleCacheAutoClearDialog = mutableStateOf(false)
    val showApiKeyDialog = mutableStateOf(false)
}

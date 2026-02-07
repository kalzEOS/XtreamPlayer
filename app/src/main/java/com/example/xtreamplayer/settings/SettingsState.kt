package com.example.xtreamplayer.settings

import com.example.xtreamplayer.ui.theme.AppFont

enum class AppThemeOption(val label: String) {
    DEFAULT("Default"),
    DEFAULT_LIGHT("Default Light"),
    DARK_PINK("Dark Pink"),
    DARK_PINK_LIGHT("Pink Light"),
    DARK_GREEN("Dark Green"),
    DARK_GREEN_LIGHT("Green Light"),
    DUSK_COPPER("Dusk Copper"),
    DUSK_COPPER_LIGHT("Copper Light")
}

enum class ClockFormatOption(val label: String) {
    AM_PM("AM/PM"),
    HOUR_24("24-hour")
}

data class SettingsState(
    val autoPlayNext: Boolean = true,
    val nextEpisodeThresholdSeconds: Int = 45,
    val subtitlesEnabled: Boolean = true,
    val rememberLogin: Boolean = true,
    val autoSignIn: Boolean = true,
    val appTheme: AppThemeOption = AppThemeOption.DEFAULT,
    val appFont: AppFont = AppFont.DEFAULT,
    val uiScale: Float = UI_SCALE_BASE,
    val fontScale: Float = 1.0f,
    val clockFormat: ClockFormatOption = ClockFormatOption.AM_PM,
    val openSubtitlesApiKey: String = "",
    val openSubtitlesUserAgent: String = ""
)

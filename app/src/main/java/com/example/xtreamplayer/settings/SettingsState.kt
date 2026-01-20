package com.example.xtreamplayer.settings

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

data class SettingsState(
    val autoPlayNext: Boolean = true,
    val nextEpisodeThresholdSeconds: Int = 60,
    val subtitlesEnabled: Boolean = false,
    val rememberLogin: Boolean = true,
    val autoSignIn: Boolean = true,
    val appTheme: AppThemeOption = AppThemeOption.DEFAULT,
    val openSubtitlesApiKey: String = "",
    val openSubtitlesUserAgent: String = ""
)

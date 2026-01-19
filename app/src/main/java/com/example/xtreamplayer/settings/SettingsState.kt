package com.example.xtreamplayer.settings

enum class AudioLanguage(val label: String) {
    ENGLISH("English"),
    SPANISH("Spanish"),
    FRENCH("French")
}

data class SettingsState(
    val autoPlayNext: Boolean = true,
    val subtitlesEnabled: Boolean = false,
    val audioLanguage: AudioLanguage = AudioLanguage.ENGLISH,
    val rememberLogin: Boolean = true,
    val autoSignIn: Boolean = true,
    val openSubtitlesApiKey: String = "",
    val openSubtitlesUserAgent: String = "XtreamPlayer"
)

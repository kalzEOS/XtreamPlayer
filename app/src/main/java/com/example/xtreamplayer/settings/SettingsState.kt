package com.example.xtreamplayer.settings

enum class PlaybackQuality(val label: String) {
    AUTO("Auto"),
    UHD_4K("4K"),
    FHD_1080("1080p"),
    HD_720("720p")
}

enum class AudioLanguage(val label: String) {
    ENGLISH("English"),
    SPANISH("Spanish"),
    FRENCH("French")
}

data class SettingsState(
    val autoPlayNext: Boolean = true,
    val subtitlesEnabled: Boolean = false,
    val playbackQuality: PlaybackQuality = PlaybackQuality.AUTO,
    val audioLanguage: AudioLanguage = AudioLanguage.ENGLISH,
    val rememberLogin: Boolean = true,
    val autoSignIn: Boolean = true,
    val openSubtitlesApiKey: String = "",
    val openSubtitlesUserAgent: String = "XtreamPlayer"
)

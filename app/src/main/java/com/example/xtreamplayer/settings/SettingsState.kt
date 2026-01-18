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

enum class ParentalRating(val label: String) {
    EVERYONE("Everyone"),
    TEEN("13+"),
    MATURE("16+"),
    ADULT("18+")
}

data class SettingsState(
    val autoPlayNext: Boolean = true,
    val subtitlesEnabled: Boolean = false,
    val matchFrameRate: Boolean = true,
    val playbackQuality: PlaybackQuality = PlaybackQuality.AUTO,
    val audioLanguage: AudioLanguage = AudioLanguage.ENGLISH,
    val rememberLogin: Boolean = true,
    val autoSignIn: Boolean = true,
    val parentalPinEnabled: Boolean = false,
    val parentalRating: ParentalRating = ParentalRating.EVERYONE,
    val openSubtitlesApiKey: String = "",
    val openSubtitlesUserAgent: String = "XtreamPlayer"
)

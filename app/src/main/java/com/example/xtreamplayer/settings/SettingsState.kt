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
    DUSK_COPPER_LIGHT("Copper Light"),
    MIDNIGHT_AURORA("Midnight Aurora"),
    MIDNIGHT_AURORA_LIGHT("Aurora Light"),
    VIOLET_NEBULA("Violet Nebula"),
    VIOLET_NEBULA_LIGHT("Violet Nebula Light")
}

enum class ClockFormatOption(val label: String) {
    AM_PM("AM/PM"),
    HOUR_24("24-hour")
}

enum class SubtitleCacheAutoClearOption(
    val intervalMs: Long,
    val label: String
) {
    OFF(0L, "Off"),
    ONE_DAY(24L * 60L * 60L * 1000L, "1 day"),
    THREE_DAYS(3L * 24L * 60L * 60L * 1000L, "3 days"),
    ONE_WEEK(7L * 24L * 60L * 60L * 1000L, "1 week"),
    TWO_WEEKS(14L * 24L * 60L * 60L * 1000L, "2 weeks"),
    THIRTY_DAYS(30L * 24L * 60L * 60L * 1000L, "30 days")
}

enum class SyncScheduleInterval(val hours: Long, val label: String) {
    OFF(0, "Off"),
    EVERY_6_HOURS(6, "Every 6 hours"),
    EVERY_12_HOURS(12, "Every 12 hours"),
    EVERY_24_HOURS(24, "Every 24 hours"),
    EVERY_7_DAYS(168, "Weekly")
}

fun syncScheduleLabel(interval: SyncScheduleInterval): String = interval.label

fun subtitleAutoClearLabel(intervalMs: Long): String {
    return SubtitleCacheAutoClearOption.entries
        .firstOrNull { it.intervalMs == intervalMs }
        ?.label
        ?: "Custom"
}

data class SettingsState(
    val autoPlayNext: Boolean = true,
    val nextEpisodeThresholdSeconds: Int = 45,
    val vodBufferEnabled: Boolean = false,
    val vodBufferSeconds: Int = 45,
    val subtitlesEnabled: Boolean = true,
    val subtitleAppearance: SubtitleAppearanceSettings = SubtitleAppearanceSettings(),
    val subtitleCacheAutoClearIntervalMs: Long = SubtitleCacheAutoClearOption.THIRTY_DAYS.intervalMs,
    val matchFrameRateEnabled: Boolean = true,
    val checkUpdatesOnStartup: Boolean = true,
    val refreshOnStartup: Boolean = false,
    val syncScheduleInterval: SyncScheduleInterval = SyncScheduleInterval.OFF,
    val rememberLogin: Boolean = true,
    val autoSignIn: Boolean = true,
    val appTheme: AppThemeOption = AppThemeOption.DEFAULT,
    val appFont: AppFont = AppFont.DEFAULT,
    val uiScale: Float = UI_SCALE_BASE,
    val fontScale: Float = 1.0f,
    val clockFormat: ClockFormatOption = ClockFormatOption.AM_PM,
    val openSubtitlesApiKey: String = "",
    val openSubtitlesUserAgent: String = "",
    val dualLiveEnabled: Boolean = false
)

fun vodBufferLabel(seconds: Int): String = "${seconds.coerceIn(5, 300)}s"

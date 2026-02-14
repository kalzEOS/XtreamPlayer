package com.example.xtreamplayer

import com.example.xtreamplayer.content.ContinueWatchingEntry

internal data class ResolvedSubtitlePersistence(
    val subtitleFileName: String?,
    val subtitleLanguage: String?,
    val subtitleLabel: String?,
    val subtitleOffsetMs: Long
)

internal fun ContinueWatchingEntry.toPlaybackSubtitleStateOrNull(): PlaybackSubtitleState? {
    val fileName = subtitleFileName?.takeUnless { it.isBlank() || it == "null" } ?: return null
    return PlaybackSubtitleState(
        fileName = fileName,
        language = subtitleLanguage?.takeUnless { it.isBlank() || it == "null" },
        label = subtitleLabel?.takeUnless { it.isBlank() || it == "null" },
        offsetMs = subtitleOffsetMs
    )
}

internal fun resolveSubtitlePersistence(
    existingEntry: ContinueWatchingEntry?,
    subtitleFileName: String?,
    subtitleLanguage: String?,
    subtitleLabel: String?,
    subtitleOffsetMs: Long
): ResolvedSubtitlePersistence {
    val hasSubtitleUpdate =
        subtitleFileName != null ||
            subtitleLanguage != null ||
            subtitleLabel != null ||
            subtitleOffsetMs != 0L
    return if (hasSubtitleUpdate) {
        ResolvedSubtitlePersistence(
            subtitleFileName = subtitleFileName?.takeUnless { it.isBlank() || it == "null" },
            subtitleLanguage = subtitleLanguage?.takeUnless { it.isBlank() || it == "null" },
            subtitleLabel = subtitleLabel?.takeUnless { it.isBlank() || it == "null" },
            subtitleOffsetMs = subtitleOffsetMs
        )
    } else {
        ResolvedSubtitlePersistence(
            subtitleFileName = existingEntry?.subtitleFileName,
            subtitleLanguage = existingEntry?.subtitleLanguage,
            subtitleLabel = existingEntry?.subtitleLabel,
            subtitleOffsetMs = existingEntry?.subtitleOffsetMs ?: 0L
        )
    }
}

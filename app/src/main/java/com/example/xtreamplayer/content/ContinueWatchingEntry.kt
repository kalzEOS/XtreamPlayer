package com.example.xtreamplayer.content

data class ContinueWatchingEntry(
    val key: String,
    val item: ContentItem,
    val positionMs: Long,
    val durationMs: Long,
    val timestampMs: Long,
    val parentItem: ContentItem? = null,
    val subtitleFileName: String? = null,
    val subtitleLanguage: String? = null,
    val subtitleLabel: String? = null,
    val subtitleOffsetMs: Long = 0L
)

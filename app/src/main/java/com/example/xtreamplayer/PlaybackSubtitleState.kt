package com.example.xtreamplayer

internal data class PlaybackSubtitleState(
    val fileName: String,
    val language: String?,
    val label: String?,
    val offsetMs: Long
)

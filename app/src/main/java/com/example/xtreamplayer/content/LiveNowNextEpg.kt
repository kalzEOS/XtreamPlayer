package com.example.xtreamplayer.content

data class LiveProgramInfo(
    val title: String,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null
)

data class LiveNowNextEpg(
    val now: LiveProgramInfo? = null,
    val next: LiveProgramInfo? = null
)

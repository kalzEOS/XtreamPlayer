package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section

data class LibrarySyncProgress(
    val section: Section,
    val sectionIndex: Int,
    val totalSections: Int,
    val itemsIndexed: Int,
    val progress: Float,
    val phase: SyncPhase = SyncPhase.BACKGROUND_FULL,
    val estimatedTotal: Int? = null
)

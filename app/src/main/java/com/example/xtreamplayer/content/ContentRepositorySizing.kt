package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section

internal fun indexPageSize(section: Section): Int {
    return when (section) {
        Section.SERIES -> 1000
        Section.MOVIES -> 800
        Section.LIVE -> 800
        else -> 200
    }
}

internal fun sectionProgress(pagesLoaded: Int): Float {
    val raw = 1f - (1f / (pagesLoaded + 1).toFloat())
    return raw.coerceIn(0.05f, 0.95f)
}

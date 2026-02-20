package com.example.xtreamplayer.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleAppearanceSettingsTest {

    @Test
    fun `normalized clamps subtitle appearance to allowed ranges`() {
        val normalized =
            SubtitleAppearanceSettings(
                customStyleEnabled = true,
                textSizeSp = 4f,
                backgroundOpacityPercent = 400,
                edgeType = Int.MAX_VALUE,
                bottomPaddingFraction = 5f
            ).normalized()

        assertEquals(SUBTITLE_TEXT_SIZE_MIN_SP, normalized.textSizeSp)
        assertEquals(100, normalized.backgroundOpacityPercent)
        assertEquals(SubtitleEdgeStyle.OUTLINE.value, normalized.edgeType)
        assertEquals(SUBTITLE_BOTTOM_PADDING_MAX, normalized.bottomPaddingFraction)
    }

    @Test
    fun `subtitleAppearanceLabel shows default when custom style disabled`() {
        assertEquals("Default", subtitleAppearanceLabel(SubtitleAppearanceSettings(customStyleEnabled = false)))
    }
}

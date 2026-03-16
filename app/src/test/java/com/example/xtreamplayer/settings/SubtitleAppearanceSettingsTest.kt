package com.example.xtreamplayer.settings

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `toAppliedSubtitleAppearance uses app defaults when custom style disabled`() {
        val applied =
            SubtitleAppearanceSettings(
                customStyleEnabled = false,
                textSizeSp = 32f,
                textColor = Color.BLUE,
                backgroundOpacityPercent = 10,
                edgeType = SubtitleEdgeStyle.NONE.value,
                bottomPaddingFraction = SUBTITLE_BOTTOM_PADDING_MIN,
                overrideEmbeddedStyles = true
            ).toAppliedSubtitleAppearance()

        assertTrue(applied.styleSettings.customStyleEnabled)
        assertEquals(18f, applied.styleSettings.textSizeSp)
        assertEquals(Color.WHITE, applied.styleSettings.textColor)
        assertEquals(55, applied.styleSettings.backgroundOpacityPercent)
        assertEquals(SubtitleEdgeStyle.OUTLINE.value, applied.styleSettings.edgeType)
        assertEquals(false, applied.styleSettings.overrideEmbeddedStyles)
        assertTrue(applied.applyEmbeddedStyles)
    }

    @Test
    fun `toAppliedSubtitleAppearance keeps custom values when custom style enabled`() {
        val applied =
            SubtitleAppearanceSettings(
                customStyleEnabled = true,
                textSizeSp = 22f,
                textColor = Color.YELLOW,
                backgroundOpacityPercent = 70,
                edgeType = SubtitleEdgeStyle.DROP_SHADOW.value,
                bottomPaddingFraction = 0.08f,
                overrideEmbeddedStyles = true
            ).toAppliedSubtitleAppearance()

        assertEquals(22f, applied.styleSettings.textSizeSp)
        assertEquals(Color.YELLOW, applied.styleSettings.textColor)
        assertEquals(70, applied.styleSettings.backgroundOpacityPercent)
        assertEquals(SubtitleEdgeStyle.DROP_SHADOW.value, applied.styleSettings.edgeType)
        assertEquals(0.08f, applied.styleSettings.bottomPaddingFraction)
        assertTrue(applied.styleSettings.overrideEmbeddedStyles)
        assertFalse(applied.applyEmbeddedStyles)
    }
}

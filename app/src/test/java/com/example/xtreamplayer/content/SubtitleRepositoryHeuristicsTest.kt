package com.example.xtreamplayer.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleRepositoryHeuristicsTest {

    @Test
    fun `detects likely subtitle payloads`() {
        val srt =
            """
            1
            00:00:01,000 --> 00:00:02,000
            Hello
            """.trimIndent()
        val vtt =
            """
            WEBVTT

            00:00.000 --> 00:01.000
            Hi
            """.trimIndent()

        assertTrue(isLikelySubtitlePayload(srt))
        assertTrue(isLikelySubtitlePayload(vtt))
    }

    @Test
    fun `rejects non subtitle payload text`() {
        assertFalse(isLikelySubtitlePayload("this is plain text without subtitle timing"))
        assertFalse(isLikelySubtitlePayload(""))
    }

    @Test
    fun `detects likely opensubtitles error payloads`() {
        assertTrue(hasLikelyOpenSubtitlesErrorMessage("""{"message":"rate limit reached"}"""))
        assertTrue(hasLikelyOpenSubtitlesErrorMessage("Request expired"))
        assertFalse(hasLikelyOpenSubtitlesErrorMessage("WEBVTT\n00:00.000 --> 00:01.000"))
    }
}

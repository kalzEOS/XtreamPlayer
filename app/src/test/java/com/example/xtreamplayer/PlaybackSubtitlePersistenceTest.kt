package com.example.xtreamplayer

import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.content.ContentType
import com.example.xtreamplayer.content.ContinueWatchingEntry
import com.example.xtreamplayer.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSubtitlePersistenceTest {

    @Test
    fun toPlaybackSubtitleStateOrNull_returnsNull_whenFileNameMissing() {
        val entryWithBlank = entry(subtitleFileName = " ")
        val entryWithLiteralNull = entry(subtitleFileName = "null")

        assertNull(entryWithBlank.toPlaybackSubtitleStateOrNull())
        assertNull(entryWithLiteralNull.toPlaybackSubtitleStateOrNull())
    }

    @Test
    fun toPlaybackSubtitleStateOrNull_mapsValidFields_andSanitizesMetadata() {
        val entry =
            entry(
                subtitleFileName = "123_en.srt",
                subtitleLanguage = "null",
                subtitleLabel = " ",
                subtitleOffsetMs = 1200L
            )

        val state = entry.toPlaybackSubtitleStateOrNull()

        requireNotNull(state)
        assertEquals("123_en.srt", state.fileName)
        assertNull(state.language)
        assertNull(state.label)
        assertEquals(1200L, state.offsetMs)
    }

    @Test
    fun resolveSubtitlePersistence_preservesExisting_whenNoSubtitleUpdateProvided() {
        val existing =
            entry(
                subtitleFileName = "existing_en.srt",
                subtitleLanguage = "en",
                subtitleLabel = "Existing",
                subtitleOffsetMs = 900L
            )

        val resolved =
            resolveSubtitlePersistence(
                existingEntry = existing,
                subtitleFileName = null,
                subtitleLanguage = null,
                subtitleLabel = null,
                subtitleOffsetMs = 0L
            )

        assertEquals("existing_en.srt", resolved.subtitleFileName)
        assertEquals("en", resolved.subtitleLanguage)
        assertEquals("Existing", resolved.subtitleLabel)
        assertEquals(900L, resolved.subtitleOffsetMs)
    }

    @Test
    fun resolveSubtitlePersistence_usesIncomingSubtitleUpdate_whenProvided() {
        val existing =
            entry(
                subtitleFileName = "existing_en.srt",
                subtitleLanguage = "en",
                subtitleLabel = "Existing",
                subtitleOffsetMs = 900L
            )

        val resolved =
            resolveSubtitlePersistence(
                existingEntry = existing,
                subtitleFileName = "new_es.srt",
                subtitleLanguage = "es",
                subtitleLabel = "Spanish",
                subtitleOffsetMs = -1500L
            )

        assertEquals("new_es.srt", resolved.subtitleFileName)
        assertEquals("es", resolved.subtitleLanguage)
        assertEquals("Spanish", resolved.subtitleLabel)
        assertEquals(-1500L, resolved.subtitleOffsetMs)
    }

    @Test
    fun resolveSubtitlePersistence_sanitizesBlankOrLiteralNullMetadata_onUpdate() {
        val resolved =
            resolveSubtitlePersistence(
                existingEntry = entry(),
                subtitleFileName = "null",
                subtitleLanguage = " ",
                subtitleLabel = "null",
                subtitleOffsetMs = 250L
            )

        assertNull(resolved.subtitleFileName)
        assertNull(resolved.subtitleLanguage)
        assertNull(resolved.subtitleLabel)
        assertEquals(250L, resolved.subtitleOffsetMs)
    }

    private fun entry(
        subtitleFileName: String? = null,
        subtitleLanguage: String? = null,
        subtitleLabel: String? = null,
        subtitleOffsetMs: Long = 0L
    ): ContinueWatchingEntry {
        return ContinueWatchingEntry(
            key = "k",
            item =
                ContentItem(
                    id = "id",
                    title = "Title",
                    subtitle = "",
                    imageUrl = null,
                    section = Section.MOVIES,
                    contentType = ContentType.MOVIES,
                    streamId = "12345",
                    containerExtension = "mp4"
                ),
            positionMs = 60_000L,
            durationMs = 600_000L,
            timestampMs = 1L,
            subtitleFileName = subtitleFileName,
            subtitleLanguage = subtitleLanguage,
            subtitleLabel = subtitleLabel,
            subtitleOffsetMs = subtitleOffsetMs
        )
    }
}

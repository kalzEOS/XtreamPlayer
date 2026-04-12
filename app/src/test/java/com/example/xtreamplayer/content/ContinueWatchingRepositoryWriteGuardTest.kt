package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueWatchingRepositoryWriteGuardTest {

    @Test
    fun shouldSkipContinueWatchingWrite_returnsTrue_when_only_position_changes_within_delta() {
        val existing = entry()

        val shouldSkip =
            shouldSkipContinueWatchingWrite(
                existingEntry = existing,
                targetKey = existing.key,
                item = existing.item,
                positionMs = existing.positionMs + 1_000L,
                durationMs = existing.durationMs,
                parentItem = existing.parentItem,
                subtitleFileName = existing.subtitleFileName,
                subtitleLanguage = existing.subtitleLanguage,
                subtitleLabel = existing.subtitleLabel,
                subtitleOffsetMs = existing.subtitleOffsetMs,
                minSaveDeltaMs = 30_000L
            )

        assertTrue(shouldSkip)
    }

    @Test
    fun shouldSkipContinueWatchingWrite_returnsFalse_when_metadata_changes() {
        val existing = entry()
        val changedItem = existing.item.copy(title = "Updated Title")

        val shouldSkip =
            shouldSkipContinueWatchingWrite(
                existingEntry = existing,
                targetKey = existing.key,
                item = changedItem,
                positionMs = existing.positionMs + 1_000L,
                durationMs = existing.durationMs,
                parentItem = existing.parentItem,
                subtitleFileName = existing.subtitleFileName,
                subtitleLanguage = existing.subtitleLanguage,
                subtitleLabel = existing.subtitleLabel,
                subtitleOffsetMs = existing.subtitleOffsetMs,
                minSaveDeltaMs = 30_000L
            )

        assertFalse(shouldSkip)
    }

    @Test
    fun shouldSkipContinueWatchingWrite_returnsTrue_when_only_nonpersisted_item_fields_change() {
        val existing = entry()
        val changedItem =
            existing.item.copy(
                description = "Updated description",
                duration = "52m",
                rating = "4.5",
                seasonLabel = "Season 2",
                episodeNumber = "8",
                categoryId = "cat-9"
            )

        val shouldSkip =
            shouldSkipContinueWatchingWrite(
                existingEntry = existing,
                targetKey = existing.key,
                item = changedItem,
                positionMs = existing.positionMs + 1_000L,
                durationMs = existing.durationMs,
                parentItem = existing.parentItem,
                subtitleFileName = existing.subtitleFileName,
                subtitleLanguage = existing.subtitleLanguage,
                subtitleLabel = existing.subtitleLabel,
                subtitleOffsetMs = existing.subtitleOffsetMs,
                minSaveDeltaMs = 30_000L
            )

        assertTrue(shouldSkip)
    }

    @Test
    fun shouldSkipContinueWatchingWrite_returnsFalse_when_key_needs_migration() {
        val existing = entry()

        val shouldSkip =
            shouldSkipContinueWatchingWrite(
                existingEntry = existing,
                targetKey = "${existing.key}-new",
                item = existing.item,
                positionMs = existing.positionMs + 1_000L,
                durationMs = existing.durationMs,
                parentItem = existing.parentItem,
                subtitleFileName = existing.subtitleFileName,
                subtitleLanguage = existing.subtitleLanguage,
                subtitleLabel = existing.subtitleLabel,
                subtitleOffsetMs = existing.subtitleOffsetMs,
                minSaveDeltaMs = 30_000L
            )

        assertFalse(shouldSkip)
    }

    private fun entry(): ContinueWatchingEntry {
        val item =
            ContentItem(
                id = "movie-1",
                title = "Movie",
                subtitle = "Subtitle",
                imageUrl = "https://img.example/poster.jpg",
                section = Section.MOVIES,
                contentType = ContentType.MOVIES,
                streamId = "stream-1",
                containerExtension = "mp4"
            )
        return ContinueWatchingEntry(
            key = "https://service|user|MOVIES|stream-1",
            item = item,
            positionMs = 120_000L,
            durationMs = 3_600_000L,
            timestampMs = 1L,
            parentItem = null,
            subtitleFileName = "movie_en.srt",
            subtitleLanguage = "en",
            subtitleLabel = "English",
            subtitleOffsetMs = 0L
        )
    }
}

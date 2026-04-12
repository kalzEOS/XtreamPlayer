package com.example.xtreamplayer.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPlaybackResumeRepositoryWriteGuardTest {

    @Test
    fun shouldSkipLocalResumeWrite_returnsTrue_when_only_position_changes_within_delta() {
        val existing =
            LocalPlaybackResumeEntry(
                mediaId = "local://movie.mp4",
                title = "Movie",
                positionMs = 60_000L,
                durationMs = 3_000_000L,
                timestampMs = 1L
            )

        val shouldSkip =
            shouldSkipLocalResumeWrite(
                existing = existing,
                title = existing.title,
                positionMs = existing.positionMs + 1_000L,
                durationMs = existing.durationMs,
                minSaveDeltaMs = 8_000L
            )

        assertTrue(shouldSkip)
    }

    @Test
    fun shouldSkipLocalResumeWrite_returnsFalse_when_title_changes() {
        val existing =
            LocalPlaybackResumeEntry(
                mediaId = "local://movie.mp4",
                title = "Old Title",
                positionMs = 60_000L,
                durationMs = 3_000_000L,
                timestampMs = 1L
            )

        val shouldSkip =
            shouldSkipLocalResumeWrite(
                existing = existing,
                title = "New Title",
                positionMs = existing.positionMs + 1_000L,
                durationMs = existing.durationMs,
                minSaveDeltaMs = 8_000L
            )

        assertFalse(shouldSkip)
    }

    @Test
    fun shouldSkipLocalResumeWrite_returnsFalse_when_duration_changes() {
        val existing =
            LocalPlaybackResumeEntry(
                mediaId = "local://movie.mp4",
                title = "Movie",
                positionMs = 60_000L,
                durationMs = 3_000_000L,
                timestampMs = 1L
            )

        val shouldSkip =
            shouldSkipLocalResumeWrite(
                existing = existing,
                title = existing.title,
                positionMs = existing.positionMs + 1_000L,
                durationMs = existing.durationMs + 10_000L,
                minSaveDeltaMs = 8_000L
            )

        assertFalse(shouldSkip)
    }
}

package com.example.xtreamplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackRecoveryTrackerTest {

    @Test
    fun recordFailure_returnsSoftRecovery_onlyAfterRepeatedDistinctFailures() {
        val tracker = PlaybackRecoveryTracker()
        val startMs = 1_000L

        assertEquals(
            PlaybackRecoveryAction.NONE,
            tracker.recordFailure(mediaId = "movie-1", nowMs = startMs)
        )
        assertEquals(
            PlaybackRecoveryAction.NONE,
            tracker.recordFailure(mediaId = "movie-1", nowMs = startMs + 5_000L)
        )
        assertEquals(
            PlaybackRecoveryAction.NONE,
            tracker.recordFailure(mediaId = "movie-2", nowMs = startMs + 10_000L)
        )
        assertEquals(
            PlaybackRecoveryAction.SOFT_RECOVERY,
            tracker.recordFailure(mediaId = "movie-2", nowMs = startMs + 15_000L)
        )
    }

    @Test
    fun recordFailure_returnsProcessRestart_whenFailurePersistsAfterSoftRecovery() {
        val tracker = PlaybackRecoveryTracker()
        val startMs = 10_000L

        repeat(4) { index ->
            val mediaId = if (index < 2) "movie-1" else "movie-2"
            tracker.recordFailure(mediaId = mediaId, nowMs = startMs + (index * 5_000L))
        }

        tracker.markSoftRecoveryPerformed(startMs + 20_000L)

        assertEquals(
            PlaybackRecoveryAction.PROCESS_RESTART,
            tracker.recordFailure(mediaId = "movie-3", nowMs = startMs + 30_000L)
        )
    }

    @Test
    fun markPlaybackHealthy_clearsRestartEscalationAfterRecoverySucceeds() {
        val tracker = PlaybackRecoveryTracker()
        val startMs = 50_000L

        repeat(4) { index ->
            val mediaId = if (index < 2) "movie-1" else "movie-2"
            tracker.recordFailure(mediaId = mediaId, nowMs = startMs + (index * 5_000L))
        }

        tracker.markSoftRecoveryPerformed(startMs + 20_000L)
        tracker.markPlaybackHealthy()

        assertEquals(
            PlaybackRecoveryAction.NONE,
            tracker.recordFailure(mediaId = "movie-3", nowMs = startMs + 30_000L)
        )
    }

    @Test
    fun recordFailure_doesNotRestart_whenRecoveryWindowHasExpired() {
        val tracker = PlaybackRecoveryTracker()
        val startMs = 100_000L

        repeat(4) { index ->
            val mediaId = if (index < 2) "movie-1" else "movie-2"
            tracker.recordFailure(mediaId = mediaId, nowMs = startMs + (index * 5_000L))
        }

        tracker.markSoftRecoveryPerformed(startMs + 20_000L)

        assertEquals(
            PlaybackRecoveryAction.NONE,
            tracker.recordFailure(mediaId = "movie-3", nowMs = startMs + 90_001L)
        )
    }
}

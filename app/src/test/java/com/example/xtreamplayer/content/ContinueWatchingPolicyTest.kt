package com.example.xtreamplayer.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueWatchingPolicyTest {

    @Test
    fun `min watch uses lower of fixed threshold and 10 percent`() {
        assertTrue(resolveContinueWatchingMinWatchMs(durationMs = 20_000L) == 2_000L)
        assertTrue(resolveContinueWatchingMinWatchMs(durationMs = 600_000L) == 30_000L)
    }

    @Test
    fun `entry is skipped before minimum watch time`() {
        assertFalse(
            shouldStoreContinueWatchingEntry(
                positionMs = 5_000L,
                durationMs = 120_000L
            )
        )
    }

    @Test
    fun `entry is removed near completion`() {
        assertFalse(
            shouldStoreContinueWatchingEntry(
                positionMs = 118_000L,
                durationMs = 120_000L
            )
        )
    }

    @Test
    fun `entry is stored in normal in-progress playback`() {
        assertTrue(
            shouldStoreContinueWatchingEntry(
                positionMs = 40_000L,
                durationMs = 120_000L
            )
        )
    }
}

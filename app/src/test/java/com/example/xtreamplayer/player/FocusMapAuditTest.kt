package com.example.xtreamplayer.player

import android.view.View
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusMapAuditTest {

    @Test
    fun `auditLinearFocusLane reports no issues for valid lane`() {
        val issues =
            auditLinearFocusLane(
                listOf(
                    FocusLinkSnapshot(1, View.NO_ID, 2),
                    FocusLinkSnapshot(2, 1, 3),
                    FocusLinkSnapshot(3, 2, View.NO_ID)
                )
            )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `auditLinearFocusLane reports invalid links`() {
        val issues =
            auditLinearFocusLane(
                listOf(
                    FocusLinkSnapshot(10, View.NO_ID, 999),
                    FocusLinkSnapshot(20, 10, View.NO_ID)
                )
            )

        assertTrue(issues.isNotEmpty())
    }
}

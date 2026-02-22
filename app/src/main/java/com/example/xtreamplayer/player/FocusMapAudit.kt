package com.example.xtreamplayer.player

import android.view.View

internal data class FocusLinkSnapshot(
    val id: Int,
    val nextFocusLeftId: Int,
    val nextFocusRightId: Int
)

internal fun auditLinearFocusLane(nodes: List<FocusLinkSnapshot>): List<String> {
    if (nodes.size <= 1) return emptyList()
    val issues = mutableListOf<String>()
    nodes.forEachIndexed { index, node ->
        val expectedLeft = nodes.getOrNull(index - 1)?.id ?: View.NO_ID
        val expectedRight = nodes.getOrNull(index + 1)?.id ?: View.NO_ID
        if (node.nextFocusLeftId != expectedLeft) {
            issues += "id=${node.id} left=${node.nextFocusLeftId} expected=$expectedLeft"
        }
        if (node.nextFocusRightId != expectedRight) {
            issues += "id=${node.id} right=${node.nextFocusRightId} expected=$expectedRight"
        }
    }
    return issues
}

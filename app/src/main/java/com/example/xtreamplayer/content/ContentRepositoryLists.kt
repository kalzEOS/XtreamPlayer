package com.example.xtreamplayer.content

internal fun interleaveLists(
    lists: List<List<ContentItem>>,
    maxItems: Int = Int.MAX_VALUE
): List<ContentItem> {
    val totalSize = lists.sumOf { it.size }
    val result = ArrayList<ContentItem>(totalSize.coerceAtMost(maxItems))
    val max = lists.maxOfOrNull { it.size } ?: 0
    for (index in 0 until max) {
        for (list in lists) {
            if (index < list.size) {
                result.add(list[index])
                if (result.size >= maxItems) return result
            }
        }
    }
    return result
}

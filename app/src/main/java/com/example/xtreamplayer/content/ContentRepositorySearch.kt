package com.example.xtreamplayer.content

internal fun collectSearchPageFromSources(
    sources: List<List<ContentItem>>,
    normalizedQuery: String,
    page: Int,
    limit: Int
): ContentPage {
    val targetStart = page * limit
    val matches = ArrayList<ContentItem>(limit)
    var matchIndex = 0
    var hasMoreMatches = false

    outer@ for (source in sources) {
        for (item in source) {
            if (!SearchNormalizer.matchesTitle(item.title, normalizedQuery)) {
                continue
            }
            if (matchIndex >= targetStart) {
                if (matches.size < limit) {
                    matches.add(item)
                } else {
                    hasMoreMatches = true
                    break@outer
                }
            }
            matchIndex++
        }
    }
    return ContentPage(items = matches, endReached = !hasMoreMatches)
}

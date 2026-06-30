package com.example.xtreamplayer.content

object SearchPolicy {
    const val MIN_QUERY_LENGTH = 2

    // Uses compact length (spaces stripped) so punctuation-split queries like "a e" (from "A&E")
    // count as 2 real characters and are allowed through.
    fun isSearchableNormalizedQuery(normalized: String): Boolean {
        if (normalized.isBlank()) return false
        return normalized.replace(" ", "").length >= MIN_QUERY_LENGTH
    }
}

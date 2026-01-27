package com.example.xtreamplayer.content

import android.util.LruCache

object SearchNormalizer {
    private val diacriticsRegex = Regex("\\p{Mn}+")
    private val nonAlnumRegex = Regex("[^\\p{L}\\p{N}]+")
    private val titleCache = LruCache<String, String>(200_000)

    fun clearCache() {
        titleCache.evictAll()
    }

    fun preWarmCache(titles: List<String>) {
        titles.forEach { title ->
            if (titleCache.get(title) == null) {
                val normalized = normalize(title)
                titleCache.put(title, normalized)
            }
        }
    }

    fun normalizeQuery(raw: String): String {
        return normalize(raw)
    }

    fun normalizeTitle(raw: String): String {
        titleCache.get(raw)?.let { return it }
        val normalized = normalize(raw)
        titleCache.put(raw, normalized)
        return normalized
    }

    fun matchesTitle(title: String, normalizedQuery: String): Boolean {
        if (normalizedQuery.isBlank()) {
            return true
        }
        val normalizedTitle = normalizeTitle(title)
        if (normalizedTitle.contains(normalizedQuery)) {
            return true
        }
        val tokens = normalizedQuery.split(' ').filter { it.isNotBlank() }
        if (tokens.size <= 1) {
            return false
        }
        return tokens.all { normalizedTitle.contains(it) }
    }

    private fun normalize(raw: String): String {
        val trimmed = stripLanguagePrefix(raw).trim()
        if (trimmed.isEmpty()) {
            return trimmed
        }
        val lower = trimmed.lowercase()
        val withoutDiacritics =
            if (lower.any { it.code > 127 }) {
                java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
                    .replace(diacriticsRegex, "")
            } else {
                lower
            }
        return nonAlnumRegex.replace(withoutDiacritics, " ").trim()
    }

    private fun stripLanguagePrefix(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length < 4) {
            return trimmed
        }
        val separatorIndex = trimmed.indexOfFirst { it == '-' || it == ':' || it == '|' }
        if (separatorIndex <= 0) {
            return trimmed
        }
        val prefix = trimmed.substring(0, separatorIndex).trim()
        val compactPrefix = prefix.replace(" ", "")
        val isLanguagePrefix = compactPrefix.length in 2..3 && compactPrefix.all { it.isLetter() }
        if (!isLanguagePrefix) {
            return trimmed
        }
        val remainder = trimmed.substring(separatorIndex + 1).trimStart()
        return if (remainder.isNotEmpty()) remainder else trimmed
    }
}

package com.example.xtreamplayer.content

import androidx.collection.LruCache

object SearchNormalizer {
    private val diacriticsRegex = Regex("\\p{Mn}+")
    private val nonAlnumRegex = Regex("[^\\p{L}\\p{N}]+")
    private val languagePrefixCodes = setOf(
        "en", "eng",
        "es", "spa",
        "fr", "fre", "fra",
        "de", "ger", "deu",
        "it", "ita",
        "pt", "por",
        "ru", "rus",
        "ja", "jpn",
        "ko", "kor",
        "zh", "chi", "zho",
        "ar", "ara",
        "hi", "hin",
        "tr", "tur",
        "nl", "dut", "nld",
        "pl", "pol",
        "sv", "swe",
        "no", "nor",
        "da", "dan",
        "fi", "fin",
        "cs", "cze", "ces",
        "el", "gre", "ell",
        "he", "heb",
        "th", "tha",
        "vi", "vie",
        "id", "ind",
        "ms", "may", "msa",
        "ro", "rum", "ron",
        "hu", "hun",
        "uk", "ukr",
        "bg", "bul",
        "hr", "hrv",
        "sr", "srp",
        "sk", "slo", "slk",
        "ca", "cat",
        "fa", "per", "fas"
    )
    private const val TITLE_CACHE_MAX_ENTRIES = 75_000
    private const val PREWARM_MAX_INSERTS = 5_000
    private val titleCache = LruCache<String, String>(TITLE_CACHE_MAX_ENTRIES)

    fun clearCache() {
        titleCache.evictAll()
    }

    fun preWarmCache(titles: List<String>) {
        if (titles.isEmpty()) return
        val remainingCapacity = (titleCache.maxSize() - titleCache.size()).coerceAtLeast(0)
        if (remainingCapacity == 0) return

        val insertBudget = minOf(PREWARM_MAX_INSERTS, remainingCapacity)
        var inserted = 0
        titles.forEach { title ->
            if (inserted >= insertBudget) return@forEach
            if (title.isBlank()) return@forEach
            if (titleCache.get(title) == null) {
                val normalized = normalizeTitleValue(title)
                titleCache.put(title, normalized)
                inserted++
            }
        }
    }

    fun normalizeQuery(raw: String): String {
        return normalizeQueryValue(raw)
    }

    fun normalizeTitle(raw: String): String {
        titleCache.get(raw)?.let { return it }
        val normalized = normalizeTitleValue(raw)
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

    private fun normalizeQueryValue(raw: String): String {
        return normalize(stripLanguagePrefix(raw))
    }

    private fun normalizeTitleValue(raw: String): String {
        return normalize(raw)
    }

    private fun normalize(raw: String): String {
        val trimmed = raw.trim()
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
        if (separatorIndex >= trimmed.lastIndex) {
            return trimmed
        }
        val separator = trimmed[separatorIndex]
        val hasValidDelimiterFormat =
            when (separator) {
                '|' -> trimmed[separatorIndex - 1].isWhitespace() && trimmed[separatorIndex + 1].isWhitespace()
                '-', ':' -> true
                else -> false
            }
        if (!hasValidDelimiterFormat) {
            return trimmed
        }
        val prefix = trimmed.substring(0, separatorIndex).trim()
        val compactPrefix = prefix.replace(" ", "")
        val normalizedPrefix = compactPrefix.lowercase()
        val isLanguagePrefix =
            compactPrefix.length in 2..3 &&
                compactPrefix.all { it.isLetter() } &&
                normalizedPrefix in languagePrefixCodes
        if (!isLanguagePrefix) {
            return trimmed
        }
        val remainder = trimmed.substring(separatorIndex + 1).trimStart()
        return if (remainder.isNotEmpty()) remainder else trimmed
    }
}

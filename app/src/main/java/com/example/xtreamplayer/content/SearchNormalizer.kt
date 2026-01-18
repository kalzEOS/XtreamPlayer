package com.example.xtreamplayer.content

object SearchNormalizer {
    fun normalizeQuery(raw: String): String {
        return stripLanguagePrefix(raw).trim()
    }

    fun normalizeTitle(raw: String): String = normalizeQuery(raw)

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

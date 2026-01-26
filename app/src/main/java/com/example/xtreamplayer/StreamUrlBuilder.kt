package com.example.xtreamplayer

import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.content.ContentType

object StreamUrlBuilder {
    fun buildUrl(config: AuthConfig, type: ContentType, streamId: String, extension: String?): String {
        return buildCandidates(config, type, streamId, extension).first()
    }

    fun buildCandidates(
        config: AuthConfig,
        type: ContentType,
        streamId: String,
        extension: String?
    ): List<String> {
        val base = normalizeBaseUrl(config.baseUrl)
        val user = config.username
        val pass = config.password
        val trimmedStreamId = streamId.trim()
        if (trimmedStreamId.startsWith("http://") || trimmedStreamId.startsWith("https://")) {
            return listOf(trimmedStreamId)
        }
        val ext = extension
            ?.trim()
            ?.removePrefix(".")
            ?.ifBlank { null }
        val basePath = when (type) {
            ContentType.LIVE -> "$base/live/$user/$pass"
            ContentType.MOVIES -> "$base/movie/$user/$pass"
            ContentType.SERIES -> "$base/series/$user/$pass"
        }
        val candidates = LinkedHashSet<String>()
        val hasInlineExtension =
            trimmedStreamId.substringAfterLast('/').contains('.')

        fun add(path: String) {
            candidates.add(path)
        }

        when (type) {
            ContentType.LIVE -> {
                val liveExt = ext ?: "ts"
                add("$basePath/$trimmedStreamId.$liveExt")
                add("$basePath/$trimmedStreamId")
            }
            ContentType.MOVIES, ContentType.SERIES -> {
                when {
                    hasInlineExtension -> {
                        add("$basePath/$trimmedStreamId")
                    }
                    !ext.isNullOrBlank() -> {
                        add("$basePath/$trimmedStreamId.$ext")
                    }
                    else -> {
                        add("$basePath/$trimmedStreamId.mp4")
                    }
                }
                val inlineExt =
                    if (hasInlineExtension) {
                        trimmedStreamId.substringAfterLast('.').lowercase()
                    } else {
                        null
                    }
                if (inlineExt != "m3u8" && ext?.lowercase() != "m3u8") {
                    val m3u8Path =
                        if (hasInlineExtension) {
                            val baseId = trimmedStreamId.substringBeforeLast('.')
                            "$basePath/$baseId.m3u8"
                        } else {
                            "$basePath/$trimmedStreamId.m3u8"
                        }
                    add(m3u8Path)
                }
                add("$basePath/$trimmedStreamId")
            }
        }
        return candidates.toList()
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().removeSuffix("/")
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }
}

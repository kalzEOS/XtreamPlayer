package com.example.xtreamplayer

import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.content.ContentType

object StreamUrlBuilder {
    fun buildUrl(config: AuthConfig, type: ContentType, streamId: String, extension: String?): String {
        val base = normalizeBaseUrl(config.baseUrl)
        val user = config.username
        val pass = config.password
        return when (type) {
            ContentType.LIVE -> "$base/live/$user/$pass/$streamId.ts"
            ContentType.MOVIES -> {
                val ext = extension?.ifBlank { "mp4" } ?: "mp4"
                "$base/movie/$user/$pass/$streamId.$ext"
            }
            ContentType.SERIES -> {
                val ext = extension?.ifBlank { "mp4" } ?: "mp4"
                "$base/series/$user/$pass/$streamId.$ext"
            }
        }
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

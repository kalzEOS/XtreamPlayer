package com.example.xtreamplayer.api

import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.content.ContentType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun buildApiUrl(
    config: AuthConfig,
    action: String?,
    params: Map<String, String>
): String? {
    val normalized = normalizeBaseUrl(config.baseUrl)
    val httpUrl = normalized.toHttpUrlOrNull() ?: return null
    val builder = httpUrl.newBuilder()
        .encodedPath("/player_api.php")
        .addQueryParameter("username", config.username)
        .addQueryParameter("password", config.password)
    if (!action.isNullOrBlank()) {
        builder.addQueryParameter("action", action)
    }
    params.forEach { (key, value) ->
        builder.addQueryParameter(key, value)
    }
    return builder.build().toString()
}

internal fun actionForSection(section: Section): String? {
    return when (section) {
        Section.ALL -> "get_vod_streams"
        Section.CONTINUE_WATCHING -> null
        Section.FAVORITES -> null
        Section.MOVIES -> "get_vod_streams"
        Section.SERIES -> "get_series"
        Section.LIVE -> "get_live_streams"
        Section.CATEGORIES -> "get_vod_categories"
        Section.LOCAL_FILES -> null
        Section.SETTINGS -> null
    }
}

internal fun actionForCategory(type: ContentType): String {
    return when (type) {
        ContentType.LIVE -> "get_live_categories"
        ContentType.MOVIES -> "get_vod_categories"
        ContentType.SERIES -> "get_series_categories"
    }
}

internal fun actionForContent(type: ContentType): String {
    return when (type) {
        ContentType.LIVE -> "get_live_streams"
        ContentType.MOVIES -> "get_vod_streams"
        ContentType.SERIES -> "get_series"
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

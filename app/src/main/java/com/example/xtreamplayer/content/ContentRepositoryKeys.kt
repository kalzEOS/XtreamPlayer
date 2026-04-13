package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig

internal fun accountKey(authConfig: AuthConfig): String {
    return "${authConfig.baseUrl}|${authConfig.username}|${authConfig.listName}"
}

internal fun indexKey(section: Section, authConfig: AuthConfig): String {
    return "${section.name}|${authConfig.baseUrl}|${authConfig.username}|${authConfig.listName}"
}

internal fun seasonCountKey(seriesId: String, authConfig: AuthConfig): String {
    return "seasons|${authConfig.baseUrl}|${authConfig.username}|${authConfig.listName}|$seriesId"
}

internal fun cacheKey(sectionKey: String, page: Int, limit: Int): String {
    return "$sectionKey-$page-$limit"
}

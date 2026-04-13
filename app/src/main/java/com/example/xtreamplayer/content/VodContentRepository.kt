package com.example.xtreamplayer.content

import com.example.xtreamplayer.api.XtreamApi
import com.example.xtreamplayer.auth.AuthConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class VodContentRepository(
    private val api: XtreamApi,
    private val contentCache: ContentCache
) {
    private val movieInfoCache = object : LinkedHashMap<String, MovieInfo>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MovieInfo>): Boolean {
            return size > 100
        }
    }
    private val movieInfoMutex = Mutex()

    suspend fun loadMovieInfo(
        item: ContentItem,
        authConfig: AuthConfig
    ): MovieInfo? {
        if (item.contentType != ContentType.MOVIES) {
            return null
        }
        val vodId = item.streamId.ifBlank { item.id }
        val key = "vod-info-${accountKey(authConfig)}-$vodId"
        movieInfoMutex.withLock {
            movieInfoCache[key]?.let { return it }
        }
        val cached = contentCache.readVodInfo(vodId, authConfig)
        if (cached != null) {
            movieInfoMutex.withLock { movieInfoCache[key] = cached }
            return cached
        }
        val result = api.fetchVodInfo(authConfig, vodId)
        val info = result.getOrElse { throw it }
        contentCache.writeVodInfo(vodId, authConfig, info)
        movieInfoMutex.withLock { movieInfoCache[key] = info }
        return info
    }

    suspend fun clearCache() {
        movieInfoMutex.withLock { movieInfoCache.clear() }
    }
}

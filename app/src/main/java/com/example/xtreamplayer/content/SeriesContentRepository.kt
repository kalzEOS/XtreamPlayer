package com.example.xtreamplayer.content

import com.example.xtreamplayer.auth.AuthConfig
import timber.log.Timber
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SeriesContentRepository(
    private val api: com.example.xtreamplayer.api.XtreamApi,
    private val contentCache: ContentCache
) {
    private val seriesEpisodesCache = object : LinkedHashMap<String, List<ContentItem>>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ContentItem>>): Boolean {
            return size > 50
        }
    }
    private val seriesSeasonCountCache = object : LinkedHashMap<String, Int>(200, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>): Boolean {
            return size > 200
        }
    }
    private val seriesSeasonCountUnavailableCache =
        object : LinkedHashMap<String, Boolean>(200, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>): Boolean {
                return size > 200
            }
        }
    private val seriesSeasonFullCache =
        object : LinkedHashMap<String, List<ContentItem>>(20, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, List<ContentItem>>
            ): Boolean {
                return size > 20
            }
        }
    private val seriesSeasonsCache =
        object : LinkedHashMap<String, List<SeasonSummary>>(50, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, List<SeasonSummary>>
            ): Boolean {
                return size > 50
            }
        }
    private val seriesEpisodesMutex = Mutex()
    private val seriesSeasonCountMutex = Mutex()
    private val seriesSeasonFullMutex = Mutex()
    private val seriesSeasonsMutex = Mutex()

    suspend fun loadSeriesEpisodePage(
        seriesId: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val allEpisodes = loadSeriesEpisodes(seriesId, authConfig)
        val offset = page * limit
        val slice = if (offset >= allEpisodes.size) {
            emptyList()
        } else {
            allEpisodes.subList(offset, (offset + limit).coerceAtMost(allEpisodes.size))
        }
        val endReached = offset + limit >= allEpisodes.size
        return ContentPage(items = slice, endReached = endReached)
    }

    suspend fun loadSeriesSeasonPage(
        seriesId: String,
        seasonLabel: String,
        offset: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val cacheKey = "series-season-${accountKey(authConfig)}-$seriesId-$seasonLabel"
        val key = cacheKey(cacheKey, offset, limit)
        val cached = contentCache.readPage(cacheKey, authConfig, offset, limit)
        if (cached != null) {
            return cached
        }
        val result = api.fetchSeriesSeasonPage(authConfig, seriesId, seasonLabel, offset, limit)
        val pageData = result.getOrElse { throw it }
        if (pageData.items.isNotEmpty()) {
            contentCache.writePage(cacheKey, authConfig, offset, limit, pageData)
        }
        return pageData
    }

    suspend fun loadSeriesEpisodes(
        seriesId: String,
        authConfig: AuthConfig
    ): List<ContentItem> {
        val key = "${accountKey(authConfig)}|$seriesId"
        val cachedSeries = seriesEpisodesMutex.withLock { seriesEpisodesCache[key] }
        if (cachedSeries != null) {
            return cachedSeries
        }
        val result = api.fetchSeriesEpisodesPage(authConfig, seriesId, 0, Int.MAX_VALUE)
        val pageData = result.getOrElse { throw it }
        val fullList = pageData.items
        seriesEpisodesMutex.withLock { seriesEpisodesCache[key] = fullList }
        return fullList
    }

    suspend fun loadSeriesSeasonCount(
        seriesId: String,
        authConfig: AuthConfig
    ): Int? {
        val key = seasonCountKey(seriesId, authConfig)
        seriesSeasonCountMutex.withLock {
            if (seriesSeasonCountUnavailableCache[key] == true) {
                return null
            }
            seriesSeasonCountCache[key]?.let { return it }
        }
        val result = api.fetchSeriesSeasonCount(authConfig, seriesId)
        val count = result.getOrNull() ?: return null
        seriesSeasonCountMutex.withLock {
            seriesSeasonCountCache[key] = count
            seriesSeasonCountUnavailableCache.remove(key)
        }
        return count
    }

    suspend fun loadSeriesSeasons(
        seriesId: String,
        authConfig: AuthConfig
    ): List<SeasonSummary> {
        val key = seasonCountKey(seriesId, authConfig)
        seriesSeasonsMutex.withLock {
            seriesSeasonsCache[key]?.let { return it }
        }
        val result = api.fetchSeriesSeasonSummaries(authConfig, seriesId)
        val payload = result.getOrElse { throw it }
        val summaries = payload.summaries
        seriesSeasonsMutex.withLock { seriesSeasonsCache[key] = summaries }
        seriesSeasonCountMutex.withLock {
            if (payload.seasonCount != null) {
                seriesSeasonCountCache[key] = payload.seasonCount
                seriesSeasonCountUnavailableCache.remove(key)
            } else if (summaries.isEmpty()) {
                seriesSeasonCountUnavailableCache[key] = true
            }
        }
        return summaries
    }

    fun peekSeriesSeasonFullCache(
        seriesId: String,
        seasonLabel: String,
        authConfig: AuthConfig
    ): List<ContentItem>? {
        val key = "season-full-${accountKey(authConfig)}-$seriesId-$seasonLabel"
        if (!seriesSeasonFullMutex.tryLock()) {
            return null
        }
        return try {
            seriesSeasonFullCache[key]
        } finally {
            seriesSeasonFullMutex.unlock()
        }
    }

    suspend fun prefetchSeriesSeasonFull(
        seriesId: String,
        seasonLabel: String,
        authConfig: AuthConfig
    ) {
        runCatching { loadSeriesSeasonFull(seriesId, seasonLabel, authConfig) }
            .onFailure { Timber.w(it, "Failed to prefetch season $seasonLabel") }
    }

    suspend fun loadSeriesSeasonFull(
        seriesId: String,
        seasonLabel: String,
        authConfig: AuthConfig
    ): List<ContentItem> {
        val key = "season-full-${accountKey(authConfig)}-$seriesId-$seasonLabel"
        seriesSeasonFullMutex.withLock {
            seriesSeasonFullCache[key]?.let { return it }
        }
        val cached = contentCache.readSeasonFull(seriesId, seasonLabel, authConfig)
        if (cached != null) {
            seriesSeasonFullMutex.withLock { seriesSeasonFullCache[key] = cached }
            return cached
        }
        val items =
            api.fetchSeriesSeasonAll(authConfig, seriesId, seasonLabel)
                .getOrElse { throw it }
        contentCache.writeSeasonFull(seriesId, seasonLabel, authConfig, items)
        seriesSeasonFullMutex.withLock { seriesSeasonFullCache[key] = items }
        return items
    }

    suspend fun clearCache() {
        seriesEpisodesMutex.withLock { seriesEpisodesCache.clear() }
        seriesSeasonCountMutex.withLock { seriesSeasonCountCache.clear() }
        seriesSeasonCountMutex.withLock { seriesSeasonCountUnavailableCache.clear() }
        seriesSeasonFullMutex.withLock { seriesSeasonFullCache.clear() }
        seriesSeasonsMutex.withLock { seriesSeasonsCache.clear() }
    }
}

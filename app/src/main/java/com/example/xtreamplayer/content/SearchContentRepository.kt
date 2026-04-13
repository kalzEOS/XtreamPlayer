package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section
import com.example.xtreamplayer.api.XtreamApi
import com.example.xtreamplayer.auth.AuthConfig
import java.util.LinkedHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.ceil

internal class SearchContentRepository(
    private val api: XtreamApi,
    private val contentCache: ContentCache,
    private val searchIndexRepository: SearchIndexRepository
) {
    private companion object {
        const val MIN_LOCAL_SEARCH_QUERY_LENGTH = 2
    }

    private val memoryCache = object : LinkedHashMap<String, ContentPage>(200, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ContentPage>): Boolean {
            return size > 200
        }
    }
    private val memoryCacheMutex = Mutex()
    private val locks = Section.values().associateWith { Mutex() }

    suspend fun searchPage(
        section: Section,
        query: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val normalizedQuery = SearchNormalizer.normalizeQuery(query)
        val canUseLocalIndex = searchIndexRepository.isSearchIndexReadyForQuery(section, authConfig)
        if (canUseLocalIndex && normalizedQuery.length >= MIN_LOCAL_SEARCH_QUERY_LENGTH) {
            val localResult = localSearchPage(section, normalizedQuery, page, limit, authConfig)
            if (localResult != null) {
                return localResult
            }
        }
        return if (section == Section.ALL) {
            searchMixedPage(query, page, limit, authConfig)
        } else {
            searchSectionPage(section, query, page, limit, authConfig)
        }
    }

    suspend fun searchFilterPages(
        limit: Int,
        page: Int,
        matcher: (ContentItem) -> Boolean,
        pageLoader: suspend (Int, Int) -> ContentPage,
        maxScanPages: Int
    ): ContentPage {
        val targetStart = page * limit
        val items = ArrayList<ContentItem>(limit)
        var matchIndex = 0
        var rawPage = 0
        var endReached = true
        while (true) {
            if (rawPage >= maxScanPages) {
                endReached = true
                break
            }
            val pageData = pageLoader(rawPage, limit)
            val matchResult = withContext(Dispatchers.Default) {
                val matches = mutableListOf<Pair<ContentItem, Int>>()
                var localMatchIndex = matchIndex
                pageData.items.forEach { item ->
                    if (matcher(item)) {
                        matches.add(item to localMatchIndex)
                        localMatchIndex++
                    }
                }
                matches to localMatchIndex
            }
            matchResult.first.forEach { (item, idx) ->
                if (idx >= targetStart && items.size < limit) {
                    items.add(item)
                }
            }
            matchIndex = matchResult.second
            if (pageData.endReached) {
                endReached = true
                break
            }
            if (items.size >= limit) {
                endReached = false
                break
            }
            rawPage++
        }
        return ContentPage(items = items, endReached = endReached)
    }

    suspend fun clearCache() {
        memoryCacheMutex.withLock { memoryCache.clear() }
    }

    internal suspend fun loadSectionPage(
        section: Section,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val key = cacheKey(section.name, page, limit)
        memoryCacheMutex.withLock {
            memoryCache[key]?.let { return it }
        }

        val lock = locks[section] ?: Mutex()
        return lock.withLock {
            memoryCacheMutex.withLock {
                memoryCache[key]?.let { return it }
            }
            val cached = contentCache.readPage(section, authConfig, page, limit)
            if (cached != null) {
                memoryCacheMutex.withLock { memoryCache[key] = cached }
                return@withLock cached
            }

            val result = api.fetchSectionPage(section, authConfig, page, limit)
            val pageData = result.getOrElse { throw it }
            if (pageData.items.isNotEmpty()) {
                contentCache.writePage(section, authConfig, page, limit, pageData)
            }
            memoryCacheMutex.withLock { memoryCache[key] = pageData }
            return@withLock pageData
        }
    }

    private suspend fun searchSectionPage(
        section: Section,
        query: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        if (
            section == Section.SETTINGS ||
            section == Section.CATEGORIES ||
            section == Section.ALL ||
            section == Section.LOCAL_FILES ||
            section == Section.FAVORITES
        ) {
            return ContentPage(items = emptyList(), endReached = true)
        }
        val normalizedQuery = SearchNormalizer.normalizeQuery(query)
        val rawQuery = query.trim()
        if (rawQuery.isBlank()) {
            return loadSectionPage(section, page, limit, authConfig)
        }
        val key = cacheKey("search-${section.name}-$normalizedQuery", page, limit)
        memoryCacheMutex.withLock {
            memoryCache[key]?.let { return it }
        }
        val apiResult = api.fetchSearchPage(section, authConfig, rawQuery, page, limit)
        val pageData = apiResult.getOrElse {
            return searchFilterPages(
                limit = limit,
                page = page,
                matcher = { item -> SearchNormalizer.matchesTitle(item.title, normalizedQuery) },
                pageLoader = { rawPage, rawLimit ->
                    loadSectionPage(section, rawPage, rawLimit, authConfig)
                },
                maxScanPages = 10
            ).also { fallback ->
                memoryCacheMutex.withLock { memoryCache[key] = fallback }
            }
        }
        val filtered = withContext(Dispatchers.Default) {
            pageData.items.filter { item ->
                SearchNormalizer.matchesTitle(item.title, normalizedQuery)
            }
        }
        if (filtered.isNotEmpty() || pageData.endReached) {
            return ContentPage(items = filtered, endReached = pageData.endReached).also { finalPage ->
                memoryCacheMutex.withLock { memoryCache[key] = finalPage }
            }
        }
        return searchFilterPages(
            limit = limit,
            page = page,
            matcher = { item ->
                SearchNormalizer.matchesTitle(item.title, normalizedQuery)
            },
            pageLoader = { rawPage, rawLimit ->
                loadSectionPage(section, rawPage, rawLimit, authConfig)
            },
            maxScanPages = 10
        ).also { fallback ->
            memoryCacheMutex.withLock { memoryCache[key] = fallback }
        }
    }

    internal suspend fun loadMixedPage(
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val key = cacheKey(Section.ALL.name, page, limit)
        memoryCacheMutex.withLock {
            memoryCache[key]?.let { return it }
        }

        val perSectionLimit = ceil(limit / 3.0).toInt().coerceAtLeast(1)
        val (live, movies, series) = coroutineScope {
            val liveDeferred = async { loadSectionPage(Section.LIVE, page, perSectionLimit, authConfig) }
            val moviesDeferred = async { loadSectionPage(Section.MOVIES, page, perSectionLimit, authConfig) }
            val seriesDeferred = async { loadSectionPage(Section.SERIES, page, perSectionLimit, authConfig) }
            Triple(liveDeferred.await(), moviesDeferred.await(), seriesDeferred.await())
        }

        val mixed = interleaveLists(listOf(live.items, movies.items, series.items), maxItems = limit)
        val endReached = live.endReached && movies.endReached && series.endReached

        val pageData = ContentPage(items = mixed, endReached = endReached)
        memoryCacheMutex.withLock { memoryCache[key] = pageData }
        return pageData
    }

    private suspend fun searchMixedPage(
        query: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val key = cacheKey("search-${Section.ALL.name}-$query", page, limit)
        memoryCacheMutex.withLock {
            memoryCache[key]?.let { return it }
        }

        val perSectionLimit = ceil(limit / 3.0).toInt().coerceAtLeast(1)
        val (live, movies, series) = coroutineScope {
            val liveDeferred = async { searchSectionPage(Section.LIVE, query, page, perSectionLimit, authConfig) }
            val moviesDeferred = async { searchSectionPage(Section.MOVIES, query, page, perSectionLimit, authConfig) }
            val seriesDeferred = async { searchSectionPage(Section.SERIES, query, page, perSectionLimit, authConfig) }
            Triple(liveDeferred.await(), moviesDeferred.await(), seriesDeferred.await())
        }

        val mixed = interleaveLists(listOf(live.items, movies.items, series.items), maxItems = limit)
        val endReached = live.endReached && movies.endReached && series.endReached

        val pageData = ContentPage(items = mixed, endReached = endReached)
        memoryCacheMutex.withLock { memoryCache[key] = pageData }
        return pageData
    }

    private suspend fun localSearchPage(
        section: Section,
        normalizedQuery: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage? {
        if (normalizedQuery.length < MIN_LOCAL_SEARCH_QUERY_LENGTH) {
            return null
        }
        if (
            section == Section.SETTINGS ||
            section == Section.CATEGORIES ||
            section == Section.LOCAL_FILES ||
            section == Section.FAVORITES
        ) {
            return null
        }
        val sources =
            if (section == Section.ALL) {
                listOf(
                    searchIndexRepository.loadSectionIndex(Section.LIVE, authConfig).orEmpty(),
                    searchIndexRepository.loadSectionIndex(Section.MOVIES, authConfig).orEmpty(),
                    searchIndexRepository.loadSectionIndex(Section.SERIES, authConfig).orEmpty()
                ).filter { it.isNotEmpty() }
            } else {
                listOf(searchIndexRepository.loadSectionIndex(section, authConfig) ?: return null)
            }
        if (sources.isEmpty()) {
            return null
        }
        Timber.d(
            "Local search using cached indexes: section=$section queryLen=${normalizedQuery.length} " +
                "sourceSizes=${sources.joinToString(",") { it.size.toString() }}"
        )
        return withContext(Dispatchers.Default) {
            collectSearchPageFromSources(
                sources = sources,
                normalizedQuery = normalizedQuery,
                page = page,
                limit = limit
            )
        }
    }
}

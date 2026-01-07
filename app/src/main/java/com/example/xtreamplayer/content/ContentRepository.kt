package com.example.xtreamplayer.content

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.api.XtreamApi
import com.example.xtreamplayer.auth.AuthConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil

class ContentRepository(
    private val api: XtreamApi,
    private val contentCache: ContentCache
) {
    private val memoryCache = object : LinkedHashMap<String, ContentPage>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ContentPage>): Boolean {
            return size > 8
        }
    }
    private val locks = Section.values().associateWith { Mutex() }
    private val categoryLock = Mutex()
    private val categoryCache = mutableMapOf<ContentType, List<CategoryItem>>()

    fun pager(section: Section, authConfig: AuthConfig): Pager<Int, ContentItem> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 12,
                initialLoadSize = 80,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                XtreamContentPagingSource(section, authConfig, this)
            }
        )
    }

    fun categoryPager(
        type: ContentType,
        categoryId: String,
        authConfig: AuthConfig
    ): Pager<Int, ContentItem> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 12,
                initialLoadSize = 80,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                XtreamCategoryPagingSource(type, categoryId, authConfig, this)
            }
        )
    }

    fun seriesPager(seriesId: String, authConfig: AuthConfig): Pager<Int, ContentItem> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 12,
                initialLoadSize = 80,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                XtreamSeriesPagingSource(seriesId, authConfig, this)
            }
        )
    }

    suspend fun loadPage(section: Section, page: Int, limit: Int, authConfig: AuthConfig): ContentPage {
        return if (section == Section.ALL) {
            loadMixedPage(page, limit, authConfig)
        } else {
            loadSectionPage(section, page, limit, authConfig)
        }
    }

    suspend fun loadCategoryPage(
        type: ContentType,
        categoryId: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val key = cacheKey("${type.name}-$categoryId", page, limit)
        synchronized(memoryCache) {
            memoryCache[key]?.let { return it }
        }
        val result = api.fetchCategoryPage(type, authConfig, categoryId, page, limit)
        val pageData = result.getOrElse { throw it }
        synchronized(memoryCache) { memoryCache[key] = pageData }
        return pageData
    }

    suspend fun loadSeriesEpisodePage(
        seriesId: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val key = cacheKey("series-$seriesId", page, limit)
        synchronized(memoryCache) {
            memoryCache[key]?.let { return it }
        }
        val result = api.fetchSeriesEpisodesPage(authConfig, seriesId, page, limit)
        val pageData = result.getOrElse { throw it }
        synchronized(memoryCache) { memoryCache[key] = pageData }
        return pageData
    }

    suspend fun loadCategories(type: ContentType, authConfig: AuthConfig): List<CategoryItem> {
        categoryLock.withLock {
            categoryCache[type]?.let { return it }
            val result = api.fetchCategories(type, authConfig)
            val categories = result.getOrElse { throw it }
            categoryCache[type] = categories
            return categories
        }
    }

    private suspend fun loadSectionPage(
        section: Section,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val key = cacheKey(section.name, page, limit)
        synchronized(memoryCache) {
            memoryCache[key]?.let { return it }
        }

        val lock = locks[section] ?: Mutex()
        return lock.withLock {
            synchronized(memoryCache) {
                memoryCache[key]?.let { return it }
            }
            val cached = contentCache.readPage(section, authConfig, page, limit)
            if (cached != null) {
                synchronized(memoryCache) { memoryCache[key] = cached }
                return@withLock cached
            }

            val result = api.fetchSectionPage(section, authConfig, page, limit)
            val pageData = result.getOrElse { throw it }
            if (pageData.items.isNotEmpty()) {
                contentCache.writePage(section, authConfig, page, limit, pageData)
            }
            synchronized(memoryCache) { memoryCache[key] = pageData }
            return@withLock pageData
        }
    }

    private suspend fun loadMixedPage(
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val key = cacheKey(Section.ALL.name, page, limit)
        synchronized(memoryCache) {
            memoryCache[key]?.let { return it }
        }

        val perSectionLimit = ceil(limit / 3.0).toInt().coerceAtLeast(1)
        val live = loadSectionPage(Section.LIVE, page, perSectionLimit, authConfig)
        val movies = loadSectionPage(Section.MOVIES, page, perSectionLimit, authConfig)
        val series = loadSectionPage(Section.SERIES, page, perSectionLimit, authConfig)

        val mixed = interleaveLists(listOf(live.items, movies.items, series.items))
            .take(limit)
        val endReached = live.endReached && movies.endReached && series.endReached

        val pageData = ContentPage(items = mixed, endReached = endReached)
        synchronized(memoryCache) { memoryCache[key] = pageData }
        return pageData
    }

    fun clearCache() {
        synchronized(memoryCache) { memoryCache.clear() }
        categoryCache.clear()
    }

    suspend fun clearDiskCache() {
        contentCache.clearAll()
    }

    private fun cacheKey(sectionKey: String, page: Int, limit: Int): String {
        return "$sectionKey-$page-$limit"
    }

    private fun interleaveLists(lists: List<List<ContentItem>>): List<ContentItem> {
        val totalSize = lists.sumOf { it.size }
        val result = ArrayList<ContentItem>(totalSize)
        val max = lists.maxOfOrNull { it.size } ?: 0
        for (index in 0 until max) {
            for (list in lists) {
                if (index < list.size) {
                    result.add(list[index])
                }
            }
        }
        return result
    }
}

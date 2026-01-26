package com.example.xtreamplayer.content

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.api.XtreamApi
import com.example.xtreamplayer.auth.AuthConfig
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.ceil

class ContentRepository(
    private val api: XtreamApi,
    private val contentCache: ContentCache
) {
    private companion object {
        const val DEFAULT_PAGE_SIZE = 24
        const val DEFAULT_PREFETCH_DISTANCE = 6
        const val DEFAULT_INITIAL_LOAD = 48
        const val SEARCH_PAGE_SIZE = 15
        const val SEARCH_PREFETCH_DISTANCE = 5
        const val SEARCH_INITIAL_LOAD = 15
        const val MIN_INDEX_PAGE_SIZE = 200
        const val MIN_LOCAL_SEARCH_QUERY_LENGTH = 2
        const val FAST_START_PAGE_SIZE = 400
        const val FAST_START_PAGES = 2
        const val BACKGROUND_PAGE_SIZE = 400
        const val BACKGROUND_SAVE_INTERVAL = 10
        const val BACKGROUND_THROTTLE_MS = 200L
        const val BOOST_PAGE_SIZE = 400
    }

    private val memoryCache = object : LinkedHashMap<String, ContentPage>(200, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ContentPage>): Boolean {
            return size > 200
        }
    }
    private val categoryThumbnailCache = object : LinkedHashMap<String, String?>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String?>): Boolean {
            return size > 50
        }
    }
    private val sectionIndexCache = mutableMapOf<String, List<ContentItem>>()
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
    private val locks = Section.values().associateWith { Mutex() }
    private val categoryLock = Mutex()
    private val memoryCacheMutex = Mutex()
    private val seriesEpisodesMutex = Mutex()
    private val seriesSeasonCountMutex = Mutex()
    private val categoryThumbnailMutex = Mutex()
    private val categoryCache = object : LinkedHashMap<String, List<CategoryItem>>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<CategoryItem>>): Boolean {
            return size > 100
        }
    }

    fun pager(section: Section, authConfig: AuthConfig): Pager<Int, ContentItem> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                prefetchDistance = DEFAULT_PREFETCH_DISTANCE,
                initialLoadSize = DEFAULT_INITIAL_LOAD,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                XtreamContentPagingSource(section, authConfig, this)
            }
        )
    }

    fun searchPager(
        section: Section,
        query: String,
        authConfig: AuthConfig
    ): Pager<Int, ContentItem> {
        return Pager(
            config = PagingConfig(
                pageSize = SEARCH_PAGE_SIZE,
                prefetchDistance = SEARCH_PREFETCH_DISTANCE,
                initialLoadSize = SEARCH_INITIAL_LOAD,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                XtreamSearchPagingSource(section, query, authConfig, this)
            }
        )
    }

    fun categorySearchPager(
        type: ContentType,
        categoryId: String,
        query: String,
        authConfig: AuthConfig
    ): Pager<Int, ContentItem> {
        return Pager(
            config = PagingConfig(
                pageSize = SEARCH_PAGE_SIZE,
                prefetchDistance = SEARCH_PREFETCH_DISTANCE,
                initialLoadSize = SEARCH_INITIAL_LOAD,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                XtreamCategorySearchPagingSource(type, categoryId, query, authConfig, this)
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
                pageSize = DEFAULT_PAGE_SIZE,
                prefetchDistance = DEFAULT_PREFETCH_DISTANCE,
                initialLoadSize = DEFAULT_INITIAL_LOAD,
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
                pageSize = DEFAULT_PAGE_SIZE,
                prefetchDistance = DEFAULT_PREFETCH_DISTANCE,
                initialLoadSize = DEFAULT_INITIAL_LOAD,
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

    suspend fun searchPage(
        section: Section,
        query: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val normalizedQuery = SearchNormalizer.normalizeQuery(query)
        val canUseLocalIndex = isSearchIndexComplete(section, authConfig)
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

    suspend fun searchCategoryPage(
        type: ContentType,
        categoryId: String,
        query: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val normalizedQuery = SearchNormalizer.normalizeQuery(query)
        if (normalizedQuery.isBlank()) {
            return loadCategoryPage(type, categoryId, page, limit, authConfig)
        }
        val key = cacheKey("search-${type.name}-$categoryId-$normalizedQuery", page, limit)
        memoryCacheMutex.withLock {
            memoryCache[key]?.let { return it }
        }
        return searchFilterPages(
            limit = limit,
            page = page,
            matcher = { item ->
                SearchNormalizer.matchesTitle(item.title, normalizedQuery)
            },
            pageLoader = { rawPage, rawLimit ->
                loadCategoryPage(type, categoryId, rawPage, rawLimit, authConfig)
            },
            maxScanPages = 6
        ).also { pageData ->
            memoryCacheMutex.withLock { memoryCache[key] = pageData }
        }
    }

    suspend fun loadCategoryPage(
        type: ContentType,
        categoryId: String,
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val cacheKey = "category-${type.name}-$categoryId"
        val key = cacheKey(cacheKey, page, limit)
        memoryCacheMutex.withLock {
            memoryCache[key]?.let { return it }
        }
        val cached = contentCache.readPage(cacheKey, authConfig, page, limit)
        if (cached != null) {
            memoryCacheMutex.withLock { memoryCache[key] = cached }
            return cached
        }
        val result = api.fetchCategoryPage(type, authConfig, categoryId, page, limit)
        val pageData = result.getOrElse { throw it }
        contentCache.writePage(cacheKey, authConfig, page, limit, pageData)
        memoryCacheMutex.withLock { memoryCache[key] = pageData }
        return pageData
    }

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

    suspend fun loadSeriesEpisodes(
        seriesId: String,
        authConfig: AuthConfig
    ): List<ContentItem> {
        val cachedSeries = seriesEpisodesMutex.withLock { seriesEpisodesCache[seriesId] }
        if (cachedSeries != null) {
            return cachedSeries
        }
        val result = api.fetchSeriesEpisodesPage(authConfig, seriesId, 0, Int.MAX_VALUE)
        val pageData = result.getOrElse { throw it }
        val fullList = pageData.items
        seriesEpisodesMutex.withLock { seriesEpisodesCache[seriesId] = fullList }
        return fullList
    }

    suspend fun loadSeriesSeasonCount(
        seriesId: String,
        authConfig: AuthConfig
    ): Int? {
        val key = seasonCountKey(seriesId, authConfig)
        seriesSeasonCountMutex.withLock {
            seriesSeasonCountCache[key]?.let { return it }
        }
        val result = api.fetchSeriesSeasonCount(authConfig, seriesId)
        val count = result.getOrNull() ?: return null
        seriesSeasonCountMutex.withLock { seriesSeasonCountCache[key] = count }
        return count
    }

    suspend fun loadCategories(
        type: ContentType,
        authConfig: AuthConfig,
        forceRefresh: Boolean = false
    ): List<CategoryItem> {
        categoryLock.withLock {
            val key = "${accountKey(authConfig)}-${type.name}"
            if (!forceRefresh) {
                categoryCache[key]?.let { return it }
                val cached = contentCache.readCategories(type, authConfig)
                if (cached != null) {
                    categoryCache[key] = cached
                    return cached
                }
            }
            val result = api.fetchCategories(type, authConfig)
            val categories = result.getOrElse { throw it }
            categoryCache[key] = categories
            contentCache.writeCategories(type, authConfig, categories)
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

    private suspend fun loadMixedPage(
        page: Int,
        limit: Int,
        authConfig: AuthConfig
    ): ContentPage {
        val key = cacheKey(Section.ALL.name, page, limit)
        memoryCacheMutex.withLock {
            memoryCache[key]?.let { return it }
        }

        val perSectionLimit = ceil(limit / 3.0).toInt().coerceAtLeast(1)
        val live = loadSectionPage(Section.LIVE, page, perSectionLimit, authConfig)
        val movies = loadSectionPage(Section.MOVIES, page, perSectionLimit, authConfig)
        val series = loadSectionPage(Section.SERIES, page, perSectionLimit, authConfig)

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
        val live = searchSectionPage(Section.LIVE, query, page, perSectionLimit, authConfig)
        val movies = searchSectionPage(Section.MOVIES, query, page, perSectionLimit, authConfig)
        val series = searchSectionPage(Section.SERIES, query, page, perSectionLimit, authConfig)

        val mixed = interleaveLists(listOf(live.items, movies.items, series.items), maxItems = limit)
        val endReached = live.endReached && movies.endReached && series.endReached

        val pageData = ContentPage(items = mixed, endReached = endReached)
        memoryCacheMutex.withLock { memoryCache[key] = pageData }
        return pageData
    }

    private suspend fun searchFilterPages(
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
            // Early termination: if requesting page 2+ but first page has no matches, stop
            if (rawPage == 0 && page > 0 && matchIndex == 0) {
                endReached = true
                break
            }
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
        val items = if (section == Section.ALL) {
            val live = loadSectionIndex(Section.LIVE, authConfig).orEmpty()
            val movies = loadSectionIndex(Section.MOVIES, authConfig).orEmpty()
            val series = loadSectionIndex(Section.SERIES, authConfig).orEmpty()
            if (live.isEmpty() && movies.isEmpty() && series.isEmpty()) {
                emptyList()
            } else {
                live + movies + series
            }
        } else {
            loadSectionIndex(section, authConfig) ?: return null
        }
        if (items.isEmpty()) {
            return null
        }
        return withContext(Dispatchers.Default) {
            val filtered = items.filter { item ->
                SearchNormalizer.matchesTitle(item.title, normalizedQuery)
            }
            val start = page * limit
            val slice = if (start >= filtered.size) {
                emptyList()
            } else {
                filtered.subList(start, (start + limit).coerceAtMost(filtered.size))
            }
            val endReached = start + limit >= filtered.size
            ContentPage(items = slice, endReached = endReached)
        }
    }

    private suspend fun loadSectionIndex(
        section: Section,
        authConfig: AuthConfig
    ): List<ContentItem>? {
        val key = indexKey(section, authConfig)
        sectionIndexCache[key]?.let { return it }
        val cached = contentCache.readSectionIndex(section, authConfig)
        if (cached != null) {
            sectionIndexCache[key] = cached
            withContext(Dispatchers.Default) {
                SearchNormalizer.preWarmCache(cached.map { it.title })
            }
        }
        return cached
    }

    suspend fun hasSectionIndex(section: Section, authConfig: AuthConfig): Boolean {
        return contentCache.hasSectionIndex(section, authConfig)
    }

    suspend fun hasSearchIndex(authConfig: AuthConfig): Boolean {
        val sections = listOf(Section.MOVIES, Section.SERIES, Section.LIVE)
        return sections.all { section ->
            contentCache.readSectionSyncCheckpoint(section, authConfig)?.isComplete == true &&
                contentCache.hasSectionIndex(section, authConfig)
        }
    }

    suspend fun hasAnySearchIndex(authConfig: AuthConfig): Boolean {
        return contentCache.hasSectionIndex(Section.SERIES, authConfig) &&
            contentCache.hasSectionIndex(Section.MOVIES, authConfig) &&
            contentCache.hasSectionIndex(Section.LIVE, authConfig)
    }

    /**
     * Sync a single section's search index
     */
    suspend fun syncSectionIndex(
        section: Section,
        authConfig: AuthConfig,
        force: Boolean = false,
        onProgress: (LibrarySyncProgress) -> Unit = {}
    ) {
        syncSearchIndex(authConfig, force, listOf(section), onProgress)
    }

    /**
     * Sync search index for specified sections (or all if not specified)
     */
    suspend fun syncSearchIndex(
        authConfig: AuthConfig,
        force: Boolean = false,
        sectionsToSync: List<Section>? = null,
        onProgress: (LibrarySyncProgress) -> Unit = {}
    ) {
        val sections = sectionsToSync ?: listOf(Section.SERIES, Section.MOVIES, Section.LIVE)
        val totalSections = sections.size

        // Check which sections need syncing
        data class SectionState(
            val section: Section,
            val index: Int,
            val needsSync: Boolean,
            val cachedItems: List<ContentItem>?
        )

        val sectionStates = sections.mapIndexed { index, section ->
            val key = indexKey(section, authConfig)
            val cached = sectionIndexCache[key]
            val hasDisk = contentCache.hasSectionIndex(section, authConfig)
            val needsSync = force || (cached == null && !hasDisk)
            val cachedItems = if (!needsSync) {
                cached ?: loadSectionIndex(section, authConfig).orEmpty()
            } else null
            SectionState(section, index, needsSync, cachedItems)
        }

        // Report progress for already-cached sections
        var completedSections = 0
        sectionStates.filter { !it.needsSync }.forEach { state ->
            completedSections++
            onProgress(
                LibrarySyncProgress(
                    section = state.section,
                    sectionIndex = state.index,
                    totalSections = totalSections,
                    itemsIndexed = state.cachedItems?.size ?: 0,
                    progress = completedSections.toFloat() / totalSections
                )
            )
        }

        // Fetch sections that need syncing in parallel
        val sectionsToSync = sectionStates.filter { it.needsSync }
        if (sectionsToSync.isEmpty()) return

        val useBulk = force || sectionStates.all { it.needsSync }
        val completedCounter = AtomicInteger(completedSections)
        val totalItemsCounter = AtomicInteger(0)

        coroutineScope {
            sectionsToSync.map { state ->
                async {
                    val section = state.section
                    val key = indexKey(section, authConfig)

                    val items = if (useBulk) {
                        val bulkResult = api.fetchSectionAll(section, authConfig)
                        if (bulkResult.isSuccess) {
                            val fetched = bulkResult.getOrNull().orEmpty()
                            if (fetched.isEmpty()) {
                                timber.log.Timber.w("Bulk fetch returned empty for $section, falling back to page-by-page")
                                null
                            } else {
                                fetched
                            }
                        } else {
                            timber.log.Timber.w("Bulk fetch failed for $section: ${bulkResult.exceptionOrNull()?.message}, falling back to page-by-page")
                            null
                        }
                    } else null

                    // Fall back to page-by-page if bulk didn't work
                    val finalItems = items ?: run {
                        timber.log.Timber.d("Starting page-by-page fetch for $section")
                        buildSectionIndex(section, authConfig)
                    }

                    // Cache results
                    // Skip in-memory cache for huge indexes (>50k items) to avoid OOM
                    if (finalItems.size < 50000) {
                        sectionIndexCache[key] = finalItems
                    } else {
                        timber.log.Timber.d("Skipping memory cache for large section $section (${finalItems.size} items)")
                    }
                    contentCache.writeSectionIndex(section, authConfig, finalItems)
                    withContext(Dispatchers.Default) {
                        SearchNormalizer.preWarmCache(finalItems.map { it.title })
                    }

                    // Report progress as this section completes
                    val completed = completedCounter.incrementAndGet()
                    val totalItems = totalItemsCounter.addAndGet(finalItems.size)
                    onProgress(
                        LibrarySyncProgress(
                            section = section,
                            sectionIndex = state.index,
                            totalSections = totalSections,
                            itemsIndexed = totalItems,
                            progress = completed.toFloat() / totalSections
                        )
                    )
                }
            }.awaitAll()
        }
    }

    private suspend fun buildSectionIndex(
        section: Section,
        authConfig: AuthConfig,
        onProgress: (Int) -> Unit = {}
    ): List<ContentItem> {
        val items = ArrayList<ContentItem>()
        var pageSize = indexPageSize(section)
        var offset = 0
        var endReached = false
        while (!endReached) {
            val pageIndex = offset / pageSize
            val result = api.fetchSectionPage(section, authConfig, pageIndex, pageSize)
            if (result.isFailure && pageSize > MIN_INDEX_PAGE_SIZE) {
                pageSize = MIN_INDEX_PAGE_SIZE
                continue
            }
            val pageData = result.getOrElse { throw it }
            if (pageData.items.isEmpty()) {
                endReached = true
            } else {
                items.addAll(pageData.items)
                endReached = pageData.endReached
                offset += pageSize
                onProgress(items.size)
            }
        }
        return items
    }

    /**
     * Fast start sync: Fetch first 2 pages of each section for immediate search capability
     * Target: quick partial index for immediate search usability
     */
    suspend fun syncFastStartIndex(
        authConfig: AuthConfig,
        onProgress: (LibrarySyncProgress) -> Unit = {}
    ): Result<FastStartResult> {
        val sections = listOf(Section.MOVIES, Section.SERIES, Section.LIVE)

        return coroutineScope {
            val results = sections.mapIndexed { index, section ->
                async {
                    val items = mutableListOf<ContentItem>()
                    for (page in 0 until FAST_START_PAGES) {
                        val pageData = api.fetchSectionPage(
                            section,
                            authConfig,
                            page,
                            FAST_START_PAGE_SIZE
                        )
                            .getOrElse {
                                Timber.w("Fast start failed for $section page $page")
                                return@async emptyList()
                            }
                        items.addAll(pageData.items)

                        onProgress(LibrarySyncProgress(
                            section = section,
                            sectionIndex = index,
                            totalSections = sections.size,
                            itemsIndexed = items.size,
                            progress = ((page + 1) / FAST_START_PAGES.toFloat()),
                            phase = com.example.xtreamplayer.content.SyncPhase.FAST_START
                        ))
                    }

                    // Write partial index to disk
                    contentCache.writeSectionIndexPartial(
                        section,
                        authConfig,
                        items,
                        FAST_START_PAGES - 1,
                        false
                    )
                    Timber.d("Fast start: $section indexed ${items.size} items")

                    items
                }
            }.awaitAll()

            val totalItems = results.sumOf { it.size }
            Timber.i("Fast start complete: $totalItems items indexed")

            Result.success(FastStartResult(itemsIndexed = totalItems, ready = true))
        }
    }

    /**
     * Background full sync: Complete library index with throttling to avoid blocking UI
     * Uses small pages (200 items) with 500ms delays
     */
    suspend fun syncBackgroundFull(
        authConfig: AuthConfig,
        sectionsToSync: List<Section> = listOf(Section.SERIES, Section.MOVIES, Section.LIVE),
        onProgress: (LibrarySyncProgress) -> Unit = {},
        checkPause: suspend () -> Boolean = { false },
        skipCompleted: Boolean = true,
        pageSize: Int = BACKGROUND_PAGE_SIZE,
        throttleMs: Long = BACKGROUND_THROTTLE_MS,
        useBulkFirst: Boolean = false,
        fallbackPageSize: Int = pageSize,
        fullReindex: Boolean = false,
        useStaging: Boolean = false
    ): Result<Unit> {
        for ((sectionIndex, section) in sectionsToSync.withIndex()) {
            val stagingMode = fullReindex || useStaging
            val hasStaging = if (stagingMode) {
                contentCache.hasSectionIndexStaging(section, authConfig)
            } else {
                false
            }
            val checkpoint = if (stagingMode) {
                if (hasStaging) {
                    contentCache.readSectionSyncCheckpoint(section, authConfig)
                } else {
                    null
                }
            } else {
                contentCache.readSectionSyncCheckpoint(section, authConfig)
            }

            if (!stagingMode && skipCompleted && checkpoint?.isComplete == true) {
                Timber.d("Background sync: $section already complete, skipping")
                continue
            }

            val startPage =
                if (stagingMode) {
                    checkpoint?.lastPageSynced?.plus(1) ?: 0
                } else {
                    checkpoint?.lastPageSynced?.plus(1) ?: FAST_START_PAGES
                }

            val allItems =
                if (stagingMode && hasStaging) {
                    contentCache.readSectionIndexStaging(section, authConfig)?.toMutableList()
                        ?: mutableListOf()
                } else if (stagingMode) {
                    if (fullReindex) {
                        contentCache.clearSectionSyncCheckpoint(section, authConfig)
                    }
                    mutableListOf()
                } else {
                    loadSectionIndex(section, authConfig)?.toMutableList() ?: mutableListOf()
                }

            if (useBulkFirst) {
                val bulkResult = api.fetchSectionAll(section, authConfig)
                if (bulkResult.isSuccess) {
                    val bulkItems = bulkResult.getOrNull().orEmpty()
                    if (bulkItems.isNotEmpty()) {
                        if (stagingMode) {
                            contentCache.writeSectionIndexStaging(section, authConfig, bulkItems)
                            contentCache.writeSectionSyncCheckpoint(
                                section,
                                authConfig,
                                lastPage = 0,
                                itemsIndexed = bulkItems.size,
                                isComplete = true
                            )
                            contentCache.commitSectionIndexStaging(section, authConfig)
                        } else {
                            contentCache.writeSectionIndex(section, authConfig, bulkItems)
                            contentCache.writeSectionSyncCheckpoint(
                                section,
                                authConfig,
                                lastPage = 0,
                                itemsIndexed = bulkItems.size,
                                isComplete = true
                            )
                        }
                        val key = indexKey(section, authConfig)
                        if (bulkItems.size < 50000) {
                            sectionIndexCache[key] = bulkItems
                        } else {
                            sectionIndexCache.remove(key)
                        }
                        onProgress(
                            LibrarySyncProgress(
                                section = section,
                                sectionIndex = sectionIndex,
                                totalSections = sectionsToSync.size,
                                itemsIndexed = bulkItems.size,
                                progress = 1f,
                                phase = com.example.xtreamplayer.content.SyncPhase.BACKGROUND_FULL
                            )
                        )
                        Timber.i("Background sync (bulk) complete: $section with ${bulkItems.size} items")
                        if (checkPause()) {
                            Timber.i("Background sync paused after bulk: $section")
                            return Result.success(Unit)
                        }
                        continue
                    }
                }
                Timber.w("Background sync: bulk fetch failed for $section, falling back to page-by-page")
            }

            var page = startPage
            Timber.d("Background sync: $section starting at page $page (checkpoint: ${checkpoint?.itemsIndexed} items)")

            while (true) {
                // Check pause on every page for responsive pausing
                if (checkPause()) {
                    contentCache.writeSectionSyncCheckpoint(section, authConfig, page - 1, allItems.size, false)
                    Timber.i("Background sync paused: $section at page $page")
                    return Result.success(Unit)
                }

                val effectivePageSize = if (useBulkFirst) fallbackPageSize else pageSize
                val pageDataResult =
                        api.fetchSectionPage(section, authConfig, page, effectivePageSize)
                if (pageDataResult.isFailure) {
                    Timber.w("Background sync: $section page $page failed: ${pageDataResult.exceptionOrNull()}")
                    contentCache.writeSectionSyncCheckpoint(section, authConfig, page - 1, allItems.size, false)
                    return Result.failure(pageDataResult.exceptionOrNull() ?: IllegalStateException("Background sync failed"))
                }

                val pageData = pageDataResult.getOrThrow()

                if (pageData.items.isNotEmpty()) {
                    val existingIds = allItems.asSequence().map { it.id }.toHashSet()
                    pageData.items.forEach { item ->
                        if (existingIds.add(item.id)) {
                            allItems.add(item)
                        }
                    }
                }

                // Incremental save every 10 pages
                if (page % BACKGROUND_SAVE_INTERVAL == 0) {
                    if (stagingMode) {
                        contentCache.updateSectionIndexIncrementalStaging(section, authConfig, allItems)
                    } else {
                        contentCache.updateSectionIndexIncremental(section, authConfig, allItems)
                        val cacheKey = indexKey(section, authConfig)
                        if (allItems.size < 50000) {
                            sectionIndexCache[cacheKey] = allItems
                        } else {
                            sectionIndexCache.remove(cacheKey)
                        }
                    }
                    contentCache.writeSectionSyncCheckpoint(section, authConfig, page, allItems.size, false)
                }

                onProgress(LibrarySyncProgress(
                    section = section,
                    sectionIndex = sectionIndex,
                    totalSections = sectionsToSync.size,
                    itemsIndexed = allItems.size,
                    progress = 0.5f, // Indeterminate for background
                    phase = com.example.xtreamplayer.content.SyncPhase.BACKGROUND_FULL
                ))

                if (pageData.items.isEmpty() || pageData.endReached) {
                    // Section complete
                    if (stagingMode) {
                        contentCache.writeSectionIndexStaging(section, authConfig, allItems)
                        contentCache.commitSectionIndexStaging(section, authConfig)
                    } else {
                        contentCache.writeSectionIndex(section, authConfig, allItems)
                    }
                    contentCache.writeSectionSyncCheckpoint(section, authConfig, page, allItems.size, true)
                    val key = indexKey(section, authConfig)
                    if (allItems.size < 50000) {
                        sectionIndexCache[key] = allItems
                    } else {
                        sectionIndexCache.remove(key)
                    }
                    Timber.i("Background sync complete: $section with ${allItems.size} items")
                    break
                }

                delay(throttleMs) // Throttle to avoid blocking UI
                page++
            }
        }

        Timber.i("Background sync: All sections complete")
        return Result.success(Unit)
    }

    /**
     * On-demand boost: Fetch next 3 pages for specific section when user enters it
     * Uses parallel fetching for speed (2-3 seconds target)
     */
    suspend fun boostSectionSync(
        section: Section,
        authConfig: AuthConfig,
        targetPages: Int = 3,
        onProgress: (LibrarySyncProgress) -> Unit = {}
    ): Result<Unit> {
        val checkpoint = contentCache.readSectionSyncCheckpoint(section, authConfig)
        val startPage = checkpoint?.lastPageSynced?.plus(1) ?: 0

        val existingItems = loadSectionIndex(section, authConfig)?.toMutableList() ?: mutableListOf()

        Timber.d("On-demand boost: $section fetching pages $startPage-${startPage + targetPages - 1}")

        // Fetch 3 pages in parallel for speed
        val newItems = coroutineScope {
            (startPage until startPage + targetPages).map { page ->
                async {
                    api.fetchSectionPage(section, authConfig, page, BOOST_PAGE_SIZE)
                        .getOrNull()?.items ?: emptyList()
                }
            }.awaitAll().flatten()
        }

        if (newItems.isNotEmpty()) {
            val existingIds = existingItems.asSequence().map { it.id }.toHashSet()
            newItems.forEach { item ->
                if (existingIds.add(item.id)) {
                    existingItems.add(item)
                }
            }
        }

        // Update index immediately
        contentCache.updateSectionIndexIncremental(section, authConfig, existingItems)
        contentCache.writeSectionSyncCheckpoint(section, authConfig, startPage + targetPages - 1, existingItems.size, false)

        Timber.i("On-demand boost: $section added ${newItems.size} items (total: ${existingItems.size})")

        onProgress(LibrarySyncProgress(
            section = section,
            sectionIndex = 0,
            totalSections = 1,
            itemsIndexed = existingItems.size,
            progress = 1.0f,
            phase = com.example.xtreamplayer.content.SyncPhase.ON_DEMAND_BOOST
        ))

        return Result.success(Unit)
    }

    /**
     * Get sync checkpoint for a section
     */
    suspend fun getSectionSyncCheckpoint(
        section: Section,
        config: AuthConfig
    ): SectionSyncCheckpoint? {
        return contentCache.readSectionSyncCheckpoint(section, config)
    }

    suspend fun hasFullIndex(authConfig: AuthConfig): Boolean {
        val sections = listOf(Section.MOVIES, Section.SERIES, Section.LIVE)
        return sections.all { section ->
            contentCache.readSectionSyncCheckpoint(section, authConfig)?.isComplete == true
        }
    }

    private suspend fun isSearchIndexComplete(
        section: Section,
        authConfig: AuthConfig
    ): Boolean {
        return if (section == Section.ALL) {
            val sections = listOf(Section.MOVIES, Section.SERIES, Section.LIVE)
            sections.all { contentCache.readSectionSyncCheckpoint(it, authConfig)?.isComplete == true }
        } else {
            contentCache.readSectionSyncCheckpoint(section, authConfig)?.isComplete == true
        }
    }

    private fun indexPageSize(section: Section): Int {
        return when (section) {
            Section.SERIES -> 1000
            Section.MOVIES -> 800
            Section.LIVE -> 800
            else -> MIN_INDEX_PAGE_SIZE
        }
    }

    private fun sectionProgress(pagesLoaded: Int): Float {
        val raw = 1f - (1f / (pagesLoaded + 1).toFloat())
        return raw.coerceIn(0.05f, 0.95f)
    }

    private fun accountKey(authConfig: AuthConfig): String {
        return "${authConfig.baseUrl}|${authConfig.username}|${authConfig.listName}"
    }

    private fun indexKey(section: Section, authConfig: AuthConfig): String {
        return "${section.name}|${authConfig.baseUrl}|${authConfig.username}|${authConfig.listName}"
    }

    private fun seasonCountKey(seriesId: String, authConfig: AuthConfig): String {
        return "seasons|${authConfig.baseUrl}|${authConfig.username}|${authConfig.listName}|$seriesId"
    }

    suspend fun clearCache() {
        memoryCacheMutex.withLock { memoryCache.clear() }
        categoryLock.withLock { categoryCache.clear() }
        categoryThumbnailMutex.withLock { categoryThumbnailCache.clear() }
        sectionIndexCache.clear()
        seriesEpisodesMutex.withLock { seriesEpisodesCache.clear() }
        seriesSeasonCountMutex.withLock { seriesSeasonCountCache.clear() }
    }

    suspend fun clearDiskCache() {
        contentCache.clearAll()
    }

    suspend fun clearDiskCacheFor(authConfig: AuthConfig) {
        contentCache.clearFor(authConfig)
    }

    suspend fun hasCacheFor(authConfig: AuthConfig): Boolean {
        return contentCache.hasCacheFor(authConfig) || contentCache.hasRefreshMarker(authConfig)
    }

    suspend fun refreshContent(authConfig: AuthConfig) {
        clearCache()
        contentCache.clearFor(authConfig)
        contentCache.writeRefreshMarker(authConfig)
    }

    suspend fun categoryThumbnail(
        type: ContentType,
        categoryId: String,
        authConfig: AuthConfig
    ): String? {
        val key = "${accountKey(authConfig)}-${type.name}-$categoryId"
        categoryThumbnailMutex.withLock {
            categoryThumbnailCache[key]?.let { return it }
        }
        val cached = contentCache.readCategoryThumbnail(type, categoryId, authConfig)
        if (cached != null) {
            categoryThumbnailMutex.withLock {
                categoryThumbnailCache[key] = cached
            }
            return cached
        }
        val page = runCatching {
            loadCategoryPage(type, categoryId, 0, 1, authConfig)
        }.getOrNull()
        val imageUrl = page?.items?.firstOrNull()?.imageUrl
        contentCache.writeCategoryThumbnail(type, categoryId, authConfig, imageUrl)
        categoryThumbnailMutex.withLock {
            categoryThumbnailCache[key] = imageUrl
        }
        return imageUrl
    }

    private fun cacheKey(sectionKey: String, page: Int, limit: Int): String {
        return "$sectionKey-$page-$limit"
    }

    private fun interleaveLists(lists: List<List<ContentItem>>, maxItems: Int = Int.MAX_VALUE): List<ContentItem> {
        val totalSize = lists.sumOf { it.size }
        val result = ArrayList<ContentItem>(totalSize.coerceAtMost(maxItems))
        val max = lists.maxOfOrNull { it.size } ?: 0
        for (index in 0 until max) {
            for (list in lists) {
                if (index < list.size) {
                    result.add(list[index])
                    if (result.size >= maxItems) return result
                }
            }
        }
        return result
    }
}

/**
 * Result of fast start sync operation
 */
data class FastStartResult(
    /** Total number of items indexed during fast start */
    val itemsIndexed: Int,

    /** True if fast start completed successfully and search is ready */
    val ready: Boolean
)

package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import java.util.LinkedHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class SearchIndexRepository(
    private val contentCache: ContentCache
) {
    private data class TransientSectionIndexEntry(
        val items: List<ContentItem>,
        val cachedAtMs: Long
    )

    private data class SearchIndexReadinessEntry(
        val checkpointTimestamp: Long,
        val ready: Boolean,
        val cachedAtMs: Long
    )

    private data class ActiveLargeSearchIndexEntry(
        val key: String,
        val items: List<ContentItem>,
        val cachedAtMs: Long
    )

    private companion object {
        const val MAX_SECTION_INDEX_CACHE_KEYS = 4
        const val MAX_TRANSIENT_SEARCH_INDEX_CACHE_KEYS = 3
        const val MAX_TRANSIENT_SEARCH_INDEX_ITEMS_IN_MEMORY = 75_000
        const val TRANSIENT_SEARCH_INDEX_TTL_MS = 15_000L
        const val ACTIVE_LARGE_SEARCH_INDEX_TTL_MS = 20_000L
        const val SEARCH_READINESS_CACHE_TTL_MS = 10_000L
        const val MAX_SEARCH_READINESS_CACHE_KEYS = 12
        const val MAX_PREWARM_TITLES_PER_SECTION = 5_000
    }

    private val sectionIndexCache =
        object : LinkedHashMap<String, List<ContentItem>>(MAX_SECTION_INDEX_CACHE_KEYS, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ContentItem>>): Boolean {
                return size > MAX_SECTION_INDEX_CACHE_KEYS
            }
        }
    private val transientSearchIndexCache =
        object : LinkedHashMap<String, TransientSectionIndexEntry>(
            MAX_TRANSIENT_SEARCH_INDEX_CACHE_KEYS,
            0.75f,
            true
        ) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, TransientSectionIndexEntry>
            ): Boolean {
                return size > MAX_TRANSIENT_SEARCH_INDEX_CACHE_KEYS
            }
        }
    private var activeLargeSearchIndex: ActiveLargeSearchIndexEntry? = null
    private val searchReadinessCache =
        object : LinkedHashMap<String, SearchIndexReadinessEntry>(
            MAX_SEARCH_READINESS_CACHE_KEYS,
            0.75f,
            true
        ) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, SearchIndexReadinessEntry>
            ): Boolean {
                return size > MAX_SEARCH_READINESS_CACHE_KEYS
            }
        }
    private val sectionIndexMutex = Mutex()
    private val transientSearchIndexMutex = Mutex()
    private val activeLargeSearchIndexMutex = Mutex()
    private val searchReadinessMutex = Mutex()

    suspend fun loadSectionIndex(
        section: Section,
        authConfig: AuthConfig
    ): List<ContentItem>? {
        val key = indexKey(section, authConfig)

        sectionIndexMutex.withLock {
            sectionIndexCache[key]?.let {
                Timber.d("Search index hit in-memory section=$section size=${it.size}")
                return it
            }
        }
        transientSearchIndexMutex.withLock {
            val transient = transientSearchIndexCache[key]
            if (transient != null) {
                val ageMs = System.currentTimeMillis() - transient.cachedAtMs
                if (ageMs <= TRANSIENT_SEARCH_INDEX_TTL_MS) {
                    Timber.d("Search index hit transient section=$section size=${transient.items.size} ageMs=$ageMs")
                    return transient.items
                }
                transientSearchIndexCache.remove(key)
            }
        }
        activeLargeSearchIndexMutex.withLock {
            val active = activeLargeSearchIndex
            if (active != null && active.key == key) {
                val ageMs = System.currentTimeMillis() - active.cachedAtMs
                if (ageMs <= ACTIVE_LARGE_SEARCH_INDEX_TTL_MS) {
                    Timber.d("Search index hit active-large section=$section size=${active.items.size} ageMs=$ageMs")
                    return active.items
                }
                activeLargeSearchIndex = null
            }
        }

        val cached = contentCache.readSectionIndex(section, authConfig)
        if (cached != null) {
            val checkpoint = contentCache.readSectionSyncCheckpoint(section, authConfig)
            if (checkpoint != null) {
                val ageMs = System.currentTimeMillis() - checkpoint.timestamp
                val ageDays = ageMs / (24 * 60 * 60 * 1000L)
                if (ageDays > 7) {
                    Timber.i("Section $section cache is ${ageDays}d old, marking for resync")
                    transientSearchIndexMutex.withLock { transientSearchIndexCache.remove(key) }
                    activeLargeSearchIndexMutex.withLock {
                        if (activeLargeSearchIndex?.key == key) {
                            activeLargeSearchIndex = null
                        }
                    }
                    return null
                }
            }

            storeSectionIndexInMemory(section, authConfig, cached)
            Timber.d("Search index loaded from disk section=$section size=${cached.size}")
            preWarmSearchTitles(cached)
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

    suspend fun cacheSectionIndex(
        section: Section,
        authConfig: AuthConfig,
        items: List<ContentItem>
    ) {
        storeSectionIndexInMemory(section, authConfig, items)
        contentCache.writeSectionIndex(section, authConfig, items)
        preWarmSearchTitles(items)
    }

    suspend fun storeSectionIndexInMemory(
        section: Section,
        authConfig: AuthConfig,
        items: List<ContentItem>
    ) {
        val key = indexKey(section, authConfig)
        sectionIndexMutex.withLock {
            if (shouldKeepSectionIndexInMemory(items.size)) {
                sectionIndexCache[key] = items
            } else {
                sectionIndexCache.remove(key)
            }
        }
        transientSearchIndexMutex.withLock {
            if (shouldKeepTransientSectionIndexInMemory(items.size)) {
                transientSearchIndexCache[key] =
                    TransientSectionIndexEntry(
                        items = items,
                        cachedAtMs = System.currentTimeMillis()
                    )
            } else {
                transientSearchIndexCache.remove(key)
            }
        }
        activeLargeSearchIndexMutex.withLock {
            if (items.size > MAX_TRANSIENT_SEARCH_INDEX_ITEMS_IN_MEMORY) {
                activeLargeSearchIndex =
                    ActiveLargeSearchIndexEntry(
                        key = key,
                        items = items,
                        cachedAtMs = System.currentTimeMillis()
                    )
            } else if (activeLargeSearchIndex?.key == key) {
                activeLargeSearchIndex = null
            }
        }
    }

    private suspend fun preWarmSearchTitles(items: List<ContentItem>) {
        if (items.isEmpty()) return
        val titleSample =
            if (items.size > MAX_PREWARM_TITLES_PER_SECTION) {
                items.subList(0, MAX_PREWARM_TITLES_PER_SECTION)
            } else {
                items
            }
        withContext(Dispatchers.Default) {
            SearchNormalizer.preWarmCache(titleSample.map { it.title })
        }
    }

    suspend fun isSearchIndexReadyForQuery(
        section: Section,
        authConfig: AuthConfig
    ): Boolean {
        return if (section == Section.ALL) {
            val sections = listOf(Section.MOVIES, Section.SERIES, Section.LIVE)
            sections.all { isSectionSearchReadyForQuery(it, authConfig) }
        } else {
            isSectionSearchReadyForQuery(section, authConfig)
        }
    }

    private suspend fun isSectionSearchReadyForQuery(
        section: Section,
        authConfig: AuthConfig
    ): Boolean {
        val checkpoint = contentCache.readSectionSyncCheckpoint(section, authConfig) ?: return false
        if (!checkpoint.isComplete) return false

        val now = System.currentTimeMillis()
        val ageDays = (now - checkpoint.timestamp) / (24 * 60 * 60 * 1000L)
        if (ageDays > 7) {
            return false
        }

        val readinessKey = indexKey(section, authConfig)
        val cached = searchReadinessMutex.withLock { searchReadinessCache[readinessKey] }
        if (
            cached != null &&
            cached.checkpointTimestamp == checkpoint.timestamp &&
            now - cached.cachedAtMs <= SEARCH_READINESS_CACHE_TTL_MS
        ) {
            return cached.ready
        }

        val ready = contentCache.hasSectionIndex(section, authConfig)
        searchReadinessMutex.withLock {
            searchReadinessCache[readinessKey] =
                SearchIndexReadinessEntry(
                    checkpointTimestamp = checkpoint.timestamp,
                    ready = ready,
                    cachedAtMs = now
                )
        }
        return ready
    }

    suspend fun clearCache() {
        sectionIndexMutex.withLock { sectionIndexCache.clear() }
        transientSearchIndexMutex.withLock { transientSearchIndexCache.clear() }
        activeLargeSearchIndexMutex.withLock { activeLargeSearchIndex = null }
        searchReadinessMutex.withLock { searchReadinessCache.clear() }
        SearchNormalizer.clearCache()
    }
}

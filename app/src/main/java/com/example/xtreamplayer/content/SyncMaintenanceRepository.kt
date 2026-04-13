package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class SyncMaintenanceRepository(
    private val contentCache: ContentCache
) {
    // Cache checkpoint validation results to avoid repeated file reads during search paging.
    private data class ValidationCacheEntry(val isValid: Boolean, val cachedAt: Long)

    private val validationCache = object : LinkedHashMap<String, ValidationCacheEntry>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ValidationCacheEntry>): Boolean {
            return size > 50
        }
    }
    private val validationCacheMutex = Mutex()

    suspend fun getSectionSyncCheckpoint(
        section: Section,
        config: AuthConfig
    ): SectionSyncCheckpoint? {
        return contentCache.readSectionSyncCheckpoint(section, config)
    }

    suspend fun hasFullIndex(authConfig: AuthConfig): Boolean {
        val sections = listOf(Section.MOVIES, Section.SERIES, Section.LIVE)
        return sections.all { section ->
            validateCheckpoint(section, authConfig)
        }
    }

    suspend fun clearCache() {
        validationCacheMutex.withLock { validationCache.clear() }
    }

    /**
     * Validate checkpoint against actual cached data.
     * Checks staleness, item count accuracy, and isComplete validity.
     */
    private suspend fun validateCheckpoint(
        section: Section,
        authConfig: AuthConfig
    ): Boolean {
        val checkpoint = contentCache.readSectionSyncCheckpoint(section, authConfig) ?: return false
        if (!checkpoint.isComplete) return false

        val cacheKey = "${section.name}|${authConfig.baseUrl}|${authConfig.username}|${checkpoint.timestamp}"
        val cachedValidation = validationCacheMutex.withLock { validationCache[cacheKey] }
        if (cachedValidation != null) {
            val cacheAge = System.currentTimeMillis() - cachedValidation.cachedAt
            if (cacheAge < 120_000) {
                return cachedValidation.isValid
            }
        }

        val isValid = performFullValidation(section, authConfig, checkpoint)
        validationCacheMutex.withLock {
            validationCache[cacheKey] = ValidationCacheEntry(isValid, System.currentTimeMillis())
        }
        return isValid
    }

    private suspend fun performFullValidation(
        section: Section,
        authConfig: AuthConfig,
        checkpoint: SectionSyncCheckpoint
    ): Boolean {
        val ageMs = System.currentTimeMillis() - checkpoint.timestamp
        val staleDays = ageMs / (24 * 60 * 60 * 1000L)
        if (staleDays > 7) {
            Timber.w("Checkpoint for $section is stale (${staleDays}d old)")
            contentCache.writeSectionSyncCheckpoint(
                section, authConfig,
                lastPage = 0,
                itemsIndexed = 0,
                isComplete = false
            )
            return false
        }

        val cachedItems = contentCache.readSectionIndex(section, authConfig)
        if (cachedItems == null) {
            Timber.w("Checkpoint claims complete for $section but no index file exists")
            contentCache.writeSectionSyncCheckpoint(
                section, authConfig,
                lastPage = 0,
                itemsIndexed = 0,
                isComplete = false
            )
            return false
        }

        val minExpected = (checkpoint.itemsIndexed * 0.9).toInt()
        if (cachedItems.size < minExpected) {
            Timber.w("Checkpoint mismatch for $section: claims ${checkpoint.itemsIndexed} but index has ${cachedItems.size}")
            contentCache.writeSectionSyncCheckpoint(
                section, authConfig,
                lastPage = 0,
                itemsIndexed = 0,
                isComplete = false
            )
            return false
        }

        return true
    }
}

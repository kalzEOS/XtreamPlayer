package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Coordinates progressive library sync with three modes:
 * 1. Fast Start - Quick 2-page sync for immediate search
 * 2. Background Full - Complete library index with throttling
 * 3. On-Demand Boost - Priority sync for specific section
 */
class ProgressiveSyncCoordinator(
    private val contentRepository: ContentRepository,
    private val settingsRepository: SettingsRepository,
    private val authConfig: AuthConfig
) {
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Default)

    private val _syncState = MutableStateFlow(ProgressiveSyncState())
    val syncState: StateFlow<ProgressiveSyncState> = _syncState.asStateFlow()

    private var fastStartJob: Job? = null
    private var backgroundSyncJob: Job? = null
    private val onDemandJobs = mutableMapOf<Section, Job>()

    private val syncMutex = Mutex()
    private val activeSyncSections = mutableSetOf<Section>()
    private val activeSyncMutex = Mutex()
    @Volatile private var disposed = false

    /**
     * Start fast start sync: 2 pages per section for immediate search capability
     */
    suspend fun startFastStartSync() {
        syncMutex.withLock {
            // Cancel existing fast start if any
            fastStartJob?.cancel()

            // Check if already complete
            if (_syncState.value.fastStartReady) {
                Timber.d("Fast start already complete, skipping")
                return
            }

            // Update state
            updateSyncState {
                it.copy(
                    phase = SyncPhase.FAST_START,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            }

            // Save state
            settingsRepository.saveSyncState(_syncState.value, accountKey())

            fastStartJob = scope.launch {
                Timber.i("Starting fast start sync")

                val result = contentRepository.syncFastStartIndex(authConfig) { progress ->
                    updateSectionProgress(progress.section, SectionSyncProgress(
                        itemsIndexed = progress.itemsIndexed,
                        lastPageSynced = 1,
                        phase = SyncPhase.FAST_START,
                        progress = progress.progress
                    ))
                }

                result.onSuccess { fastStartResult ->
                    Timber.i("Fast start complete: ${fastStartResult.itemsIndexed} items")

                    updateSyncState {
                        it.copy(
                            phase = SyncPhase.IDLE,
                            fastStartReady = true,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }

                    settingsRepository.saveSyncState(_syncState.value, accountKey())
                }.onFailure { error ->
                    Timber.e(error, "Fast start failed")

                    updateSyncState { it.copy(phase = SyncPhase.IDLE) }
                }
            }
        }
    }

    /**
     * Start background full sync: complete library index with throttling
     */
    suspend fun startBackgroundFullSync() {
        startBackgroundFullSync(force = false, throttleMs = null, fullReindex = false)
    }

    suspend fun startBackgroundFullSync(
        force: Boolean,
        throttleMs: Long?,
        fullReindex: Boolean
    ) {
        syncMutex.withLock {
            // Cancel existing background sync if any
            backgroundSyncJob?.cancel()

            // Check if already complete
            if (_syncState.value.fullIndexComplete && !force && !fullReindex) {
                Timber.d("Background sync already complete, skipping")
                return
            }

            // Check if paused
            if (_syncState.value.isPaused) {
                Timber.d("Background sync is paused, not starting")
                return
            }

            // Update state
            updateSyncState {
                it.copy(
                    phase = SyncPhase.BACKGROUND_FULL,
                    fullIndexComplete = if (force || fullReindex) false else it.fullIndexComplete,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            }

            settingsRepository.saveSyncState(_syncState.value, accountKey())

            backgroundSyncJob = scope.launch {
                Timber.i("Starting background full sync")

                val sections = listOf(Section.SERIES, Section.MOVIES, Section.LIVE)

                val result =
                        runCatching {
                            contentRepository.syncBackgroundFull(
                                authConfig = authConfig,
                                sectionsToSync = sections,
                                onProgress = { progress ->
                                    updateSyncState { it.copy(currentSection = progress.section) }

                                    updateSectionProgress(progress.section, SectionSyncProgress(
                                        itemsIndexed = progress.itemsIndexed,
                                        lastPageSynced = 0, // Not tracked during background
                                        phase = SyncPhase.BACKGROUND_FULL,
                                        progress = progress.progress
                                    ))
                                },
                                checkPause = {
                                    _syncState.value.isPaused
                                },
                                skipCompleted = if (fullReindex) false else !force,
                                throttleMs = throttleMs ?: 200L,
                                useBulkFirst = true,
                                fallbackPageSize = 1000,
                                fullReindex = fullReindex,
                                useStaging = fullReindex,
                                onSectionStart = { section ->
                                    activeSyncMutex.withLock {
                                        activeSyncSections.add(section)
                                    }
                                    Timber.d("Background sync: started section $section")
                                },
                                onSectionComplete = { section ->
                                    activeSyncMutex.withLock {
                                        activeSyncSections.remove(section)
                                    }
                                    Timber.d("Background sync: completed section $section")
                                }
                            )
                        }.getOrElse { error ->
                            Result.failure(error)
                        }

                result.onSuccess {
                    if (_syncState.value.isPaused) {
                        Timber.i("Background sync paused")
                        updateSyncState {
                            it.copy(
                                phase = SyncPhase.PAUSED,
                                currentSection = null
                            )
                        }
                        settingsRepository.saveSyncState(_syncState.value, accountKey())
                        return@onSuccess
                    }

                    Timber.i("Background sync complete")

                    updateSyncState {
                        it.copy(
                            phase = SyncPhase.COMPLETE,
                            fullIndexComplete = true,
                            sectionsCompleted = sections.toSet(),
                            currentSection = null,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }

                    settingsRepository.saveSyncState(_syncState.value, accountKey())
                }.onFailure { error ->
                    // Clear active sections on failure
                    scope.launch {
                        activeSyncMutex.withLock { activeSyncSections.clear() }
                    }

                    if (error is CancellationException && _syncState.value.isPaused) {
                        Timber.i("Background sync paused (cancelled)")
                        updateSyncState {
                            it.copy(
                                phase = SyncPhase.PAUSED,
                                currentSection = null
                            )
                        }
                        settingsRepository.saveSyncState(_syncState.value, accountKey())
                    } else if (error is CancellationException) {
                        Timber.i("Background sync cancelled")
                        updateSyncState {
                            it.copy(
                                phase = SyncPhase.IDLE,
                                currentSection = null
                            )
                        }
                    } else {
                        Timber.e(error, "Background sync failed")

                        updateSyncState {
                            it.copy(
                                phase = SyncPhase.IDLE,
                                currentSection = null
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Boost specific section: fetch next 3 pages when user enters section
     */
    suspend fun boostSection(section: Section) {
        syncMutex.withLock {
            // Check if section is already being synced
            val isActive =
                activeSyncMutex.withLock { activeSyncSections.contains(section) }
            if (isActive) {
                Timber.d("Section $section sync already in progress, skipping boost")
                return
            }

            // Cancel existing boost for this section if any
            onDemandJobs[section]?.cancel()

            // Check if section already complete
            if (_syncState.value.sectionsCompleted.contains(section)) {
                Timber.d("Section $section already complete, skipping boost")
                return
            }

            // Mark section as actively syncing
            activeSyncMutex.withLock {
                activeSyncSections.add(section)
            }

            // Create boost job
            onDemandJobs[section] = scope.launch {
                Timber.i("Starting on-demand boost for $section")

                try {

                val result = contentRepository.boostSectionSync(
                    section = section,
                    authConfig = authConfig,
                    onProgress = { progress ->
                        updateSectionProgress(progress.section, SectionSyncProgress(
                            itemsIndexed = progress.itemsIndexed,
                            lastPageSynced = 0,
                            phase = SyncPhase.ON_DEMAND_BOOST,
                            progress = progress.progress
                        ))
                    }
                )

                result.onSuccess {
                    Timber.i("On-demand boost complete for $section")
                }.onFailure { error ->
                    Timber.e(error, "On-demand boost failed for $section")
                }

                } finally {
                    // Clean up: remove from active syncs and job map
                    activeSyncMutex.withLock {
                        activeSyncSections.remove(section)
                    }
                    syncMutex.withLock { onDemandJobs.remove(section) }
                }
            }
        }
    }

    /**
     * Pause background sync
     */
    suspend fun pauseBackgroundSync() {
        syncMutex.withLock {
            if (_syncState.value.phase != SyncPhase.BACKGROUND_FULL) {
                Timber.d("No background sync to pause")
                return
            }

            Timber.i("Pausing background sync")

            updateSyncState {
                it.copy(
                    isPaused = true,
                    phase = SyncPhase.PAUSED,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            }

            backgroundSyncJob?.cancel()
            settingsRepository.saveSyncState(_syncState.value, accountKey())
        }
        activeSyncMutex.withLock { activeSyncSections.clear() }
    }

    /**
     * Pause all sync activity for playback.
     * Returns true if any sync work was active and is now paused.
     */
    suspend fun pauseAllForPlayback(): Boolean {
        var wasActive = false
        syncMutex.withLock {
            val phase = _syncState.value.phase
            val hasActivePhase =
                phase == SyncPhase.FAST_START ||
                        phase == SyncPhase.BACKGROUND_FULL ||
                        phase == SyncPhase.ON_DEMAND_BOOST
            val hasJobs =
                (fastStartJob?.isActive == true) ||
                        (backgroundSyncJob?.isActive == true) ||
                        onDemandJobs.values.any { it.isActive }
            wasActive = hasActivePhase || hasJobs
            if (!wasActive) {
                return@withLock
            }

            Timber.i("Pausing all sync activity for playback")

            // Let background sync notice pause and save checkpoints.
            fastStartJob?.cancel()
            onDemandJobs.values.forEach { it.cancel() }
            onDemandJobs.clear()

            updateSyncState {
                it.copy(
                    isPaused = true,
                    phase = SyncPhase.PAUSED,
                    currentSection = null,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            }
            settingsRepository.saveSyncState(_syncState.value, accountKey())
        }
        activeSyncMutex.withLock { activeSyncSections.clear() }
        return wasActive
    }

    /**
     * Resume background sync
     */
    suspend fun resumeBackgroundSync() {
        var shouldResume = false
        syncMutex.withLock {
            if (!_syncState.value.isPaused) {
                Timber.d("Background sync not paused, cannot resume")
                return
            }

            Timber.i("Resuming background sync")

            updateSyncState {
                it.copy(
                    isPaused = false,
                    phase = SyncPhase.BACKGROUND_FULL,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            }

            settingsRepository.saveSyncState(_syncState.value, accountKey())
            shouldResume = true
        }

        if (shouldResume) {
            startBackgroundFullSync(force = false, throttleMs = null, fullReindex = false)
        }
    }

    /**
     * Resume sync after playback. Restarts fast start if needed, otherwise background full.
     */
    suspend fun resumeAfterPlayback() {
        syncMutex.withLock {
            if (!_syncState.value.isPaused) {
                return
            }
            updateSyncState {
                it.copy(
                    isPaused = false,
                    phase = SyncPhase.IDLE,
                    currentSection = null,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            }
            settingsRepository.saveSyncState(_syncState.value, accountKey())
        }

        if (!_syncState.value.fastStartReady) {
            startFastStartSync()
        } else if (!_syncState.value.fullIndexComplete) {
            startBackgroundFullSync(force = false, throttleMs = null, fullReindex = false)
        }
    }

    suspend fun startManualSync() {
        syncMutex.withLock {
            if (_syncState.value.isPaused) {
                updateSyncState { it.copy(isPaused = false) }
            }
        }
        // Faster, incremental sync that skips already indexed pages
        startBackgroundFullSync(force = true, throttleMs = 100L, fullReindex = true)
    }

    /**
     * Cancel all sync operations
     */
    fun cancelAllSyncs() {
        Timber.i("Cancelling all sync operations")
        scope.launch {
            cancelAllSyncsInternal()
        }
    }

    private suspend fun cancelAllSyncsInternal() {
        syncMutex.withLock {
            fastStartJob?.cancel()
            backgroundSyncJob?.cancel()
            onDemandJobs.values.forEach { it.cancel() }
            onDemandJobs.clear()
            fastStartJob = null
            backgroundSyncJob = null
            updateSyncState {
                it.copy(
                    phase = SyncPhase.IDLE,
                    currentSection = null
                )
            }
        }
        activeSyncMutex.withLock { activeSyncSections.clear() }
    }

    /**
     * Check if fast start is ready
     */
    fun isFastStartReady(): Boolean = _syncState.value.fastStartReady

    /**
     * Check if full index is complete
     */
    fun isFullIndexComplete(): Boolean = _syncState.value.fullIndexComplete

    /**
     * Get progress for specific section
     */
    fun getSectionProgress(section: Section): SectionSyncProgress? {
        return _syncState.value.sectionProgress[section]
    }

    /**
     * Update section progress in state
     */
    private fun updateSectionProgress(section: Section, progress: SectionSyncProgress) {
        updateSyncState { state ->
            val currentProgress = state.sectionProgress.toMutableMap()
            currentProgress[section] = progress
            state.copy(sectionProgress = currentProgress)
        }
    }

    private fun accountKey(): String {
        return "${authConfig.baseUrl}|${authConfig.username}|${authConfig.listName}"
    }

    fun restoreState(state: ProgressiveSyncState) {
        updateSyncState {
            state.copy(
                currentSection = null,
                sectionProgress = emptyMap()
            )
        }
    }

    private inline fun updateSyncState(
        crossinline transform: (ProgressiveSyncState) -> ProgressiveSyncState
    ) {
        _syncState.update { current -> transform(current) }
    }

    /**
     * Clean up resources
     */
    suspend fun dispose() {
        if (disposed) return
        disposed = true
        Timber.d("Disposing ProgressiveSyncCoordinator")
        cancelAllSyncsInternal()
        scopeJob.cancelAndJoin()
    }

    fun disposeAsync() {
        if (disposed) return
        disposed = true
        Timber.d("Disposing ProgressiveSyncCoordinator")
        scopeJob.cancel()
    }
}

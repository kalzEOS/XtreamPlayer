package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section

/**
 * Represents the current phase of progressive library sync
 */
enum class SyncPhase {
    /** No sync operation in progress */
    IDLE,

    /** Fast start phase: syncing 1-2 pages per section for immediate search */
    FAST_START,

    /** Background full sync: completing entire library index with throttling */
    BACKGROUND_FULL,

    /** On-demand boost: priority sync for specific section when user enters it */
    ON_DEMAND_BOOST,

    /** All syncing operations complete */
    COMPLETE,

    /** Background sync paused by user or network loss */
    PAUSED
}

/**
 * Overall progressive sync state tracking all sections and phases
 */
data class ProgressiveSyncState(
    /** Current sync phase */
    val phase: SyncPhase = SyncPhase.IDLE,

    /** Sections that have completed full indexing */
    val sectionsCompleted: Set<Section> = emptySet(),

    /** True when fast start sync is complete and search is usable */
    val fastStartReady: Boolean = false,

    /** True when full library index is complete for all sections */
    val fullIndexComplete: Boolean = false,

    /** Currently active section being synced (null if none) */
    val currentSection: Section? = null,

    /** Per-section progress details */
    val sectionProgress: Map<Section, SectionSyncProgress> = emptyMap(),

    /** True if background sync is paused */
    val isPaused: Boolean = false,

    /** Timestamp of last sync activity (milliseconds since epoch) */
    val lastSyncTimestamp: Long = 0L
)

/**
 * Progress tracking for individual section sync
 */
data class SectionSyncProgress(
    /** Number of items indexed so far */
    val itemsIndexed: Int = 0,

    /** Estimated total items (null if unknown) */
    val estimatedTotal: Int? = null,

    /** Last page number successfully synced */
    val lastPageSynced: Int = 0,

    /** Current sync phase for this section */
    val phase: SyncPhase = SyncPhase.IDLE,

    /** Progress as fraction 0.0-1.0 (0.0 if indeterminate) */
    val progress: Float = 0f
)

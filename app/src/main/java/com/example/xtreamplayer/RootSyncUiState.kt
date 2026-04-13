package com.example.xtreamplayer

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.content.ProgressiveSyncCoordinator
import kotlinx.coroutines.Job

internal data class SectionSyncState(
    val progress: Float = 0f,
    val itemsIndexed: Int = 0,
    val isActive: Boolean = false
)

internal data class LibrarySyncRequest(
    val config: AuthConfig,
    val reason: String,
    val force: Boolean,
    val sectionsToSync: List<Section>?
)

internal class RootSyncUiState {
    val startupDeferredReady = mutableStateOf(false)
    val progressiveSyncCoordinator = mutableStateOf<ProgressiveSyncCoordinator?>(null)
    val lastRefreshedAccountKey = mutableStateOf<String?>(null)
    val isRefreshing = mutableStateOf(false)
    val refreshJob = mutableStateOf<Job?>(null)
    val refreshToken = mutableIntStateOf(0)
    val hasCacheForAccount = mutableStateOf<Boolean?>(null)
    val hasSearchIndex = mutableStateOf<Boolean?>(null)
    val sectionSyncStates = mutableStateMapOf<Section, SectionSyncState>()
    val librarySyncJob = mutableStateOf<Job?>(null)
    val librarySyncToken = mutableIntStateOf(0)
    val pendingLibrarySync = mutableStateOf<LibrarySyncRequest?>(null)
    val lastLibrarySyncRequest = mutableStateOf<LibrarySyncRequest?>(null)
    val syncedSections = mutableStateOf(setOf<Section>())
}

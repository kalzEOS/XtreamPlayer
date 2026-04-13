package com.example.xtreamplayer

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberUpdatedState
import com.example.xtreamplayer.auth.AuthUiState
import com.example.xtreamplayer.content.ContentRepository
import com.example.xtreamplayer.content.ProgressiveSyncCoordinator
import com.example.xtreamplayer.content.ProgressiveSyncState
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.settings.SettingsRepository
import com.example.xtreamplayer.settings.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val STARTUP_DEFER_NON_CRITICAL_MS = 1_000L
private const val SUBTITLE_AUTO_CLEAR_CHECK_INTERVAL_MS = 60L * 60L * 1000L

@Composable
internal fun SettingsAndSyncHost(
    context: Context,
    coroutineScope: CoroutineScope,
    settings: SettingsState,
    settingsRepository: SettingsRepository,
    contentRepository: ContentRepository,
    subtitleRepository: SubtitleRepository,
    authState: AuthUiState,
    activePlaybackQueue: PlaybackQueue?,
    selectedSectionState: MutableState<Section>,
    showManageListsState: MutableState<Boolean>,
    showAppearanceState: MutableState<Boolean>,
    focusToContentTriggerState: MutableIntState,
    focusAppearanceOnSettingsReturnState: MutableState<Boolean>,
    focusManageListsOnSettingsReturnState: MutableState<Boolean>,
    wasShowingAppearanceState: MutableState<Boolean>,
    wasShowingManageListsState: MutableState<Boolean>,
    startupDeferredReadyState: MutableState<Boolean>,
    startupUpdateCheckEnabledState: MutableState<Boolean?>,
    progressiveSyncCoordinatorState: MutableState<ProgressiveSyncCoordinator?>,
    syncState: ProgressiveSyncState
) {
    val showManageLists = showManageListsState.value
    val showAppearance = showAppearanceState.value
    val selectedSection = selectedSectionState.value
    val startupDeferredReady = startupDeferredReadyState.value
    val latestProgressiveSyncCoordinator = rememberUpdatedState(progressiveSyncCoordinatorState.value)

    LaunchedEffect(Unit) {
        delay(STARTUP_DEFER_NON_CRITICAL_MS)
        startupDeferredReadyState.value = true
    }

    LaunchedEffect(settings.subtitleCacheAutoClearIntervalMs, startupDeferredReady) {
        if (!startupDeferredReady) return@LaunchedEffect
        val intervalMs = settings.subtitleCacheAutoClearIntervalMs
        if (intervalMs <= 0L) return@LaunchedEffect

        while (true) {
            if (activePlaybackQueue != null) {
                delay(SUBTITLE_AUTO_CLEAR_CHECK_INTERVAL_MS)
                continue
            }
            val nowMs = System.currentTimeMillis()
            val lastRunMs = settingsRepository.subtitleCacheAutoClearLastRunMs()
            if (lastRunMs <= 0L) {
                settingsRepository.setSubtitleCacheAutoClearLastRunMs(nowMs)
            } else if (nowMs - lastRunMs >= intervalMs) {
                val removed = withContext(Dispatchers.IO) {
                    subtitleRepository.clearCacheAndCount()
                }
                settingsRepository.setSubtitleCacheAutoClearLastRunMs(nowMs)
                if (removed > 0) {
                    Timber.d("Auto-cleared subtitle cache files: $removed")
                }
            }
            delay(SUBTITLE_AUTO_CLEAR_CHECK_INTERVAL_MS)
        }
    }

    val syncCoordinatorAccountKey =
        authState.activeConfig?.let { config ->
            "${config.baseUrl}|${config.username}|${config.listName}|${config.password}"
        }
    LaunchedEffect(syncCoordinatorAccountKey, startupDeferredReady) {
        if (!startupDeferredReady) return@LaunchedEffect
        val previousCoordinator = progressiveSyncCoordinatorState.value
        if (previousCoordinator != null) {
            withContext(NonCancellable) {
                previousCoordinator.dispose()
            }
            progressiveSyncCoordinatorState.value = null
        }
        progressiveSyncCoordinatorState.value =
            authState.activeConfig?.let { config ->
                ProgressiveSyncCoordinator(
                    contentRepository = contentRepository,
                    settingsRepository = settingsRepository,
                    authConfig = config
                )
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            latestProgressiveSyncCoordinator.value?.disposeAsync()
        }
    }

    LaunchedEffect(authState.activeConfig, progressiveSyncCoordinatorState.value, startupDeferredReady) {
        if (!startupDeferredReady) return@LaunchedEffect
        val coordinator = progressiveSyncCoordinatorState.value ?: return@LaunchedEffect
        if (authState.activeConfig != null) {
            val config = authState.activeConfig ?: return@LaunchedEffect
            val syncAccountKey = "${config.baseUrl}|${config.username}|${config.listName}"
            val savedState = settingsRepository.loadSyncState(syncAccountKey)
            val hasFullIndex = contentRepository.hasFullIndex(config)
            val hasSearchIndex = contentRepository.hasSearchIndex(config)
            val hasAnySearchIndex = contentRepository.hasAnySearchIndex(config)

            val effectiveState =
                savedState
                    ?: if (hasFullIndex) {
                        ProgressiveSyncState(
                            phase = com.example.xtreamplayer.content.SyncPhase.COMPLETE,
                            fastStartReady = true,
                            fullIndexComplete = true,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    } else {
                        null
                    }

            if (effectiveState != null) {
                coordinator.restoreState(effectiveState)
            }

            if (!hasFullIndex && (!hasAnySearchIndex || savedState == null || !savedState.fastStartReady)) {
                coordinator.startFastStartSync()
            } else if (savedState?.phase == com.example.xtreamplayer.content.SyncPhase.BACKGROUND_FULL &&
                savedState.isPaused.not() &&
                savedState.fullIndexComplete.not()
            ) {
                coordinator.resumeBackgroundSync()
            }
        }
    }

    LaunchedEffect(syncState.fastStartReady, startupDeferredReady) {
        if (!startupDeferredReady) return@LaunchedEffect
        val coordinator = progressiveSyncCoordinatorState.value ?: return@LaunchedEffect
        if (syncState.fastStartReady && !syncState.fullIndexComplete) {
            delay(2000)
            coordinator.startBackgroundFullSync()
        }
    }

    LaunchedEffect(startupDeferredReady) {
        if (startupDeferredReady) {
            startupUpdateCheckEnabledState.value = settingsRepository.isStartupUpdateCheckEnabled()
        }
    }

    LaunchedEffect(selectedSection) {
        if (selectedSection != Section.SETTINGS) {
            showManageListsState.value = false
        }
    }

    LaunchedEffect(showManageLists) {
        if (showManageLists) {
            focusToContentTriggerState.intValue++
        }
    }

    LaunchedEffect(showAppearance) {
        if (showAppearance) {
            focusToContentTriggerState.intValue++
        }
    }

    LaunchedEffect(authState.isSignedIn) {
        if (authState.isSignedIn) {
            showManageListsState.value = false
        }
    }

    BackHandler(enabled = showManageLists) { showManageListsState.value = false }
    BackHandler(enabled = showAppearance) { showAppearanceState.value = false }

    LaunchedEffect(showAppearance, selectedSection) {
        if (wasShowingAppearanceState.value && !showAppearance && selectedSection == Section.SETTINGS) {
            focusAppearanceOnSettingsReturnState.value = true
        }
        wasShowingAppearanceState.value = showAppearance
    }

    LaunchedEffect(showManageLists, selectedSection) {
        if (wasShowingManageListsState.value && !showManageLists && selectedSection == Section.SETTINGS) {
            focusManageListsOnSettingsReturnState.value = true
        }
        wasShowingManageListsState.value = showManageLists
    }
}

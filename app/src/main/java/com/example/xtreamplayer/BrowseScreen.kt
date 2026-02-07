package com.example.xtreamplayer

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.auth.AuthUiState
import com.example.xtreamplayer.content.CategoryItem
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.content.ContentRepository
import com.example.xtreamplayer.content.ContentType
import com.example.xtreamplayer.content.ContinueWatchingEntry
import com.example.xtreamplayer.content.ContinueWatchingRepository
import com.example.xtreamplayer.content.FavoritesRepository
import com.example.xtreamplayer.content.HistoryRepository
import com.example.xtreamplayer.content.ProgressiveSyncCoordinator
import com.example.xtreamplayer.content.ProgressiveSyncState
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.settings.SettingsState
import com.example.xtreamplayer.settings.SettingsViewModel
import com.example.xtreamplayer.ui.components.SideNav
import com.example.xtreamplayer.ui.theme.AppTheme
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Composable
internal fun BrowseScreen(
    context: Context,
    coroutineScope: CoroutineScope,
    authState: AuthUiState,
    savedConfig: AuthConfig?,
    activeConfig: AuthConfig?,
    settings: SettingsState,
    settingsViewModel: SettingsViewModel,
    appVersionName: String,
    selectedSectionState: MutableState<Section>,
    navExpandedState: MutableState<Boolean>,
    navLayoutExpanded: Boolean,
    navSlideExpanded: Boolean,
    navOffsetPx: Float,
    navProgress: Float,
    moveFocusToNavState: MutableState<Boolean>,
    focusToContentTriggerState: MutableState<Int>,
    showManageListsState: MutableState<Boolean>,
    showAppearanceState: MutableState<Boolean>,
    focusAppearanceOnSettingsReturn: Boolean,
    showThemeDialogState: MutableState<Boolean>,
    showFontDialogState: MutableState<Boolean>,
    showUiScaleDialogState: MutableState<Boolean>,
    showFontScaleDialogState: MutableState<Boolean>,
    showNextEpisodeThresholdDialogState: MutableState<Boolean>,
    showApiKeyDialogState: MutableState<Boolean>,
    cacheClearNonceState: MutableState<Int>,
    contentRepository: ContentRepository,
    favoritesRepository: FavoritesRepository,
    historyRepository: HistoryRepository,
    continueWatchingRepository: ContinueWatchingRepository,
    subtitleRepository: SubtitleRepository,
    playbackEngine: com.example.xtreamplayer.player.Media3PlaybackEngine,
    progressiveSyncCoordinator: ProgressiveSyncCoordinator?,
    syncState: ProgressiveSyncState,
    storagePermissionLauncher: ActivityResultLauncher<Array<String>>,
    localFiles: androidx.compose.runtime.snapshots.SnapshotStateList<LocalFileItem>,
    allNavItemFocusRequester: FocusRequester,
    continueWatchingNavItemFocusRequester: FocusRequester,
    favoritesNavItemFocusRequester: FocusRequester,
    moviesNavItemFocusRequester: FocusRequester,
    seriesNavItemFocusRequester: FocusRequester,
    liveNavItemFocusRequester: FocusRequester,
    categoriesNavItemFocusRequester: FocusRequester,
    localFilesNavItemFocusRequester: FocusRequester,
    settingsNavItemFocusRequester: FocusRequester,
    contentItemFocusRequester: FocusRequester,
    resumeFocusId: String?,
    resumeFocusRequester: FocusRequester,
    filteredContinueWatchingItems: List<ContinueWatchingEntry>,
    filteredFavoriteContentItems: List<ContentItem>,
    filteredFavoriteCategoryItems: List<CategoryItem>,
    filteredFavoriteContentKeys: Set<String>,
    filteredFavoriteCategoryKeys: Set<String>,
    isPlaybackActive: Boolean,
    onItemFocused: (ContentItem) -> Unit,
    onPlay: (ContentItem, List<ContentItem>) -> Unit,
    onPlayWithPosition: (ContentItem, Long) -> Unit,
    onPlayContinueWatching: (ContentItem, Long, ContentItem?) -> Unit,
    onPlayWithPositionAndQueue: (ContentItem, List<ContentItem>, Long?) -> Unit,
    onMovieInfo: (ContentItem, List<ContentItem>) -> Unit,
    onMovieInfoContinueWatching: (ContentItem, List<ContentItem>) -> Unit,
    onPlayLocalFile: (Int) -> Unit,
    localResumePositionMsForUri: (Uri) -> Long?,
    onToggleFavorite: (ContentItem) -> Unit,
    onToggleCategoryFavorite: (CategoryItem) -> Unit,
    isItemFavorite: (ContentItem) -> Boolean,
    isCategoryFavorite: (CategoryItem) -> Boolean,
    onSeriesPlaybackStart: (ContentItem) -> Unit,
    onTriggerSectionSync: (Section, AuthConfig) -> Unit,
    onEditList: () -> Unit,
    onSignOutKeepSaved: () -> Unit,
    onSignOutForget: () -> Unit,
    onCheckForUpdates: () -> Unit,
    hasStoragePermission: (Context) -> Boolean,
    scanMediaStoreMedia: (Context) -> List<LocalFileItem>,
    getRequiredMediaPermissions: () -> Array<String>
) {
    var selectedSection by selectedSectionState
    var navExpanded by navExpandedState
    var moveFocusToNav by moveFocusToNavState
    var focusToContentTrigger by focusToContentTriggerState
    var showManageLists by showManageListsState
    var showAppearance by showAppearanceState
    var showThemeDialog by showThemeDialogState
    var showFontDialog by showFontDialogState
    var showUiScaleDialog by showUiScaleDialogState
    var showFontScaleDialog by showFontScaleDialogState
    var showNextEpisodeThresholdDialog by showNextEpisodeThresholdDialogState
    var showApiKeyDialog by showApiKeyDialogState
    var cacheClearNonce by cacheClearNonceState

Row(modifier = Modifier.fillMaxSize()) {
    SideNav(
            selectedSection = selectedSection,
            onSectionSelected = {
                selectedSection = it
                // Trigger lazy sync for SERIES/LIVE when first accessed
                if (it == Section.SERIES || it == Section.LIVE) {
                    activeConfig?.let { config ->
                        onTriggerSectionSync(it, config)
                    }
                }
                // Don't auto-focus content - user must press Right to navigate
                // there
            },
            onMoveRight = {
                Timber.d(
                        "NavItem: onMoveRight called, incrementing focusToContentTrigger from $focusToContentTrigger to ${focusToContentTrigger + 1}"
                )
                focusToContentTrigger++
            },
            expanded = navSlideExpanded,
            layoutExpanded = navLayoutExpanded,
            allNavItemFocusRequester = allNavItemFocusRequester,
            continueWatchingNavItemFocusRequester =
                    continueWatchingNavItemFocusRequester,
            favoritesNavItemFocusRequester = favoritesNavItemFocusRequester,
            moviesNavItemFocusRequester = moviesNavItemFocusRequester,
            seriesNavItemFocusRequester = seriesNavItemFocusRequester,
            liveNavItemFocusRequester = liveNavItemFocusRequester,
            categoriesNavItemFocusRequester = categoriesNavItemFocusRequester,
            localFilesNavItemFocusRequester = localFilesNavItemFocusRequester,
            settingsNavItemFocusRequester = settingsNavItemFocusRequester,
            modifier =
                    Modifier.graphicsLayer { translationX = navOffsetPx }
                            .alpha(navProgress)
    )

    Box(modifier = Modifier.fillMaxHeight().weight(1f)) {
        val sectionTitle = remember(selectedSection) {
                when (selectedSection) {
                    Section.ALL -> "ALL CONTENT"
                    Section.CONTINUE_WATCHING -> "CONTINUE WATCHING"
                    Section.FAVORITES -> "FAVORITES"
                    Section.MOVIES -> "MOVIES CONTENT"
                    Section.SERIES -> "SERIES CONTENT"
                    Section.LIVE -> "LIVE CONTENT"
                    Section.CATEGORIES -> "CATEGORIES CONTENT"
                    Section.LOCAL_FILES -> "PLAY LOCAL FILES"
                    Section.SETTINGS -> "SETTINGS"
                }
        }

        val handleMoveLeft = remember {
            {
                if (!navExpanded) {
                    navExpanded = true
                }
                moveFocusToNav = true
            }
        }

        if (selectedSection == Section.SETTINGS) {
            if (showManageLists) {
                ManageListsScreen(
                        savedConfig = savedConfig,
                        activeConfig = authState.activeConfig,
                        settings = settings,
                        contentItemFocusRequester = contentItemFocusRequester,
                        onMoveLeft = handleMoveLeft,
                        onBack = { showManageLists = false },
                        onEditList = {
                            showManageLists = false
                            coroutineScope.launch {
                                contentRepository.clearCache()
                                contentRepository.clearDiskCache()
                            }
                            onEditList()
                        },
                        onSignOut = {
                            showManageLists = false
                            coroutineScope.launch {
                                contentRepository.clearCache()
                                contentRepository.clearDiskCache()
                            }
                            onSignOutKeepSaved()
                        },
                        onForgetList = {
                            showManageLists = false
                            coroutineScope.launch {
                                contentRepository.clearCache()
                                contentRepository.clearDiskCache()
                            }
                            onSignOutForget()
                        }
                )
            } else if (showAppearance) {
                AppearanceScreen(
                        settings = settings,
                        contentItemFocusRequester = contentItemFocusRequester,
                        onMoveLeft = handleMoveLeft,
                        onBack = { showAppearance = false },
                        onOpenThemeSelector = { showThemeDialog = true },
                        onOpenFontSelector = { showFontDialog = true },
                        onOpenUiScale = { showUiScaleDialog = true },
                        onOpenFontScale = { showFontScaleDialog = true },
                        onToggleClockFormat = settingsViewModel::toggleClockFormat
                )
            } else {
                val activeListName =
                        authState.activeConfig?.listName
                                ?: savedConfig?.listName ?: "Not set"
                SettingsScreen(
                        settings = settings,
                        activeListName = activeListName,
                        appVersionLabel = "v${appVersionName}",
                        contentItemFocusRequester = contentItemFocusRequester,
                        focusAppearanceOnReturn = focusAppearanceOnSettingsReturn,
                        onMoveLeft = handleMoveLeft,
                        onToggleAutoPlay = settingsViewModel::toggleAutoPlayNext,
                        onOpenNextEpisodeThreshold = { showNextEpisodeThresholdDialog = true },
                        onToggleSubtitles = settingsViewModel::toggleSubtitles,
                        onOpenAppearance = { showAppearance = true },
                        onToggleRememberLogin =
                                settingsViewModel::toggleRememberLogin,
                        onToggleAutoSignIn = settingsViewModel::toggleAutoSignIn,
                        onOpenSubtitlesApiKey = { showApiKeyDialog = true },
                        onManageLists = { showManageLists = true },
                        onRefreshContent = {
                            val config = authState.activeConfig
                            if (config != null) {
                                val isSyncRunning =
                                        syncState.phase ==
                                                com.example.xtreamplayer.content.SyncPhase.FAST_START ||
                                                syncState.phase ==
                                                        com.example.xtreamplayer.content.SyncPhase.BACKGROUND_FULL ||
                                                syncState.phase ==
                                                        com.example.xtreamplayer.content.SyncPhase.ON_DEMAND_BOOST
                                if (isSyncRunning) {
                                    Toast.makeText(
                                                    context,
                                                    "Sync already in progress",
                                                    Toast.LENGTH_SHORT
                                    )
                                            .show()
                                } else {
                                    coroutineScope.launch {
                                        if (syncState.isPaused) {
                                            Toast.makeText(
                                                            context,
                                                            "Resuming sync...",
                                                            Toast.LENGTH_SHORT
                                            )
                                                    .show()
                                            progressiveSyncCoordinator?.resumeBackgroundSync()
                                        } else {
                                            Toast.makeText(
                                                            context,
                                                            "Starting library sync...",
                                                            Toast.LENGTH_SHORT
                                            )
                                                    .show()
                                            progressiveSyncCoordinator?.startManualSync()
                                        }
                                    }
                                }
                            }
                        },
                        onCheckForUpdates = { onCheckForUpdates() },
                        onClearCache = {
                            coroutineScope.launch {
                                val contentBytes = contentRepository.diskCacheSizeBytes()
                                val cacheBytes =
                                    withContext(Dispatchers.IO) {
                                        cacheDirSizeBytes(context.cacheDir)
                                    }
                                val bytes = contentBytes + cacheBytes
                                contentRepository.clearCache()
                                contentRepository.clearDiskCache()
                                withContext(Dispatchers.IO) {
                                    subtitleRepository.clearCache()
                                    clearCacheDir(context.cacheDir)
                                }
                                playbackEngine.player.stop()
                                playbackEngine.player.clearMediaItems()
                                cacheClearNonce++
                                val mb = bytes / (1024.0 * 1024.0)
                                val sizeLabel =
                                        if (mb < 0.1) {
                                            "<0.1 MB"
                                        } else {
                                            String.format(Locale.US, "%.1f MB", mb)
                                        }
                                Toast.makeText(
                                                context,
                                                "Cache cleared ($sizeLabel)",
                                                Toast.LENGTH_SHORT
                                )
                                        .show()
                            }
                        },
                        onSignOut = {
                            coroutineScope.launch {
                                contentRepository.clearCache()
                                contentRepository.clearDiskCache()
                            }
                            onSignOutKeepSaved()
                        }
                )
            }
        } else {
            val activeConfig = authState.activeConfig
            if (activeConfig != null) {
                val hasFavoriteContentKeys =
                        filteredFavoriteContentKeys.isNotEmpty()
                val hasFavoriteCategoryKeys =
                        filteredFavoriteCategoryKeys.isNotEmpty()
                when (selectedSection) {
                    Section.CONTINUE_WATCHING -> {
                        ContinueWatchingScreen(
                                title = sectionTitle,
                                contentRepository = contentRepository,
                                authConfig = activeConfig,
                                settings = settings,
                                navLayoutExpanded = navLayoutExpanded,
                                continueWatchingItems =
                                        filteredContinueWatchingItems,
                                contentItemFocusRequester =
                                        contentItemFocusRequester,
                                resumeFocusId = resumeFocusId,
                                resumeFocusRequester = resumeFocusRequester,
                                onItemFocused = onItemFocused,
                                onPlay = onPlayContinueWatching,
                                onPlayWithPosition = onPlayWithPositionAndQueue,
                                onMovieInfo = onMovieInfoContinueWatching,
                                onSeriesPlaybackStart = onSeriesPlaybackStart,
                                onMoveLeft = handleMoveLeft,
                                onToggleFavorite = onToggleFavorite,
                                onRemoveEntry = { item ->
                                    coroutineScope.launch {
                                        continueWatchingRepository.removeEntry(
                                                activeConfig,
                                                item
                                        )
                                        Toast.makeText(
                                                        context,
                                                        "Removed from Continue Watching",
                                                        Toast.LENGTH_SHORT
                                        )
                                                .show()
                                    }
                                },
                                onClearAll = {
                                    coroutineScope.launch {
                                        continueWatchingRepository.clearAll(activeConfig)
                                        Toast.makeText(
                                                        context,
                                                        "Continue Watching cleared",
                                                        Toast.LENGTH_SHORT
                                        )
                                                .show()
                                    }
                                },
                                isItemFavorite = isItemFavorite
                        )
                    }
                    Section.CATEGORIES -> {
                        CategorySectionScreen(
                                title = sectionTitle,
                                contentRepository = contentRepository,
                                authConfig = activeConfig,
                                settings = settings,
                                navLayoutExpanded = navLayoutExpanded,
                                isPlaybackActive = isPlaybackActive,
                                continueWatchingEntries = filteredContinueWatchingItems,
                                contentItemFocusRequester =
                                        contentItemFocusRequester,
                                resumeFocusId = resumeFocusId,
                                resumeFocusRequester = resumeFocusRequester,
                                onItemFocused = onItemFocused,
                                onPlay = onPlay,
                                onPlayWithPosition = onPlayWithPositionAndQueue,
                                onMovieInfo = onMovieInfo,
                                onMoveLeft = handleMoveLeft,
                                onToggleFavorite = onToggleFavorite,
                                onRemoveContinueWatching = { item ->
                                    coroutineScope.launch {
                                        continueWatchingRepository.removeEntry(activeConfig, item)
                                    }
                                },
                                onToggleCategoryFavorite =
                                        onToggleCategoryFavorite,
                                isItemFavorite = isItemFavorite,
                                isCategoryFavorite = isCategoryFavorite,
                                onSeriesPlaybackStart = onSeriesPlaybackStart
                        )
                    }
                    Section.FAVORITES -> {
                        FavoritesScreen(
                                title = sectionTitle,
                                contentRepository = contentRepository,
                                authConfig = activeConfig,
                                settings = settings,
                                navLayoutExpanded = navLayoutExpanded,
                                isPlaybackActive = isPlaybackActive,
                                favoriteContentItems = filteredFavoriteContentItems,
                                favoriteCategoryItems =
                                        filteredFavoriteCategoryItems,
                                hasFavoriteContentKeys = hasFavoriteContentKeys,
                                hasFavoriteCategoryKeys = hasFavoriteCategoryKeys,
                                continueWatchingEntries = filteredContinueWatchingItems,
                                contentItemFocusRequester =
                                        contentItemFocusRequester,
                                resumeFocusId = resumeFocusId,
                                resumeFocusRequester = resumeFocusRequester,
                                onItemFocused = onItemFocused,
                                onPlay = onPlay,
                                onPlayWithPosition = onPlayWithPositionAndQueue,
                                onMovieInfo = onMovieInfo,
                                onMoveLeft = handleMoveLeft,
                                onToggleFavorite = onToggleFavorite,
                                onRemoveContinueWatching = { item ->
                                    coroutineScope.launch {
                                        continueWatchingRepository.removeEntry(activeConfig, item)
                                    }
                                },
                                onToggleCategoryFavorite =
                                        onToggleCategoryFavorite,
                                isItemFavorite = isItemFavorite,
                                isCategoryFavorite = isCategoryFavorite,
                                onSeriesPlaybackStart = onSeriesPlaybackStart
                        )
                    }
                    Section.LOCAL_FILES -> {
                        LocalFilesScreen(
                                title = sectionTitle,
                                settings = settings,
                                files = localFiles,
                                contentItemFocusRequester =
                                        contentItemFocusRequester,
                                resumeFocusId = resumeFocusId,
                                resumeFocusRequester = resumeFocusRequester,
                                onPickFiles = {
                                    if (hasStoragePermission(context)) {
                                        val scanned = scanMediaStoreMedia(context)
                                        scanned.forEach { item ->
                                            if (localFiles.none {
                                                        it.uri == item.uri
                                                    }
                                            ) {
                                                localFiles.add(item)
                                            }
                                        }
                                        if (scanned.isEmpty()) {
                                            Toast.makeText(
                                                            context,
                                                            "No media files found on device",
                                                            Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                        } else {
                                            val videoCount =
                                                    scanned.count {
                                                        it.mediaType ==
                                                                LocalMediaType.VIDEO
                                                    }
                                            val audioCount =
                                                    scanned.count {
                                                        it.mediaType ==
                                                                LocalMediaType.AUDIO
                                                    }
                                            Toast.makeText(
                                                            context,
                                                            "Found $videoCount video(s), $audioCount audio file(s)",
                                                            Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                        }
                                    } else {
                                        storagePermissionLauncher.launch(
                                                getRequiredMediaPermissions()
                                        )
                                    }
                                },
                                onRefresh = {
                                    if (hasStoragePermission(context)) {
                                        // Clear the list and rescan to remove
                                        // disconnected storage items
                                        localFiles.clear()
                                        val scanned = scanMediaStoreMedia(context)
                                        localFiles.addAll(scanned)
                                        if (scanned.isEmpty()) {
                                            Toast.makeText(
                                                            context,
                                                            "No media files found",
                                                            Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                        } else {
                                            val videoCount =
                                                    scanned.count {
                                                        it.mediaType ==
                                                                LocalMediaType.VIDEO
                                                    }
                                            val audioCount =
                                                    scanned.count {
                                                        it.mediaType ==
                                                                LocalMediaType.AUDIO
                                                    }
                                            Toast.makeText(
                                                            context,
                                                            "Refreshed: $videoCount video(s), $audioCount audio file(s)",
                                                            Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                        }
                                    } else {
                                        Toast.makeText(
                                                        context,
                                                        "Scan for media first to grant permissions",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    }
                                },
                                onPlayFile = onPlayLocalFile,
                                localResumePositionMsForUri = localResumePositionMsForUri,
                                onMoveLeft = handleMoveLeft
                        )
                    }
                    else -> {
                        SectionScreen(
                                title = sectionTitle,
                                section = selectedSection,
                                contentRepository = contentRepository,
                                authConfig = activeConfig,
                                settings = settings,
                                navLayoutExpanded = navLayoutExpanded,
                                isPlaybackActive = isPlaybackActive,
                                continueWatchingEntries = filteredContinueWatchingItems,
                                contentItemFocusRequester =
                                        contentItemFocusRequester,
                                resumeFocusId = resumeFocusId,
                                resumeFocusRequester = resumeFocusRequester,
                                onItemFocused = onItemFocused,
                                onPlay = onPlay,
                                onPlayWithPosition = onPlayWithPositionAndQueue,
                                onMovieInfo = onMovieInfo,
                                onMoveLeft = handleMoveLeft,
                                onToggleFavorite = onToggleFavorite,
                                onRemoveContinueWatching = { item ->
                                    coroutineScope.launch {
                                        continueWatchingRepository.removeEntry(activeConfig, item)
                                    }
                                },
                                isItemFavorite = isItemFavorite,
                                onSeriesPlaybackStart = onSeriesPlaybackStart
                        )
                    }
                }
            }
        }
    }
}
}

package com.example.xtreamplayer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.auth.AuthViewModel
import com.example.xtreamplayer.content.CategoryItem
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.content.ContentRepository
import com.example.xtreamplayer.content.ContentType
import com.example.xtreamplayer.content.ContinueWatchingEntry
import com.example.xtreamplayer.content.ContinueWatchingRepository
import com.example.xtreamplayer.content.FavoriteContentEntry
import com.example.xtreamplayer.content.FavoritesRepository
import com.example.xtreamplayer.content.HistoryEntry
import com.example.xtreamplayer.content.HistoryRepository
import com.example.xtreamplayer.content.MovieInfo
import com.example.xtreamplayer.content.SeriesInfo
import com.example.xtreamplayer.content.SearchNormalizer
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.player.Media3PlaybackEngine
import com.example.xtreamplayer.player.BufferProfile
import com.example.xtreamplayer.settings.PlaybackSettingsController
import com.example.xtreamplayer.settings.SettingsState
import com.example.xtreamplayer.settings.SettingsViewModel
import com.example.xtreamplayer.update.UpdateRelease
import com.example.xtreamplayer.update.compareVersions
import com.example.xtreamplayer.update.downloadUpdateApk
import com.example.xtreamplayer.update.fetchLatestRelease
import com.example.xtreamplayer.update.parseVersionParts
import com.example.xtreamplayer.ui.ApiKeyInputDialog
import com.example.xtreamplayer.ui.AppDialog
import com.example.xtreamplayer.ui.AppScale
import com.example.xtreamplayer.ui.LocalAppBaseDensity
import com.example.xtreamplayer.ui.LocalAppScale
import com.example.xtreamplayer.ui.AudioBoostDialog
import com.example.xtreamplayer.ui.AudioTrackDialog
import com.example.xtreamplayer.ui.FocusableButton
import com.example.xtreamplayer.ui.NextEpisodeOverlay
import com.example.xtreamplayer.ui.NextEpisodeThresholdDialog
import com.example.xtreamplayer.ui.PlaybackSettingsDialog
import com.example.xtreamplayer.ui.PlaybackSpeedDialog
import com.example.xtreamplayer.ui.SubtitleDialogState
import com.example.xtreamplayer.ui.SubtitleOptionsDialog
import com.example.xtreamplayer.ui.SubtitleSearchDialog
import com.example.xtreamplayer.ui.FontSelectionDialog
import com.example.xtreamplayer.ui.FontScaleDialog
import com.example.xtreamplayer.ui.ThemeSelectionDialog
import com.example.xtreamplayer.ui.UiScaleDialog
import com.example.xtreamplayer.ui.VideoResolutionDialog
import com.example.xtreamplayer.ui.rememberDebouncedSearchState
import com.example.xtreamplayer.ui.components.AppBackground
import com.example.xtreamplayer.ui.components.MenuButton
import com.example.xtreamplayer.ui.components.NAV_WIDTH
import com.example.xtreamplayer.ui.components.SideNav
import com.example.xtreamplayer.ui.components.TopBar
import com.example.xtreamplayer.ui.components.TopBarButton
import com.example.xtreamplayer.ui.components.bestContrastText
import com.example.xtreamplayer.ui.theme.AppTheme
import com.example.xtreamplayer.ui.theme.AppColors
import com.example.xtreamplayer.ui.theme.XtreamPlayerTheme
import com.example.xtreamplayer.viewmodel.BrowseViewModel
import com.example.xtreamplayer.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.util.Locale
import java.io.File
import android.provider.Settings
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

private enum class HomeDestination {
    HOME,
    LIVE,
    MOVIES,
    SERIES,
    SETTINGS
}

private data class UpdateUiState(
    val showDialog: Boolean = false,
    val inProgress: Boolean = false,
    val pendingRelease: UpdateRelease? = null
)

private data class SectionSyncState(
    val progress: Float = 0f,
    val itemsIndexed: Int = 0,
    val isActive: Boolean = false
)

private data class LibrarySyncRequest(
    val config: AuthConfig,
    val reason: String,
    val force: Boolean,
    val sectionsToSync: List<Section>?
)


@Composable
fun RootScreen(
        playbackSettingsController: PlaybackSettingsController,
        playbackEngine: Media3PlaybackEngine,
        contentRepository: ContentRepository,
        favoritesRepository: FavoritesRepository,
        historyRepository: HistoryRepository,
        continueWatchingRepository: ContinueWatchingRepository,
        subtitleRepository: SubtitleRepository
) {
    val context = LocalContext.current
    val appVersionName = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: ""
        }.getOrDefault("")
    }
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = hiltViewModel()
    val browseViewModel: BrowseViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val savedConfig by authViewModel.savedConfig.collectAsStateWithLifecycle()
    val savedConfigLoaded by authViewModel.savedConfigLoaded.collectAsStateWithLifecycle()
    val favoriteContentKeys by
            favoritesRepository.favoriteContentKeys.collectAsStateWithLifecycle(
                    initialValue = emptySet()
            )
    val favoriteCategoryKeys by
            favoritesRepository.favoriteCategoryKeys.collectAsStateWithLifecycle(
                    initialValue = emptySet()
            )
    val favoriteContentEntries by
            favoritesRepository.favoriteContentEntries.collectAsStateWithLifecycle(
                    initialValue = emptyList()
            )
    val favoriteCategoryEntries by
            favoritesRepository.favoriteCategoryEntries.collectAsStateWithLifecycle(
                    initialValue = emptyList()
            )
    val historyEntries by
            historyRepository.historyEntries.collectAsStateWithLifecycle(initialValue = emptyList())
    val continueWatchingEntries by
            continueWatchingRepository.continueWatchingEntries.collectAsStateWithLifecycle(
                    initialValue = emptyList()
            )
    var pendingPlayerReset by playerViewModel.pendingPlayerReset
    var playerResetNonce by playerViewModel.playerResetNonce

    var selectedSection by browseViewModel.selectedSection
    var navExpanded by browseViewModel.navExpanded
    var navLayoutExpanded by remember { mutableStateOf(true) }
    var navSlideExpanded by remember { mutableStateOf(true) }
    val showManageListsState = remember { mutableStateOf(false) }
    var showManageLists by showManageListsState
    val showAppearanceState = remember { mutableStateOf(false) }
    var showAppearance by showAppearanceState
    var wasShowingAppearance by remember { mutableStateOf(false) }
    var updateUiState by remember { mutableStateOf(UpdateUiState()) }
    var updateCheckJob by remember { mutableStateOf<Job?>(null) }
    val showApiKeyDialogState = remember { mutableStateOf(false) }
    var showApiKeyDialog by showApiKeyDialogState
    val showThemeDialogState = remember { mutableStateOf(false) }
    var showThemeDialog by showThemeDialogState
    val showFontDialogState = remember { mutableStateOf(false) }
    var showFontDialog by showFontDialogState
    val showUiScaleDialogState = remember { mutableStateOf(false) }
    var showUiScaleDialog by showUiScaleDialogState
    val showFontScaleDialogState = remember { mutableStateOf(false) }
    var showFontScaleDialog by showFontScaleDialogState
    val showNextEpisodeThresholdDialogState = remember { mutableStateOf(false) }
    var showNextEpisodeThresholdDialog by showNextEpisodeThresholdDialogState
    var showLocalFilesGuest by remember { mutableStateOf(false) }
    val cacheClearNonceState = remember { mutableStateOf(0) }
    var cacheClearNonce by cacheClearNonceState
    var activePlaybackQueue by playerViewModel.activePlaybackQueue
    var activePlaybackTitle by playerViewModel.activePlaybackTitle
    var activePlaybackItem by playerViewModel.activePlaybackItem
    var activePlaybackItems by playerViewModel.activePlaybackItems
    var activePlaybackSeriesParent by playerViewModel.activePlaybackSeriesParent
    var movieInfoItem by remember { mutableStateOf<ContentItem?>(null) }
    var movieInfoQueue by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var movieInfoInfo by remember { mutableStateOf<MovieInfo?>(null) }
    var movieInfoFromContinueWatching by remember { mutableStateOf(false) }
    var movieInfoResumePositionMs by remember { mutableStateOf<Long?>(null) }
    var movieInfoLoadJob by remember { mutableStateOf<Job?>(null) }
    var playbackFallbackAttempts by playerViewModel.playbackFallbackAttempts
    var liveReconnectAttempts by playerViewModel.liveReconnectAttempts
    var liveReconnectJob by playerViewModel.liveReconnectJob
    var pendingResume by playerViewModel.pendingResume
    var syncPausedForPlayback by remember { mutableStateOf(false) }
    var resumePositionMs by playerViewModel.resumePositionMs
    var resumeFocusId by playerViewModel.resumeFocusId
    val resumeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(navExpanded) {
        if (navExpanded) {
            navLayoutExpanded = true
            navSlideExpanded = false
            withFrameNanos {}
            delay(16)
            navSlideExpanded = true
        } else {
            navSlideExpanded = false
            delay(NAV_ANIM_DURATION_MS.toLong())
            if (!navExpanded) {
                navLayoutExpanded = false
            }
        }
    }

    // Progressive sync coordinator
    val settingsRepository = remember { com.example.xtreamplayer.settings.SettingsRepository(context) }
    val progressiveSyncCoordinator = remember(authState.activeConfig) {
        authState.activeConfig?.let { config ->
            com.example.xtreamplayer.content.ProgressiveSyncCoordinator(
                contentRepository = contentRepository,
                settingsRepository = settingsRepository,
                authConfig = config
            )
        }
    }
    val syncState by (progressiveSyncCoordinator?.syncState ?: kotlinx.coroutines.flow.MutableStateFlow(
        com.example.xtreamplayer.content.ProgressiveSyncState()
    )).collectAsStateWithLifecycle()

    DisposableEffect(progressiveSyncCoordinator) {
        onDispose {
            progressiveSyncCoordinator?.dispose()
        }
    }

    // Auto-start fast start sync on first login
    LaunchedEffect(authState.activeConfig) {
        if (authState.activeConfig != null && progressiveSyncCoordinator != null) {
            val config = authState.activeConfig ?: return@LaunchedEffect
            val syncAccountKey = "${config.baseUrl}|${config.username}|${config.listName}"
            val savedState = settingsRepository.loadSyncState(syncAccountKey)
            val hasFullIndex = contentRepository.hasFullIndex(config)
            val hasSearchIndex = contentRepository.hasSearchIndex(config)
            val hasAnySearchIndex = contentRepository.hasAnySearchIndex(config)

            val effectiveState =
                    savedState
                            ?: if (hasFullIndex) {
                                com.example.xtreamplayer.content.ProgressiveSyncState(
                                        phase = com.example.xtreamplayer.content.SyncPhase.COMPLETE,
                                        fastStartReady = true,
                                        fullIndexComplete = true,
                                        lastSyncTimestamp = System.currentTimeMillis()
                                )
                            } else {
                                null
                            }

            if (effectiveState != null) {
                progressiveSyncCoordinator.restoreState(effectiveState)
            }

            if (!hasFullIndex && (!hasAnySearchIndex || savedState == null || !savedState.fastStartReady)) {
                progressiveSyncCoordinator.startFastStartSync()
            } else if (savedState?.phase == com.example.xtreamplayer.content.SyncPhase.BACKGROUND_FULL &&
                            savedState.isPaused.not() &&
                            savedState.fullIndexComplete.not()
            ) {
                progressiveSyncCoordinator.resumeBackgroundSync()
            }
        }
    }

    // Auto-start background sync after fast start completes
    LaunchedEffect(syncState.fastStartReady) {
        if (syncState.fastStartReady && !syncState.fullIndexComplete && progressiveSyncCoordinator != null) {
            kotlinx.coroutines.delay(2000) // 2 second grace period
            progressiveSyncCoordinator.startBackgroundFullSync()
        }
    }

    val focusToContentTriggerState = remember { mutableStateOf(0) }
    var focusToContentTrigger by focusToContentTriggerState
    val moveFocusToNavState = remember { mutableStateOf(false) }
    var moveFocusToNav by moveFocusToNavState

    val allNavItemFocusRequester = remember { FocusRequester() }
    val continueWatchingNavItemFocusRequester = remember { FocusRequester() }
    val favoritesNavItemFocusRequester = remember { FocusRequester() }
    val moviesNavItemFocusRequester = remember { FocusRequester() }
    val seriesNavItemFocusRequester = remember { FocusRequester() }
    val liveNavItemFocusRequester = remember { FocusRequester() }
    val categoriesNavItemFocusRequester = remember { FocusRequester() }
    val localFilesNavItemFocusRequester = remember { FocusRequester() }
    val settingsNavItemFocusRequester = remember { FocusRequester() }
    val contentItemFocusRequester = remember { FocusRequester() }
    val localFiles = remember { mutableStateListOf<LocalFileItem>() }

    fun scanLocalMedia(
            replaceExisting: Boolean,
            emptyMessage: String,
            successMessage: (videoCount: Int, audioCount: Int) -> String
    ) {
        coroutineScope.launch {
            val scanned = withContext(Dispatchers.IO) { scanMediaStoreMedia(context) }
            if (replaceExisting) {
                localFiles.clear()
                localFiles.addAll(scanned)
            } else {
                scanned.forEach { item ->
                    if (localFiles.none { it.uri == item.uri }) {
                        localFiles.add(item)
                    }
                }
            }
            if (scanned.isEmpty()) {
                Toast.makeText(context, emptyMessage, Toast.LENGTH_SHORT).show()
            } else {
                val videoCount = scanned.count { it.mediaType == LocalMediaType.VIDEO }
                val audioCount = scanned.count { it.mediaType == LocalMediaType.AUDIO }
                Toast.makeText(
                        context,
                        successMessage(videoCount, audioCount),
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val storagePermissionLauncher =
            rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted) {
                    scanLocalMedia(
                            replaceExisting = false,
                            emptyMessage = "No media files found on device",
                            successMessage = { videoCount, audioCount ->
                                "Found $videoCount video(s), $audioCount audio file(s)"
                            }
                    )
                } else {
                    Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }

    suspend fun requestFocusWithFrames(requester: FocusRequester, label: String): Boolean {
        fun tryRequest(attempt: Int): Boolean {
            return runCatching { requester.requestFocus() }.getOrElse { error ->
                Timber.w(error, "FocusDebug: $label focus request failed (attempt $attempt)")
                false
            }
        }
        repeat(3) { attempt ->
            withFrameNanos {}
            if (tryRequest(attempt + 1)) return true
        }
        return false
    }

    LaunchedEffect(focusToContentTrigger) {
        if (focusToContentTrigger > 0) {
            Timber.d("FocusDebug: Requesting content focus for trigger=$focusToContentTrigger")
            val resumeKey = resumeFocusId
            val requesters =
                    if (resumeKey != null) {
                        listOf(resumeFocusRequester, contentItemFocusRequester)
                    } else {
                        listOf(contentItemFocusRequester)
                    }
            requesters.forEach { requester ->
                if (requestFocusWithFrames(requester, "content")) {
                    return@LaunchedEffect
                }
            }
        }
    }

    LaunchedEffect(moveFocusToNav) {
        if (moveFocusToNav) {
            val requester =
                    when (selectedSection) {
                        Section.ALL -> allNavItemFocusRequester
                        Section.CONTINUE_WATCHING -> continueWatchingNavItemFocusRequester
                        Section.FAVORITES -> favoritesNavItemFocusRequester
                        Section.MOVIES -> moviesNavItemFocusRequester
                        Section.SERIES -> seriesNavItemFocusRequester
                        Section.LIVE -> liveNavItemFocusRequester
                        Section.CATEGORIES -> categoriesNavItemFocusRequester
                        Section.LOCAL_FILES -> localFilesNavItemFocusRequester
                        Section.SETTINGS -> settingsNavItemFocusRequester
                    }
            requestFocusWithFrames(requester, "nav")
            moveFocusToNav = false
        }
    }

    LaunchedEffect(showAppearance, selectedSection) {
        if (wasShowingAppearance && !showAppearance && selectedSection == Section.SETTINGS) {
            requestFocusWithFrames(contentItemFocusRequester, "settings-back")
        }
        wasShowingAppearance = showAppearance
    }

    LaunchedEffect(settings.autoSignIn, settings.rememberLogin, savedConfig) {
        authViewModel.tryAutoSignIn(settings)
    }

    LaunchedEffect(authState.activeConfig) {
        if (authState.activeConfig != null) {
            showLocalFilesGuest = false
        }
    }

    LaunchedEffect(
            authState.activeConfig?.username,
            authState.activeConfig?.baseUrl,
            authState.activeConfig?.listName
    ) { contentRepository.clearCache() }

    val activeConfig = authState.activeConfig
    val accountKey = activeConfig?.let { "${it.baseUrl}|${it.username}|${it.listName}" }
    var lastRefreshedAccountKey by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshJob by remember { mutableStateOf<Job?>(null) }
    var refreshToken by remember { mutableStateOf(0) }
    var hasCacheForAccount by remember { mutableStateOf<Boolean?>(null) }
    var hasSearchIndex by remember { mutableStateOf<Boolean?>(null) }
    val updateHttpClient = remember { OkHttpClient() }

    val sectionSyncStates = remember {
        mutableStateMapOf<Section, SectionSyncState>()
    }
    var librarySyncJob by remember { mutableStateOf<Job?>(null) }
    var librarySyncToken by remember { mutableStateOf(0) }
    var pendingLibrarySync by remember { mutableStateOf<LibrarySyncRequest?>(null) }
    var lastLibrarySyncRequest by remember { mutableStateOf<LibrarySyncRequest?>(null) }

    // Track which sections have been synced
    var syncedSections by remember { mutableStateOf(setOf<Section>()) }

    val isLibrarySyncing = sectionSyncStates.values.any { it.isActive }

    fun triggerLibrarySync(config: AuthConfig, reason: String, force: Boolean, sectionsToSync: List<Section>? = null) {
        if (isLibrarySyncing) return
        val request = LibrarySyncRequest(
            config = config,
            reason = reason,
            force = force,
            sectionsToSync = sectionsToSync
        )
        lastLibrarySyncRequest = request
        if (activePlaybackQueue != null) {
            pendingLibrarySync = request
            return
        }

        val sections = sectionsToSync ?: listOf(Section.SERIES, Section.MOVIES, Section.LIVE)
        // Mark sections as syncing
        sections.forEach { section ->
            sectionSyncStates[section] = SectionSyncState(isActive = true)
        }

        Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
        val token = librarySyncToken
        val configKey = "${config.baseUrl}|${config.username}|${config.listName}"
        librarySyncJob = coroutineScope.launch {
            val result = runCatching {
                contentRepository.syncSearchIndex(config, force, sectionsToSync) { progress ->
                    // Update progress for specific section
                    sectionSyncStates[progress.section] = SectionSyncState(
                        progress = progress.progress,
                        itemsIndexed = progress.itemsIndexed,
                        isActive = true
                    )
                }
            }
            if (token != librarySyncToken || accountKey != configKey) {
                // Clear syncing state
                sections.forEach { section ->
                    sectionSyncStates.remove(section)
                }
                return@launch
            }
            val message =
                    if (result.isSuccess) {
                        // Mark sections as synced and clear syncing state
                        sections.forEach { section ->
                            sectionSyncStates.remove(section)
                        }
                        syncedSections = syncedSections + sections
                        hasSearchIndex = contentRepository.hasSearchIndex(config)
                        "Search library ready"
                    } else {
                        // Clear syncing state on error
                        sections.forEach { section ->
                            sectionSyncStates.remove(section)
                        }
                        val detail = result.exceptionOrNull()?.message ?: "Unknown error"
                        "Search library failed: $detail"
                    }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun triggerSectionSync(section: Section, config: AuthConfig) {
        // Skip if already synced
        if (syncedSections.contains(section)) return

        // Use progressive sync coordinator for on-demand boost
        coroutineScope.launch {
            val checkpoint = contentRepository.getSectionSyncCheckpoint(section, config)
            if (checkpoint?.isComplete == true) {
                syncedSections = syncedSections + section
                return@launch
            }

            // Trigger on-demand boost
            progressiveSyncCoordinator?.boostSection(section)
        }
    }
    fun triggerRefresh(config: AuthConfig, reason: String) {
        if (isRefreshing) return
        isRefreshing = true
        Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
        val token = refreshToken
        val configKey = "${config.baseUrl}|${config.username}|${config.listName}"
        refreshJob = coroutineScope.launch {
            val result = runCatching { contentRepository.refreshContent(config) }
            if (token != refreshToken || accountKey != configKey) {
                isRefreshing = false
                return@launch
            }
            val message =
                    if (result.isSuccess) {
                        hasCacheForAccount = true
                        "Refresh complete. Content updates as you browse."
                    } else {
                        val detail = result.exceptionOrNull()?.message ?: "Unknown error"
                        "Refresh failed: $detail"
                    }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            isRefreshing = false
        }
    }

    fun launchApkInstall(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun ensureInstallPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        if (context.packageManager.canRequestPackageInstalls()) return true
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(
            context,
            "Allow installs from this app to continue",
            Toast.LENGTH_LONG
        ).show()
        return false
    }

    fun checkForUpdates() {
        if (updateCheckJob?.isActive == true) return
        updateCheckJob = coroutineScope.launch {
            val result = runCatching { fetchLatestRelease(updateHttpClient) }
            val latest = result.getOrNull()
            if (latest == null) {
                Toast.makeText(context, "Update check failed", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val localParts = parseVersionParts(appVersionName)
            if (localParts.isEmpty() || latest.versionParts.isEmpty()) {
                Toast.makeText(context, "Update info unavailable", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (compareVersions(localParts, latest.versionParts) >= 0) {
                Toast.makeText(context, "Already up to date", Toast.LENGTH_SHORT).show()
                return@launch
            }
            updateUiState = updateUiState.copy(
                pendingRelease = latest,
                showDialog = true
            )
        }
    }

    fun startUpdateDownload(release: UpdateRelease) {
        if (updateUiState.inProgress) return
        updateUiState = updateUiState.copy(inProgress = true)
        coroutineScope.launch {
            val file = runCatching {
                downloadUpdateApk(context, release, updateHttpClient)
            }.getOrNull()
            updateUiState = updateUiState.copy(inProgress = false)
            if (file == null) {
                Toast.makeText(context, "Update download failed", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (!ensureInstallPermission()) {
                return@launch
            }
            updateUiState = updateUiState.copy(showDialog = false)
            launchApkInstall(file)
        }
    }
    LaunchedEffect(accountKey) {
        if (activeConfig == null) {
            hasCacheForAccount = null
            hasSearchIndex = null
        } else {
            hasCacheForAccount = contentRepository.hasCacheFor(activeConfig)
            hasSearchIndex = contentRepository.hasSearchIndex(activeConfig)
        }
    }
    LaunchedEffect(authState.isSignedIn, accountKey, hasCacheForAccount) {
        if (authState.isSignedIn &&
                        activeConfig != null &&
                        accountKey != lastRefreshedAccountKey &&
                        hasCacheForAccount == false
        ) {
            triggerRefresh(activeConfig, "Refreshing content...")
            lastRefreshedAccountKey = accountKey
        }
    }
    // Legacy search indexing is handled by progressive sync

    LaunchedEffect(authState.isSignedIn) {
        if (authState.isSignedIn) {
            navExpanded = true
            moveFocusToNav = true
            selectedSection = Section.ALL
            showManageLists = false
        } else {
            refreshToken++
            librarySyncToken++
            refreshJob?.cancel()
            librarySyncJob?.cancel()
            refreshJob = null
            librarySyncJob = null
            isRefreshing = false
            sectionSyncStates.clear()
            syncedSections = emptySet()
            lastRefreshedAccountKey = null
            hasCacheForAccount = null
            hasSearchIndex = null
        }
    }

    LaunchedEffect(selectedSection) {
        if (selectedSection != Section.SETTINGS) {
            showManageLists = false
        }
    }

    LaunchedEffect(showManageLists) {
        if (showManageLists) {
            focusToContentTrigger++
        }
    }

    LaunchedEffect(showAppearance) {
        if (showAppearance) {
            focusToContentTrigger++
        }
    }

    LaunchedEffect(activePlaybackQueue) {
        val queue = activePlaybackQueue
        playbackFallbackAttempts = queue?.fallbackUris?.mapValues { 0 } ?: emptyMap()
        val player: ExoPlayer? = playbackEngine.player
        if (queue != null) {
            if (player == null) return@LaunchedEffect
            playbackEngine.setQueue(queue.items, queue.startIndex)
            val seekPosition = resumePositionMs
            if (seekPosition != null && seekPosition > 0) {
                player.seekTo(seekPosition)
                resumePositionMs = null
            }
            player.playWhenReady = true
        } else {
            player?.stop()
            player?.clearMediaItems()
            if (resumeFocusId != null) {
                resumeFocusRequester.requestFocus()
            }
        }
    }

    BackHandler(enabled = showManageLists) { showManageLists = false }
    BackHandler(enabled = showAppearance) { showAppearance = false }

    val handleItemFocused: (ContentItem) -> Unit = { item -> resumeFocusId = item.id }
    val openMovieInfo: (ContentItem, List<ContentItem>) -> Unit = { item, items ->
        val config = authState.activeConfig
        if (config != null) {
            movieInfoLoadJob?.cancel()
            movieInfoItem = null
            movieInfoInfo = null
            movieInfoFromContinueWatching = false
            movieInfoResumePositionMs = null
            movieInfoQueue = if (items.isEmpty()) listOf(item) else items
            movieInfoLoadJob =
                    coroutineScope.launch {
                        val infoResult = runCatching { contentRepository.loadMovieInfo(item, config) }
                        val info = infoResult.getOrNull()
                        if (info == null) {
                            val message =
                                    if (infoResult.exceptionOrNull() != null) {
                                        "Failed to load content info"
                                    } else {
                                        "No details available"
                                    }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                        movieInfoInfo = info
                        movieInfoItem = item
                    }
        }
    }

    val openMovieInfoFromContinueWatching: (ContentItem, List<ContentItem>) -> Unit = { item, items ->
        val config = authState.activeConfig
        if (config != null) {
            movieInfoLoadJob?.cancel()
            movieInfoItem = null
            movieInfoInfo = null
            movieInfoFromContinueWatching = true
            movieInfoResumePositionMs =
                    continueWatchingEntries.firstOrNull {
                        it.item.contentType == ContentType.MOVIES &&
                                (it.item.id == item.id || it.item.streamId == item.streamId)
                    }?.positionMs?.takeIf { it > 0 }
            movieInfoQueue = if (items.isEmpty()) listOf(item) else items
            movieInfoLoadJob =
                    coroutineScope.launch {
                        val infoResult = runCatching { contentRepository.loadMovieInfo(item, config) }
                        val info = infoResult.getOrNull()
                        if (info == null) {
                            val message =
                                    if (infoResult.exceptionOrNull() != null) {
                                        "Failed to load content info"
                                    } else {
                                        "No details available"
                                    }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                        movieInfoInfo = info
                        movieInfoItem = item
                    }
        }
    }

    val handlePlayItem: (ContentItem, List<ContentItem>) -> Unit = { item, items ->
        val config = authState.activeConfig
        if (config != null) {
            resumeFocusId = item.id
            val playableItems = items.filter(::isPlayableContent)
            activePlaybackItems = playableItems
            activePlaybackItem = item
            if (item.contentType != ContentType.SERIES) {
                activePlaybackSeriesParent = null
            }
            val profile =
                    if (item.contentType == ContentType.LIVE) {
                        BufferProfile.LIVE
                    } else {
                        BufferProfile.VOD
                    }
            playbackEngine.setBufferProfile(profile)
            val queue = buildPlaybackQueue(items, item, config)
            activePlaybackQueue = queue
            activePlaybackTitle = queue.items.getOrNull(queue.startIndex)?.title ?: item.title
            coroutineScope.launch { historyRepository.addToHistory(config, item) }
        }
    }

    val switchLiveChannel: (Int) -> Boolean = switchLiveChannel@{ delta ->
        val config = authState.activeConfig ?: return@switchLiveChannel false
        val current = activePlaybackItem ?: return@switchLiveChannel false
        if (current.contentType != ContentType.LIVE) return@switchLiveChannel false
        val liveItems = activePlaybackItems.filter { it.contentType == ContentType.LIVE }
        if (liveItems.isEmpty()) return@switchLiveChannel false
        val currentIndex = liveItems.indexOfFirst { it.id == current.id }.takeIf { it >= 0 }
            ?: return@switchLiveChannel false
        val nextIndex = ((currentIndex + delta) % liveItems.size + liveItems.size) % liveItems.size
        val nextItem = liveItems[nextIndex]

        resumePositionMs = null
        activePlaybackItems = liveItems
        activePlaybackItem = nextItem
        activePlaybackTitle = nextItem.title
        activePlaybackSeriesParent = null
        val queue = buildPlaybackQueue(liveItems, nextItem, config)
        activePlaybackQueue = queue
        true
    }

    val handlePlayLocalFile: (Int) -> Unit = { index ->
        if (index in localFiles.indices) {
            resumeFocusId = localFiles[index].uri.toString()
            activePlaybackItems = emptyList()
            activePlaybackItem = null
            activePlaybackSeriesParent = null
            playbackEngine.setBufferProfile(BufferProfile.VOD)
            activePlaybackQueue = buildLocalPlaybackQueue(localFiles, index)
            activePlaybackTitle = localFiles[index].displayName
            resumePositionMs = null
        }
    }

    val handlePlayItemWithPosition: (ContentItem, Long) -> Unit = { item, positionMs ->
        val config = authState.activeConfig
        if (config != null) {
            resumeFocusId = item.id
            val items = listOf(item)
            val playableItems = items.filter(::isPlayableContent)
            activePlaybackItems = playableItems
            activePlaybackItem = item
            if (item.contentType != ContentType.SERIES) {
                activePlaybackSeriesParent = null
            }
            resumePositionMs = if (positionMs > 0) positionMs else null
            playbackEngine.setBufferProfile(BufferProfile.VOD)
            val queue = buildPlaybackQueue(items, item, config)
            activePlaybackQueue = queue
            activePlaybackTitle = queue.items.getOrNull(queue.startIndex)?.title ?: item.title
            coroutineScope.launch { historyRepository.addToHistory(config, item) }
        }
    }

    val handlePlayItemWithPositionAndQueue: (ContentItem, List<ContentItem>, Long?) -> Unit =
            { item, items, positionMs ->
                val config = authState.activeConfig
                if (config != null) {
                    resumeFocusId = item.id
                    val playableItems = items.filter(::isPlayableContent)
                    activePlaybackItems = playableItems
                    activePlaybackItem = item
                    if (item.contentType != ContentType.SERIES) {
                        activePlaybackSeriesParent = null
                    }
                    resumePositionMs = positionMs?.takeIf { it > 0 }
                    val profile =
                            if (item.contentType == ContentType.LIVE) {
                                BufferProfile.LIVE
                            } else {
                                BufferProfile.VOD
                            }
                    playbackEngine.setBufferProfile(profile)
                    val queue = buildPlaybackQueue(items, item, config)
                    activePlaybackQueue = queue
                    activePlaybackTitle = queue.items.getOrNull(queue.startIndex)?.title ?: item.title
                    coroutineScope.launch { historyRepository.addToHistory(config, item) }
                }
            }

    val handleToggleFavorite: (ContentItem) -> Unit =
            handleToggleFavorite@{ item ->
                val config = authState.activeConfig ?: return@handleToggleFavorite
                coroutineScope.launch {
                    val added = favoritesRepository.toggleContentFavorite(config, item)
                    val message = if (added) "Added to favorites" else "Removed from favorites"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }

    val handleToggleCategoryFavorite: (CategoryItem) -> Unit =
            handleToggleCategoryFavorite@{ category ->
                val config = authState.activeConfig ?: return@handleToggleCategoryFavorite
                coroutineScope.launch {
                    val added = favoritesRepository.toggleCategoryFavorite(config, category)
                    val message = if (added) "Added to favorites" else "Removed from favorites"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }

    val filteredFavoriteContentKeys =
            remember(favoriteContentKeys, activeConfig) {
                if (activeConfig == null) {
                    emptySet()
                } else {
                    favoritesRepository.filterKeysForConfig(favoriteContentKeys, activeConfig)
                }
            }
    val filteredFavoriteCategoryKeys =
            remember(favoriteCategoryKeys, activeConfig) {
                if (activeConfig == null) {
                    emptySet()
                } else {
                    favoritesRepository.filterKeysForConfig(favoriteCategoryKeys, activeConfig)
                }
            }
    val filteredFavoriteContentItems =
            remember(favoriteContentEntries, filteredFavoriteContentKeys, activeConfig) {
                if (activeConfig == null) {
                    emptyList()
                } else {
                    favoriteContentEntries
                            .filter {
                                favoritesRepository.isKeyForConfig(it.key, activeConfig) &&
                                        filteredFavoriteContentKeys.contains(it.key)
                            }
                            .map { it.item }
                            .distinctBy { "${it.contentType.name}:${it.id}" }
                }
            }
    val filteredFavoriteCategoryItems =
            remember(favoriteCategoryEntries, filteredFavoriteCategoryKeys, activeConfig) {
                if (activeConfig == null) {
                    emptyList()
                } else {
                    favoriteCategoryEntries
                            .filter {
                                favoritesRepository.isKeyForConfig(it.key, activeConfig) &&
                                        filteredFavoriteCategoryKeys.contains(it.key)
                            }
                            .map { it.category }
                            .distinctBy { "${it.type.name}:${it.id}" }
                }
            }
    val isContentFavorite: (ContentItem) -> Boolean = { item ->
        val config = authState.activeConfig
        config != null && favoritesRepository.isContentFavorite(favoriteContentKeys, config, item)
    }
    val isCategoryFavorite: (CategoryItem) -> Boolean = { category ->
        val config = authState.activeConfig
        config != null &&
                favoritesRepository.isCategoryFavorite(favoriteCategoryKeys, config, category)
    }

    val filteredContinueWatchingItems =
            remember(continueWatchingEntries, activeConfig) {
                if (activeConfig == null) {
                    emptyList()
                } else {
                    continueWatchingEntries.filter {
                        continueWatchingRepository.isEntryForConfig(it, activeConfig)
                    }
                }
            }

    fun savePlaybackProgress() {
        val config = authState.activeConfig
        val item = activePlaybackItem
        if (config != null &&
                        item != null &&
                        (item.contentType == ContentType.MOVIES ||
                                item.contentType == ContentType.SERIES)
        ) {
            val player: ExoPlayer? = playbackEngine.player
            val safePlayer = player ?: return
            val position = safePlayer.currentPosition
            val duration = safePlayer.duration
            if (position > 0) {
                val progressPercent = if (duration > 0) (position * 100) / duration else 0
                coroutineScope.launch {
                    if (duration > 0 && progressPercent >= 90) {
                        continueWatchingRepository.removeEntry(config, item)
                    } else {
                        val parentItem =
                                if (item.contentType == ContentType.SERIES) {
                                    activePlaybackSeriesParent
                                } else {
                                    null
                                }
                        continueWatchingRepository.updateProgress(
                                config,
                                item,
                                position,
                                duration,
                                parentItem = parentItem
                        )
                    }
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val latestPlaybackItem by rememberUpdatedState(activePlaybackItem)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val player: ExoPlayer? = playbackEngine.player
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    val item = latestPlaybackItem
                    if (item != null &&
                                    (item.contentType == ContentType.MOVIES ||
                                            item.contentType == ContentType.SERIES)
                    ) {
                        if (player == null) return@LifecycleEventObserver
                        val position = player.currentPosition
                        val duration = player.duration
                        if (duration > 0 && position > 0) {
                            pendingResume =
                                    PendingResume(
                                            mediaId = item.id,
                                            positionMs = position,
                                            shouldPlay = player.isPlaying
                                    )
                        }
                        savePlaybackProgress()
                        player.playWhenReady = false
                    }
                }
                Lifecycle.Event.ON_START -> {
                    val item = latestPlaybackItem
                    val resume = pendingResume
                    if (item != null &&
                                    resume != null &&
                                    resume.mediaId == item.id &&
                                    (item.contentType == ContentType.MOVIES ||
                                            item.contentType == ContentType.SERIES)
                    ) {
                        if (player == null) return@LifecycleEventObserver
                        player.seekTo(resume.positionMs)
                        player.playWhenReady = resume.shouldPlay
                        pendingResume = null
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Periodic playback tracking
    LaunchedEffect(activePlaybackQueue, activePlaybackItem) {
        while (activePlaybackQueue != null && activePlaybackItem != null) {
            delay(10_000)
            savePlaybackProgress()
        }
    }

    LaunchedEffect(activePlaybackQueue, pendingPlayerReset) {
        if (activePlaybackQueue == null && pendingPlayerReset) {
            if (activePlaybackQueue == null && pendingPlayerReset) {
                playbackEngine.reset()
                playerResetNonce++
            }
            pendingPlayerReset = false
        }
    }

    PlaybackSyncEffects(
        activePlaybackQueue = activePlaybackQueue,
        syncState = syncState,
        progressiveSyncCoordinator = progressiveSyncCoordinator,
        syncPausedForPlayback = syncPausedForPlayback,
        setSyncPausedForPlayback = { syncPausedForPlayback = it },
        librarySyncJob = librarySyncJob,
        setLibrarySyncJob = { librarySyncJob = it },
        pendingLibrarySync = pendingLibrarySync,
        setPendingLibrarySync = { pendingLibrarySync = it },
        lastLibrarySyncRequest = lastLibrarySyncRequest,
        sectionSyncStates = sectionSyncStates,
        activeConfig = authState.activeConfig,
        triggerLibrarySync = ::triggerLibrarySync
    )

    fun attemptLiveReconnect(): Boolean {
        val item = activePlaybackItem
        if (item?.contentType != ContentType.LIVE) return false
        if (liveReconnectAttempts >= LIVE_RECONNECT_MAX_ATTEMPTS) return false
        if (liveReconnectJob?.isActive == true) return true
        liveReconnectAttempts += 1
        Toast.makeText(context, "Reconnecting...", Toast.LENGTH_SHORT).show()
        liveReconnectJob =
                coroutineScope.launch {
                    delay(LIVE_RECONNECT_DELAY_MS)
                    val player: ExoPlayer? = playbackEngine.player
                    player?.prepare()
                    player?.playWhenReady = true
                }
        return true
    }

    val latestPlaybackSettingsController by rememberUpdatedState(playbackSettingsController)

    DisposableEffect(playbackEngine, playerResetNonce) {
        val listener =
                object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val title = mediaItem?.mediaMetadata?.title?.toString()
                        if (!title.isNullOrBlank()) {
                            activePlaybackTitle = title
                        }
                        val currentIndex = playbackEngine.player.currentMediaItemIndex
                        if (currentIndex < 0 || activePlaybackItems.isEmpty()) return
                        val queueItems = activePlaybackQueue?.items
                        if (queueItems != null && queueItems.size != activePlaybackItems.size) {
                            Timber.w(
                                    "Playback queue mismatch: queue=${queueItems.size} items=${activePlaybackItems.size} index=$currentIndex"
                            )
                            return
                        }
                        val safeIndex = currentIndex.coerceIn(0, activePlaybackItems.lastIndex)
                        if (safeIndex != currentIndex) {
                            Timber.w("Clamped playback index $currentIndex to $safeIndex")
                        }
                        activePlaybackItem = activePlaybackItems[safeIndex]
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY && !playbackEngine.player.isPlaying
                        ) {
                            savePlaybackProgress()
                        }
                        if (playbackState == Player.STATE_READY &&
                                        activePlaybackItem?.contentType == ContentType.LIVE
                        ) {
                            liveReconnectAttempts = 0
                            liveReconnectJob?.cancel()
                            liveReconnectJob = null
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (attemptLiveReconnect()) {
                            return
                        }
                        pendingPlayerReset = true
                        val currentFormat =
                                playbackEngine.player.videoFormat
                        val isHevc =
                                currentFormat?.sampleMimeType?.equals(
                                        MimeTypes.VIDEO_H265,
                                        ignoreCase = true
                                ) == true ||
                                        currentFormat?.codecs?.contains("hvc1", ignoreCase = true) == true ||
                                        currentFormat?.codecs?.contains("hev1", ignoreCase = true) == true ||
                                        errorIndicatesHevc(error)
                        if (isHevc && !isHevcDecodeSupported()) {
                            Toast.makeText(
                                            context,
                                            "HEVC not supported. Use Next/Previous to continue.",
                                            Toast.LENGTH_LONG
                            )
                                    .show()
                            Timber.e(error, "Playback failed: HEVC not supported on this device")
                            // Don't return - allow Next/Previous navigation even for HEVC errors
                        }
                        val mediaId = playbackEngine.player.currentMediaItem?.mediaId
                        if (mediaId != null) {
                            val candidates = activePlaybackQueue?.fallbackUris?.get(mediaId).orEmpty()
                            val attempt = playbackFallbackAttempts[mediaId] ?: 0
                            val nextAttempt = attempt + 1
                            if (nextAttempt < candidates.size) {
                                playbackFallbackAttempts = playbackFallbackAttempts + (mediaId to nextAttempt)
                                val nextUri = candidates[nextAttempt]
                                Timber.w(
                                        error,
                                        "Playback failed for $mediaId, retrying with fallback ${nextAttempt + 1}/${candidates.size}: $nextUri"
                                )
                                val currentItem = playbackEngine.player.currentMediaItem
                                if (currentItem != null) {
                                    playbackEngine.player.setMediaItem(
                                            currentItem.buildUpon()
                                                    .setUri(nextUri)
                                                    .setMimeType(guessMimeTypeForUri(nextUri))
                                                    .build()
                                    )
                                    playbackEngine.player.prepare()
                                    playbackEngine.player.playWhenReady = true
                                } else {
                                    Timber.w("Current item is null during fallback retry")
                                }
                            } else {
                                Timber.e(error, "Playback failed for $mediaId; no more fallbacks")
                                Toast.makeText(
                                                context,
                                                "Playback failed. Use Next/Previous to continue.",
                                                Toast.LENGTH_LONG
                                )
                                        .show()
                            }
                        } else {
                            Timber.w(error, "Playback error with null mediaId")
                            Toast.makeText(
                                            context,
                                            "Playback failed. Use Next/Previous to continue.",
                                            Toast.LENGTH_LONG
                            )
                                    .show()
                        }
                        // Player is now in error state but Next/Previous navigation will still work
                    }
                }
        playbackEngine.player.addListener(listener)
        latestPlaybackSettingsController.bind(playbackEngine)
        onDispose {
            playbackEngine.player.removeListener(listener)
            latestPlaybackSettingsController.unbind(playbackEngine)
            // NOTE: Don't release player here - it's released in Activity.onDestroy()
        }
    }

    LaunchedEffect(settings) { playbackSettingsController.apply(settings) }

    XtreamPlayerTheme(appTheme = settings.appTheme, fontFamily = settings.appFont.fontFamily) {
        val baseDensity = LocalDensity.current
        val uiScale = settings.uiScale.coerceIn(0.7f, 1.3f)
        val fontScale = settings.fontScale.coerceIn(0.7f, 1.4f)
        val appScale = remember(uiScale, fontScale) { AppScale(uiScale, fontScale) }
        val scaledDensity = Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale * uiScale * fontScale
        )

        CompositionLocalProvider(
            LocalAppBaseDensity provides baseDensity,
            LocalAppScale provides appScale,
            LocalDensity provides scaledDensity
        ) {
            AppBackground {
        val shouldAutoSignIn =
                settings.autoSignIn && settings.rememberLogin && savedConfig != null
        val isWaitingForSavedConfig =
                !savedConfigLoaded && settings.autoSignIn && settings.rememberLogin
        val showAuthLoading =
                !authState.isSignedIn &&
                        !authState.isEditingList &&
                        authState.errorMessage == null &&
                        (authState.isLoading ||
                                (!authState.autoSignInSuppressed &&
                                        (shouldAutoSignIn || isWaitingForSavedConfig)))

        if (showAuthLoading) {
            AuthLoadingScreen()
        } else if (authState.isSignedIn) {
            val colors = AppTheme.colors
            val context = LocalContext.current
            val versionLabel = remember(context) { "v${appVersionName(context)}" }
            val quickSearchReady by produceState(
                    initialValue = false,
                    key1 = authState.activeConfig,
                    key2 = syncState.fastStartReady,
                    key3 = cacheClearNonce
            ) {
                val config = authState.activeConfig
                value = if (config == null) {
                    false
                } else {
                    contentRepository.hasAnySearchIndex(config)
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(72.dp)
                                        .padding(start = 20.dp, top = 12.dp)
                ) {
                    MenuButton(
                            expanded = navExpanded,
                            onToggle = {
                                navExpanded = !navExpanded
                                // Focus stays on menu button - user navigates manually
                            },
                            onMoveRight = { focusToContentTrigger++ }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                            text = versionLabel,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = settings.appFont.fontFamily,
                            modifier = Modifier
                                    .padding(end = 12.dp, bottom = 2.dp)
                                    .align(Alignment.CenterVertically)
                    )
                }

                val isPlaybackActiveLocal = activePlaybackQueue != null
                val isProgressiveSyncActive =
                    syncState.phase == com.example.xtreamplayer.content.SyncPhase.FAST_START ||
                        syncState.phase == com.example.xtreamplayer.content.SyncPhase.BACKGROUND_FULL ||
                        syncState.phase == com.example.xtreamplayer.content.SyncPhase.ON_DEMAND_BOOST ||
                        syncState.phase == com.example.xtreamplayer.content.SyncPhase.PAUSED
                val isLegacySyncActive = sectionSyncStates.values.any { it.isActive }
                val shouldShowSyncUi =
                    !isPlaybackActiveLocal && (isProgressiveSyncActive || isLegacySyncActive)

                // Progressive sync status indicators
                if (shouldShowSyncUi) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.TopEnd),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Fast Search Ready indicator (only while sync is active)
                            if (quickSearchReady) {
                                Row(
                                    modifier =
                                        Modifier.background(
                                                Color(0xFF2E7D32),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Quick Search Ready",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontFamily = AppTheme.fontFamily
                                    )
                                }
                            }

                            // Background/boost syncing indicator
                            if (isProgressiveSyncActive) {
                                Row(
                                    modifier =
                                        Modifier.background(
                                                Color(0xFF424242),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .clickable {
                                                coroutineScope.launch {
                                                    if (syncState.isPaused) {
                                                        progressiveSyncCoordinator?.resumeBackgroundSync()
                                                    } else {
                                                        progressiveSyncCoordinator?.pauseBackgroundSync()
                                                    }
                                                }
                                            },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    val currentSection = syncState.currentSection
                                    val progress =
                                        currentSection?.let { syncState.sectionProgress[it] }
                                    val text =
                                        if (syncState.isPaused) {
                                            "Sync paused"
                                        } else if (currentSection != null && progress != null) {
                                            "Syncing ${currentSection.name.lowercase()}... (${progress.itemsIndexed} items)"
                                        } else {
                                            "Syncing library..."
                                        }
                                    Text(
                                        text,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontFamily = AppTheme.fontFamily
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = if (syncState.isPaused) "Resume" else "Pause",
                                        fontSize = 11.sp,
                                        color = Color(0xFF81C784),
                                        fontFamily = AppTheme.fontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // Removed extra long sync banner; top-right pill is the only sync indicator.

                val navProgress by animateFloatAsState(
                        targetValue = if (navSlideExpanded) 1f else 0f,
                        animationSpec = tween(durationMillis = NAV_ANIM_DURATION_MS),
                        label = "navSlide"
                )
                val navWidthPx = with(LocalDensity.current) { NAV_WIDTH.toPx() }
                val navOffsetPx = -navWidthPx * (1f - navProgress)

                BrowseScreen(
                        context = context,
                        coroutineScope = coroutineScope,
                        authState = authState,
                        savedConfig = savedConfig,
                        activeConfig = activeConfig,
                        settings = settings,
                        settingsViewModel = settingsViewModel,
                        appVersionName = appVersionName,
                        selectedSectionState = browseViewModel.selectedSection,
                        navExpandedState = browseViewModel.navExpanded,
                        navLayoutExpanded = navLayoutExpanded,
                        navSlideExpanded = navSlideExpanded,
                        navOffsetPx = navOffsetPx,
                        navProgress = navProgress,
                        moveFocusToNavState = moveFocusToNavState,
                        focusToContentTriggerState = focusToContentTriggerState,
                        showManageListsState = showManageListsState,
                        showAppearanceState = showAppearanceState,
                        showThemeDialogState = showThemeDialogState,
                        showFontDialogState = showFontDialogState,
                        showUiScaleDialogState = showUiScaleDialogState,
                        showFontScaleDialogState = showFontScaleDialogState,
                        showNextEpisodeThresholdDialogState = showNextEpisodeThresholdDialogState,
                        showApiKeyDialogState = showApiKeyDialogState,
                        cacheClearNonceState = cacheClearNonceState,
                        contentRepository = contentRepository,
                        favoritesRepository = favoritesRepository,
                        historyRepository = historyRepository,
                        continueWatchingRepository = continueWatchingRepository,
                        subtitleRepository = subtitleRepository,
                        playbackEngine = playbackEngine,
                        progressiveSyncCoordinator = progressiveSyncCoordinator,
                        syncState = syncState,
                        storagePermissionLauncher = storagePermissionLauncher,
                        localFiles = localFiles,
                        allNavItemFocusRequester = allNavItemFocusRequester,
                        continueWatchingNavItemFocusRequester = continueWatchingNavItemFocusRequester,
                        favoritesNavItemFocusRequester = favoritesNavItemFocusRequester,
                        moviesNavItemFocusRequester = moviesNavItemFocusRequester,
                        seriesNavItemFocusRequester = seriesNavItemFocusRequester,
                        liveNavItemFocusRequester = liveNavItemFocusRequester,
                        categoriesNavItemFocusRequester = categoriesNavItemFocusRequester,
                        localFilesNavItemFocusRequester = localFilesNavItemFocusRequester,
                        settingsNavItemFocusRequester = settingsNavItemFocusRequester,
                        contentItemFocusRequester = contentItemFocusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        filteredContinueWatchingItems = filteredContinueWatchingItems,
                        filteredFavoriteContentItems = filteredFavoriteContentItems,
                        filteredFavoriteCategoryItems = filteredFavoriteCategoryItems,
                        filteredFavoriteContentKeys = filteredFavoriteContentKeys,
                        filteredFavoriteCategoryKeys = filteredFavoriteCategoryKeys,
                        isPlaybackActive = activePlaybackQueue != null,
                        onItemFocused = handleItemFocused,
                        onPlay = handlePlayItem,
                        onPlayWithPosition = handlePlayItemWithPosition,
                        onPlayContinueWatching = { item, position, parent ->
                            activePlaybackSeriesParent = parent
                            handlePlayItemWithPosition(item, position)
                        },
                        onPlayWithPositionAndQueue = handlePlayItemWithPositionAndQueue,
                        onMovieInfo = openMovieInfo,
                        onMovieInfoContinueWatching = openMovieInfoFromContinueWatching,
                        onPlayLocalFile = handlePlayLocalFile,
                        onToggleFavorite = handleToggleFavorite,
                        onToggleCategoryFavorite = handleToggleCategoryFavorite,
                        isItemFavorite = isContentFavorite,
                        isCategoryFavorite = isCategoryFavorite,
                        onSeriesPlaybackStart = { activePlaybackSeriesParent = it },
                        onTriggerSectionSync = { section, config ->
                            triggerSectionSync(section, config)
                        },
                        onEditList = {
                            coroutineScope.launch {
                                contentRepository.clearCache()
                                contentRepository.clearDiskCache()
                            }
                            authViewModel.enterEditMode()
                        },
                        onSignOutKeepSaved = {
                            coroutineScope.launch {
                                contentRepository.clearCache()
                                contentRepository.clearDiskCache()
                            }
                            authViewModel.signOut(keepSaved = true)
                        },
                        onSignOutForget = {
                            coroutineScope.launch {
                                contentRepository.clearCache()
                                contentRepository.clearDiskCache()
                            }
                            authViewModel.signOut(keepSaved = false)
                        },
                        onCheckForUpdates = { checkForUpdates() },
                        hasStoragePermission = ::hasStoragePermission,
                        scanMediaStoreMedia = ::scanMediaStoreMedia,
                        getRequiredMediaPermissions = ::getRequiredMediaPermissions
                )

                if (showThemeDialog) {
                    ThemeSelectionDialog(
                        themes = com.example.xtreamplayer.settings.AppThemeOption.values().toList(),
                        currentTheme = settings.appTheme,
                        onThemeSelected = { settingsViewModel.setAppTheme(it) },
                        onDismiss = { showThemeDialog = false }
                    )
                }

                if (showFontDialog) {
                    FontSelectionDialog(
                        fonts = com.example.xtreamplayer.ui.theme.AppFont.values().toList(),
                        currentFont = settings.appFont,
                        onFontSelected = { settingsViewModel.setAppFont(it) },
                        onDismiss = { showFontDialog = false }
                    )
                }

                if (showNextEpisodeThresholdDialog) {
                    NextEpisodeThresholdDialog(
                        currentSeconds = settings.nextEpisodeThresholdSeconds,
                        onSecondsChange = { settingsViewModel.setNextEpisodeThreshold(it) },
                        onDismiss = { showNextEpisodeThresholdDialog = false }
                    )
                }

                if (showApiKeyDialog) {
                    ApiKeyInputDialog(
                        currentKey = settings.openSubtitlesApiKey,
                        currentUserAgent = settings.openSubtitlesUserAgent,
                        onSave = { apiKey, userAgent ->
                            settingsViewModel.setOpenSubtitlesApiKey(apiKey)
                            settingsViewModel.setOpenSubtitlesUserAgent(userAgent)
                            Toast.makeText(
                                context,
                                "OpenSubtitles settings saved",
                                Toast.LENGTH_SHORT
                            ).show()
                            showApiKeyDialog = false
                        },
                        onDismiss = { showApiKeyDialog = false }
                    )
                }

                val pendingRelease = updateUiState.pendingRelease
                if (updateUiState.showDialog && pendingRelease != null) {
                    UpdatePromptDialog(
                        release = pendingRelease,
                        isDownloading = updateUiState.inProgress,
                        onUpdate = { startUpdateDownload(pendingRelease) },
                        onLater = { updateUiState = updateUiState.copy(showDialog = false) }
                    )
                }
                }

        } else {
            if (showLocalFilesGuest) {
                BackHandler(enabled = true) { showLocalFilesGuest = false }
                LocalFilesScreen(
                        title = "PLAY LOCAL FILES",
                        settings = settings,
                        files = localFiles,
                        contentItemFocusRequester = contentItemFocusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        onPickFiles = {
                            if (hasStoragePermission(context)) {
                                scanLocalMedia(
                                        replaceExisting = false,
                                        emptyMessage = "No media files found on device",
                                        successMessage = { videoCount, audioCount ->
                                            "Found $videoCount video(s), $audioCount audio file(s)"
                                        }
                                )
                            } else {
                                storagePermissionLauncher.launch(getRequiredMediaPermissions())
                            }
                        },
                        onRefresh = {
                            if (hasStoragePermission(context)) {
                                scanLocalMedia(
                                        replaceExisting = true,
                                        emptyMessage = "No media files found",
                                        successMessage = { videoCount, audioCount ->
                                            "Refreshed: $videoCount video(s), $audioCount audio file(s)"
                                        }
                                )
                            } else {
                                Toast.makeText(
                                                context,
                                                "Scan for media first to grant permissions",
                                                Toast.LENGTH_SHORT
                                )
                                        .show()
                            }
                        },
                        onPlayFile = handlePlayLocalFile,
                        onMoveLeft = { showLocalFilesGuest = false },
                        showBackButton = true,
                        onBack = { showLocalFilesGuest = false }
                )
            } else {
                LoginScreen(
                        authState = authState,
                        initialConfig = authState.activeConfig ?: savedConfig,
                        onSignIn = { listName, baseUrl, username, password ->
                            authViewModel.signIn(
                                    listName = listName,
                                    baseUrl = baseUrl,
                                    username = username,
                                    password = password,
                                    rememberLogin = settings.rememberLogin
                            )
                        },
                        onOpenLocalFiles = {
                            showLocalFilesGuest = true
                            focusToContentTrigger++
                        }
                )
            }
        }

        if (showUiScaleDialog) {
            UiScaleDialog(
                currentScale = settings.uiScale,
                onScaleChange = { settingsViewModel.setUiScale(it) },
                onDismiss = { showUiScaleDialog = false }
            )
        }

        if (showFontScaleDialog) {
            FontScaleDialog(
                currentScale = settings.fontScale,
                onScaleChange = { settingsViewModel.setFontScale(it) },
                onDismiss = { showFontScaleDialog = false }
            )
        }

        PlayerScreen(
                activePlaybackQueue = activePlaybackQueue,
                activePlaybackTitle = activePlaybackTitle,
                activePlaybackItem = activePlaybackItem,
                playbackEngine = playbackEngine,
                subtitleRepository = subtitleRepository,
                settings = settings,
                onRequestOpenSubtitlesApiKey = { showApiKeyDialog = true },
                onExitPlayback = {
                    savePlaybackProgress()
                    activePlaybackQueue = null
                    activePlaybackTitle = null
                    activePlaybackItem = null
                    activePlaybackSeriesParent = null
                    resumePositionMs = null
                },
                onPlayNextEpisode = { playbackEngine.player.seekToNextMediaItem() },
                onLiveChannelSwitch = switchLiveChannel,
                loadLiveNowNext = loadLiveNowNext@{ item ->
                    val config = authState.activeConfig ?: return@loadLiveNowNext Result.success(null)
                    if (item.contentType != ContentType.LIVE) {
                        return@loadLiveNowNext Result.success(null)
                    }
                    contentRepository.loadLiveNowNext(
                        streamId = item.streamId,
                        authConfig = config
                    )
                }
        )

        if (movieInfoItem != null) {
            val item = movieInfoItem!!
            val isInContinueWatching =
                    continueWatchingEntries.any {
                        it.item.contentType == ContentType.MOVIES &&
                                (it.item.id == item.id || it.item.streamId == item.streamId)
                    }
            MovieInfoDialog(
                    item = item,
                    info = movieInfoInfo,
                    queueItems = movieInfoQueue,
                    isFavorite = isContentFavorite(item),
                    onToggleFavorite = { handleToggleFavorite(it) },
                    onPlay = { selected, queue ->
                        movieInfoItem = null
                        movieInfoInfo = null
                        movieInfoFromContinueWatching = false
                        movieInfoResumePositionMs = null
                        handlePlayItem(selected, queue)
                    },
                    onPlayWithPosition = { selected, queue, position ->
                        movieInfoItem = null
                        movieInfoInfo = null
                        movieInfoFromContinueWatching = false
                        movieInfoResumePositionMs = null
                        handlePlayItemWithPositionAndQueue(selected, queue, position)
                    },
                    resumePositionMs = movieInfoResumePositionMs,
                    showClearContinueWatching = movieInfoFromContinueWatching && isInContinueWatching,
                    onClearContinueWatching =
                            if (isInContinueWatching) {
                                {
                                    val config = authState.activeConfig
                                    if (config != null) {
                                        coroutineScope.launch {
                                            continueWatchingRepository.removeEntry(config, item)
                                            Toast.makeText(
                                                            context,
                                                            "Removed from Continue Watching",
                                                            Toast.LENGTH_SHORT
                                            )
                                                    .show()
                                        }
                                    }
                                }
                            } else {
                                null
                    },
                    onDismiss = {
                        movieInfoItem = null
                        movieInfoInfo = null
                        movieInfoFromContinueWatching = false
                        movieInfoResumePositionMs = null
                    }
            )
        }
    }
    }
    }
}

@Composable
private fun PlaybackSyncEffects(
        activePlaybackQueue: PlaybackQueue?,
        syncState: com.example.xtreamplayer.content.ProgressiveSyncState,
        progressiveSyncCoordinator: com.example.xtreamplayer.content.ProgressiveSyncCoordinator?,
        syncPausedForPlayback: Boolean,
        setSyncPausedForPlayback: (Boolean) -> Unit,
        librarySyncJob: Job?,
        setLibrarySyncJob: (Job?) -> Unit,
        pendingLibrarySync: LibrarySyncRequest?,
        setPendingLibrarySync: (LibrarySyncRequest?) -> Unit,
        lastLibrarySyncRequest: LibrarySyncRequest?,
        sectionSyncStates: MutableMap<Section, SectionSyncState>,
        activeConfig: AuthConfig?,
        triggerLibrarySync: (AuthConfig, String, Boolean, List<Section>?) -> Unit
) {
    LaunchedEffect(activePlaybackQueue, syncState.phase, pendingLibrarySync, activeConfig, syncPausedForPlayback) {
        if (activePlaybackQueue != null) {
            val shouldPause =
                    syncState.phase == com.example.xtreamplayer.content.SyncPhase.FAST_START ||
                            syncState.phase == com.example.xtreamplayer.content.SyncPhase.BACKGROUND_FULL ||
                            syncState.phase == com.example.xtreamplayer.content.SyncPhase.ON_DEMAND_BOOST
            if (!syncPausedForPlayback && shouldPause) {
                progressiveSyncCoordinator?.pauseAllForPlayback()
                setSyncPausedForPlayback(true)
            }
            val job = librarySyncJob
            if (job?.isActive == true) {
                val fallbackRequest =
                        activeConfig?.let {
                            LibrarySyncRequest(
                                    config = it,
                                    reason = "Syncing library",
                                    force = false,
                                    sectionsToSync = null
                            )
                        }
                val request = lastLibrarySyncRequest ?: fallbackRequest
                if (request != null) {
                    setPendingLibrarySync(request)
                }
                job.cancel()
                setLibrarySyncJob(null)
                sectionSyncStates.clear()
            }
        } else {
            if (syncPausedForPlayback) {
                setSyncPausedForPlayback(false)
                progressiveSyncCoordinator?.resumeAfterPlayback()
            }
            val request = pendingLibrarySync
            if (request != null) {
                setPendingLibrarySync(null)
                triggerLibrarySync(
                        request.config,
                        "Resuming library sync",
                        request.force,
                        request.sectionsToSync
                )
            }
        }
    }
}

@Composable
private fun AuthLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
                text = "XTREAM PLAYER",
                color = AppTheme.colors.textPrimary,
                fontSize = 26.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
        )
    }
}

private fun isPlayableContent(item: ContentItem): Boolean {
    return item.contentType != ContentType.SERIES || !item.containerExtension.isNullOrBlank()
}

private fun buildPlaybackQueue(
        items: List<ContentItem>,
        current: ContentItem,
        authConfig: AuthConfig
): PlaybackQueue {
    val playableItems = items.filter(::isPlayableContent).toMutableList()
    if (playableItems.none { it.id == current.id }) {
        playableItems.add(current)
    }
    val startIndex =
            playableItems.indexOfFirst { it.id == current.id }.let { index ->
                if (index >= 0) index else 0
            }
    val fallbackUris = LinkedHashMap<String, List<Uri>>()
    val queueItems =
            playableItems.map { item ->
                val candidates =
                        StreamUrlBuilder.buildCandidates(
                                config = authConfig,
                                type = item.contentType,
                                streamId = item.streamId,
                                extension = item.containerExtension
                        )
                val uris = candidates.map(Uri::parse)
                PlaybackQueueItem(
                        mediaId = "${item.contentType.name}:${item.id}",
                        title = item.title,
                        type = item.contentType,
                        uri = uris.first()
                )
                    .also { queueItem -> fallbackUris[queueItem.mediaId] = uris }
            }
    return PlaybackQueue(queueItems, startIndex, fallbackUris)
}

private fun buildLocalPlaybackQueue(items: List<LocalFileItem>, startIndex: Int): PlaybackQueue {
    if (items.isEmpty()) {
        return PlaybackQueue(emptyList(), 0)
    }
    val safeIndex = startIndex.coerceIn(0, items.lastIndex)
    val queueItems =
            items.map { item ->
                PlaybackQueueItem(
                        mediaId = "local:${item.uri}",
                        title = item.displayName,
                        type = ContentType.MOVIES,
                        uri = item.uri
                )
            }
    return PlaybackQueue(queueItems, safeIndex)
}

private fun collectUrisFromResult(data: Intent?): List<Uri> {
    if (data == null) return emptyList()
    val uris = mutableListOf<Uri>()
    data.data?.let { uris.add(it) }
    val clip = data.clipData
    if (clip != null) {
        for (i in 0 until clip.itemCount) {
            uris.add(clip.getItemAt(i).uri)
        }
    }
    return uris.distinct()
}

private fun resolveLocalFileName(context: Context, uri: Uri): String {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            val name = cursor.getString(nameIndex)
            if (!name.isNullOrBlank()) {
                return name
            }
        }
    }
    return uri.lastPathSegment ?: "Local file"
}

private fun getVolumeDisplayName(context: Context, volumeName: String): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val storageManager =
                context.getSystemService(Context.STORAGE_SERVICE) as?
                        android.os.storage.StorageManager
        storageManager?.storageVolumes?.forEach { volume ->
            val uuid = volume.uuid
            // Match by UUID or check if it's primary
            if (volumeName == "external_primary" && volume.isPrimary) {
                return volume.getDescription(context) ?: "Internal Storage"
            }
            if (uuid != null && volumeName.contains(uuid, ignoreCase = true)) {
                return volume.getDescription(context) ?: "External Storage"
            }
        }
    }
    // Fallback display names
    return when {
        volumeName == "external" || volumeName == "external_primary" -> "Internal Storage"
        else -> "USB Storage"
    }
}

private fun scanMediaStoreMedia(context: Context): List<LocalFileItem> {
    val mediaFiles = mutableListOf<LocalFileItem>()
    val seenVideoIds = mutableSetOf<Long>()
    val seenAudioIds = mutableSetOf<Long>()

    // Get all volume names (includes USB drives on API 29+)
    val volumeNames =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.getExternalVolumeNames(context)
            } else {
                setOf("external")
            }

    // Query each volume for videos and audio
    for (volumeName in volumeNames) {
        val displayVolumeName = getVolumeDisplayName(context, volumeName)

        // Scan videos
        val videoCollection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(volumeName)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
        val videoProjection =
                arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME)
        val videoSortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        runCatching {
            context.contentResolver.query(
                            videoCollection,
                            videoProjection,
                            null,
                            null,
                            videoSortOrder
                    )
                    ?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val nameColumn =
                                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            if (id in seenVideoIds) continue
                            seenVideoIds.add(id)
                            val name = cursor.getString(nameColumn) ?: "Video"
                            val contentUri =
                                    android.content.ContentUris.withAppendedId(videoCollection, id)
                            mediaFiles.add(
                                    LocalFileItem(
                                            uri = contentUri,
                                            displayName = name,
                                            volumeName = displayVolumeName,
                                            mediaType = LocalMediaType.VIDEO
                                    )
                            )
                        }
                    }
        }

        // Scan audio
        val audioCollection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(volumeName)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
        val audioProjection =
                arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
        val audioSortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        runCatching {
            context.contentResolver.query(
                            audioCollection,
                            audioProjection,
                            null,
                            null,
                            audioSortOrder
                    )
                    ?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val nameColumn =
                                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            if (id in seenAudioIds) continue
                            seenAudioIds.add(id)
                            val name = cursor.getString(nameColumn) ?: "Audio"
                            val contentUri =
                                    android.content.ContentUris.withAppendedId(audioCollection, id)
                            mediaFiles.add(
                                    LocalFileItem(
                                            uri = contentUri,
                                            displayName = name,
                                            volumeName = displayVolumeName,
                                            mediaType = LocalMediaType.AUDIO
                                    )
                            )
                        }
                    }
        }
    }

    // Sort by volume name first, then by display name
    return mediaFiles.sortedWith(compareBy({ it.volumeName }, { it.displayName }))
}

private fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
    }
}

private fun getRequiredMediaPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun isLightTheme(colors: AppColors): Boolean {
    return colors.background.luminance() > 0.5f
}

private fun cardTitleColor(colors: AppColors): Color {
    return if (isLightTheme(colors)) Color.White else colors.textPrimary
}

private fun cardSubtitleColor(colors: AppColors): Color {
    return if (isLightTheme(colors)) Color.White.copy(alpha = 0.72f) else colors.textSecondary
}

private fun cardTextStripColor(colors: AppColors, textColor: Color): Color {
    val lightText = textColor.luminance() > 0.6f
    return if (lightText) {
        Color.Black.copy(alpha = if (isLightTheme(colors)) 0.28f else 0.22f)
    } else {
        Color.White.copy(alpha = if (isLightTheme(colors)) 0.35f else 0.16f)
    }
}

private fun guessMimeTypeForUri(uri: Uri): String? {
    val candidate = uri.toString().lowercase()
    return if (candidate.contains(".m3u8")) {
        MimeTypes.APPLICATION_M3U8
    } else {
        null
    }
}

private fun appVersionName(context: Context): String {
    return runCatching {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val versionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            ).versionName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }
        versionName ?: "?"
    }.getOrDefault("?")
}

private fun isHevcDecodeSupported(): Boolean {
    return runCatching {
                val mediaCodecList =
                        android.media.MediaCodecList(
                                android.media.MediaCodecList.REGULAR_CODECS
                        )
                mediaCodecList.codecInfos.any { codecInfo ->
                    !codecInfo.isEncoder && codecInfo.supportedTypes.any { type ->
                        type.equals("video/hevc", ignoreCase = true) ||
                                type.equals("video/h265", ignoreCase = true)
                    }
                }
            }
            .getOrDefault(false)
}

private fun errorIndicatesHevc(error: PlaybackException): Boolean {
    val message = error.message?.lowercase().orEmpty()
    if (message.contains("video/hevc") || message.contains("hvc1") || message.contains("hev1")) {
        return true
    }
    var cause = error.cause
    while (cause != null) {
        val causeMessage = cause.message?.lowercase().orEmpty()
        if (causeMessage.contains("video/hevc") || causeMessage.contains("hvc1") || causeMessage.contains("hev1")) {
            return true
        }
        val className = cause::class.java.name
        if (className.contains("MediaCodecVideoDecoderException")) {
            return true
        }
        cause = cause.cause
    }
    return false
}

private fun scaleTextSize(size: TextUnit, scale: Float): TextUnit {
    return if (scale == 1f) size else (size.value * scale).sp
}

private const val POSTER_ASPECT_RATIO = 2f / 3f
private const val LANDSCAPE_ASPECT_RATIO = 16f / 9f
private const val NAV_ANIM_DURATION_MS = 180
private const val LIVE_RECONNECT_MAX_ATTEMPTS = 3
private const val LIVE_RECONNECT_DELAY_MS = 1_000L
private val CONTENT_HORIZONTAL_PADDING = 104.dp
private val DETAIL_ROW_SPACING = 6.dp
private val DETAIL_SECTION_SPACING = 10.dp

@Composable
private fun rememberReflowColumns(
        baseColumns: Int,
        navLayoutExpanded: Boolean,
        spacing: Dp = 16.dp,
        horizontalPadding: Dp = CONTENT_HORIZONTAL_PADDING
): Int {
    val configuration = LocalConfiguration.current
    if (baseColumns <= 1) return baseColumns
    val density = LocalDensity.current
    val spacingPx = with(density) { spacing.toPx() }
    val paddingPx = with(density) { horizontalPadding.toPx() }
    val fullWidthPx =
            with(density) { configuration.screenWidthDp.dp.toPx() }.minus(paddingPx).coerceAtLeast(1f)
    val navWidthPx = with(density) { NAV_WIDTH.toPx() }
    val baseWidthPx = (fullWidthPx - navWidthPx).coerceAtLeast(1f)
    val contentWidthPx = if (navLayoutExpanded) baseWidthPx else fullWidthPx
    val minCardWidthPx =
            (baseWidthPx - spacingPx * (baseColumns - 1)) / baseColumns.toFloat()
    if (minCardWidthPx <= 0f) return baseColumns
    val columns =
            kotlin.math.floor((contentWidthPx + spacingPx) / (minCardWidthPx + spacingPx))
                    .toInt()
    return columns.coerceAtLeast(baseColumns)
}


@Composable
private fun FavoriteIndicator(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val colors = AppTheme.colors
    val context = LocalContext.current
    Box(
            modifier =
                    modifier.size(22.dp)
                            .background(colors.overlayStrong, shape)
                            .border(1.dp, Color(0xFFEF5350), shape)
    ) {
        Icon(
                painter = painterResource(R.drawable.ic_favorite),
                contentDescription = "Favorite",
                tint = Color(0xFFEF5350),
                modifier = Modifier.align(Alignment.Center).size(14.dp)
        )
    }
}

@Composable
private fun LibrarySyncBanner(progress: Float, itemsIndexed: Int, section: Section) {
    val sectionName = when (section) {
        Section.MOVIES -> "Movies"
        Section.SERIES -> "Series"
        Section.LIVE -> "Live"
        else -> "Content"
    }

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppTheme.colors.surfaceMuted)
                            .border(1.dp, AppTheme.colors.panelBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
                text = if (itemsIndexed > 0) "Syncing $sectionName library · $itemsIndexed items" else "Syncing $sectionName library...",
                color = AppTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily,
                letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
                progress = progress.coerceIn(0.03f, 1f),
                color = AppTheme.colors.accentAlt,
                trackColor = AppTheme.colors.surfaceAlt,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
        )
    }
}

private fun formatPlaybackTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

@Composable
private fun HomeHubScreen(onSelect: (HomeDestination) -> Unit, onOpenSettings: () -> Unit) {
    val focusRequesters = remember { listOf(FocusRequester(), FocusRequester(), FocusRequester()) }
    // Focus is managed by user navigation - no auto-focus on screen load
    Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HomeCard(
                    label = "LIVE",
                    description = "Channels, now playing",
                    iconRes = R.drawable.ic_category_live,
                    brush =
                            Brush.linearGradient(
                                    colors = listOf(Color(0xFF1DD3B0), Color(0xFF0D6871))
                            ),
                    focusRequester = focusRequesters[0],
                    onActivate = { onSelect(HomeDestination.LIVE) }
            )
            HomeCard(
                    label = "MOVIES",
                    description = "Film library",
                    iconRes = R.drawable.ic_category_movies,
                    brush =
                            Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFB347), Color(0xFF8A2B2B))
                            ),
                    focusRequester = focusRequesters[1],
                    onActivate = { onSelect(HomeDestination.MOVIES) }
            )
            HomeCard(
                    label = "SERIES",
                    description = "Box sets and episodes",
                    iconRes = R.drawable.ic_category_series,
                    brush =
                            Brush.linearGradient(
                                    colors = listOf(Color(0xFF8C7BFF), Color(0xFF2D296D))
                            ),
                    focusRequester = focusRequesters[2],
                    onActivate = { onSelect(HomeDestination.SERIES) }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TopBarButton(label = "SETTINGS", onActivate = onOpenSettings)
        }
    }
}

@Composable
private fun RowScope.HomeCard(
        label: String,
        description: String,
        iconRes: Int,
        brush: Brush,
        focusRequester: FocusRequester,
        onActivate: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)
    val borderColor = if (isFocused) AppTheme.colors.focus else AppTheme.colors.borderStrong
    val backgroundBrush =
            if (isFocused) {
                brush
            } else {
                Brush.linearGradient(colors = listOf(AppTheme.colors.surfaceMuted, AppTheme.colors.surfaceMuted))
            }
    Box(
            modifier =
                    Modifier.weight(1f)
                            .height(220.dp)
                            .focusRequester(focusRequester)
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                if (it.type != KeyEventType.KeyDown) {
                                    false
                                } else if (it.key == Key.Enter ||
                                                it.key == Key.NumPadEnter ||
                                                it.key == Key.DirectionCenter
                                ) {
                                    onActivate()
                                    true
                                } else {
                                    false
                                }
                            }
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onActivate
                            )
                            .clip(shape)
                            .background(backgroundBrush)
                            .border(1.dp, borderColor, shape)
                            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                    modifier =
                            Modifier.size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                            if (isFocused) AppTheme.colors.textPrimary.copy(alpha = 0.13f) else AppTheme.colors.surfaceAlt
                                    )
            ) {
                Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint =
                                if (isFocused) AppTheme.colors.textOnAccent
                                else AppTheme.colors.textPrimary,
                        modifier = Modifier.align(Alignment.Center).size(28.dp)
                )
            }
            Text(
                    text = label,
                    color =
                            if (isFocused) AppTheme.colors.textOnAccent
                            else AppTheme.colors.textPrimary,
                    fontSize = 22.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
            )
            Text(
                    text = description,
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = AppTheme.fontFamily,
                    letterSpacing = 0.4.sp
            )
        }
    }
}

private enum class BrowserPrimaryTab {
    ALL,
    FAVORITES,
    HISTORY,
    CATEGORY
}

@Composable
private fun ContentBrowserScreen(
        contentType: ContentType,
        contentRepository: ContentRepository,
        favoritesRepository: FavoritesRepository,
        historyEntries: List<HistoryEntry>,
        favoriteContentEntries: List<FavoriteContentEntry>,
        favoriteContentKeys: Set<String>,
        authConfig: AuthConfig,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onToggleFavorite: (ContentItem) -> Unit
) {
    var primaryTab by remember { mutableStateOf(BrowserPrimaryTab.ALL) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var selectedSeries by remember { mutableStateOf<ContentItem?>(null) }
    val searchState = rememberDebouncedSearchState(key = contentType to authConfig)
    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var isLoadingCategories by remember { mutableStateOf(true) }
    var categoriesError by remember { mutableStateOf<String?>(null) }
    val searchFocusRequester = remember { FocusRequester() }
    val primaryFocusRequester = remember { FocusRequester() }
    val secondaryFocusRequester = remember { FocusRequester() }
    val previewFocusRequester = remember { FocusRequester() }

    LaunchedEffect(contentType, authConfig) {
        primaryTab = BrowserPrimaryTab.ALL
        selectedCategory = null
        selectedSeries = null
    }

    LaunchedEffect(contentType, authConfig) {
        isLoadingCategories = true
        categoriesError = null
        runCatching { contentRepository.loadCategories(contentType, authConfig) }
                .onSuccess { categories = it }
                .onFailure { categoriesError = it.message ?: "Failed to load categories" }
        isLoadingCategories = false
    }

    LaunchedEffect(primaryTab) {
        if (primaryTab != BrowserPrimaryTab.CATEGORY) {
            selectedCategory = null
            selectedSeries = null
        }
    }

    val section =
            when (contentType) {
                ContentType.LIVE -> Section.LIVE
                ContentType.MOVIES -> Section.MOVIES
                ContentType.SERIES -> Section.SERIES
            }

    val isFavorite: (ContentItem) -> Boolean = { item ->
        favoritesRepository.isContentFavorite(favoriteContentKeys, authConfig, item)
    }

    val favoriteItems =
            remember(favoriteContentEntries, authConfig, contentType) {
                favoriteContentEntries
                        .filter { favoritesRepository.isKeyForConfig(it.key, authConfig) }
                        .map { it.item }
                        .filter { it.contentType == contentType }
                        .distinctBy { "${it.contentType.name}:${it.id}" }
            }

    val historyItems =
            remember(historyEntries, authConfig, contentType) {
                historyEntries
                        .filter { favoritesRepository.isKeyForConfig(it.key, authConfig) }
                        .map { it.item }
                        .filter { it.contentType == contentType }
            }

    val normalizedQuery = searchState.debouncedQuery
    val showSearch = normalizedQuery.isNotBlank()

    val allPagerFlow =
            remember(section, authConfig, normalizedQuery) {
                if (showSearch) {
                    contentRepository.searchPager(section, normalizedQuery, authConfig).flow
                } else {
                    contentRepository.pager(section, authConfig).flow
                }
            }
    val categoryPagerFlow =
            remember(selectedCategory?.id, contentType, authConfig, normalizedQuery) {
                val category = selectedCategory
                if (category == null) {
                    null
                } else if (showSearch) {
                    contentRepository.categorySearchPager(
                                    contentType,
                                    category.id,
                                    normalizedQuery,
                                    authConfig
                            )
                            .flow
                } else {
                    contentRepository.categoryPager(contentType, category.id, authConfig).flow
                }
            }
    val seriesPagerFlow =
            remember(selectedSeries?.streamId, authConfig) {
                val series = selectedSeries
                if (series == null) null
                else contentRepository.seriesPager(series.streamId, authConfig).flow
            }

    val allLazyItems =
            if (primaryTab == BrowserPrimaryTab.ALL && selectedSeries == null) {
                allPagerFlow.collectAsLazyPagingItems()
            } else {
                null
            }
    val categoryLazyItems =
            if (primaryTab == BrowserPrimaryTab.CATEGORY &&
                            selectedCategory != null &&
                            selectedSeries == null
            ) {
                categoryPagerFlow?.collectAsLazyPagingItems()
            } else {
                null
            }
    val seriesLazyItems =
            if (selectedSeries != null) {
                seriesPagerFlow?.collectAsLazyPagingItems()
            } else {
                null
            }

    val filteredFavoritesState = remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    LaunchedEffect(favoriteItems, normalizedQuery) {
        filteredFavoritesState.value =
                if (normalizedQuery.isBlank()) {
                    favoriteItems
                } else {
                    withContext(Dispatchers.Default) {
                        favoriteItems.filter {
                            SearchNormalizer.matchesTitle(it.title, normalizedQuery)
                        }
                    }
                }
    }
    val filteredFavorites = filteredFavoritesState.value

    val filteredHistoryState = remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    LaunchedEffect(historyItems, normalizedQuery) {
        filteredHistoryState.value =
                if (normalizedQuery.isBlank()) {
                    historyItems
                } else {
                    withContext(Dispatchers.Default) {
                        historyItems.filter {
                            SearchNormalizer.matchesTitle(it.title, normalizedQuery)
                        }
                    }
                }
    }
    val filteredHistory = filteredHistoryState.value

    val currentPreviewItem = remember { mutableStateOf<ContentItem?>(null) }
    val previewQueueItems =
            when {
                selectedSeries != null -> seriesLazyItems?.itemSnapshotList?.items.orEmpty()
                primaryTab == BrowserPrimaryTab.FAVORITES -> filteredFavorites
                primaryTab == BrowserPrimaryTab.HISTORY -> filteredHistory
                primaryTab == BrowserPrimaryTab.CATEGORY ->
                        categoryLazyItems?.itemSnapshotList?.items.orEmpty()
                else -> allLazyItems?.itemSnapshotList?.items.orEmpty()
            }

    Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PrimarySidebar(
                contentType = contentType,
                searchQuery = searchState.query,
                onSearchChange = { searchState.query = it },
                onSearch = { searchState.performSearch() },
                searchFocusRequester = searchFocusRequester,
                primaryFocusRequester = primaryFocusRequester,
                onMoveDownFromSearch = { primaryFocusRequester.requestFocus() },
                primaryTab = primaryTab,
                onPrimarySelected = { selected ->
                    primaryTab = selected
                    selectedSeries = null
                    selectedCategory = null
                    // Focus stays on tab - user navigates right to content manually
                },
                categories = categories,
                isLoadingCategories = isLoadingCategories,
                categoriesError = categoriesError,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    primaryTab = BrowserPrimaryTab.CATEGORY
                    selectedSeries = null
                    selectedCategory = category
                    // Focus stays on category tab - user navigates right to content manually
                }
        )

        SecondarySidebar(
                contentType = contentType,
                primaryTab = primaryTab,
                selectedCategory = selectedCategory,
                selectedSeries = selectedSeries,
                onSeriesSelected = { selectedSeries = it },
                favorites = filteredFavorites,
                history = filteredHistory,
                allItems = allLazyItems,
                categoryItems = categoryLazyItems,
                seriesItems = seriesLazyItems,
                onPlay = onPlay,
                onToggleFavorite = onToggleFavorite,
                onItemFocused = { item ->
                    currentPreviewItem.value = item
                    onItemFocused(item)
                },
                isFavorite = isFavorite,
                focusRequester = secondaryFocusRequester,
                resumeFocusId = resumeFocusId,
                resumeFocusRequester = resumeFocusRequester,
                onMoveLeft = { primaryFocusRequester.requestFocus() },
                onMoveRight = { previewFocusRequester.requestFocus() }
        )

        PreviewPanel(
                contentType = contentType,
                selectedSeries = selectedSeries,
                item = currentPreviewItem.value,
                queueItems = previewQueueItems,
                focusRequester = previewFocusRequester,
                onPlay = onPlay,
                onMoveLeft = { secondaryFocusRequester.requestFocus() }
        )
    }
}

@Composable
private fun PrimarySidebar(
        contentType: ContentType,
        searchQuery: String,
        onSearchChange: (String) -> Unit,
        onSearch: () -> Unit,
        searchFocusRequester: FocusRequester,
        primaryFocusRequester: FocusRequester,
        onMoveDownFromSearch: () -> Unit,
        primaryTab: BrowserPrimaryTab,
        onPrimarySelected: (BrowserPrimaryTab) -> Unit,
        categories: List<CategoryItem>,
        isLoadingCategories: Boolean,
        categoriesError: String?,
        selectedCategory: CategoryItem?,
        onCategorySelected: (CategoryItem) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
            modifier =
                    Modifier.width(260.dp)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(AppTheme.colors.panelBackground)
                            .border(1.dp, AppTheme.colors.panelBorder, shape)
                            .padding(16.dp)
    ) {
        SearchInput(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Search ${contentType.label.lowercase()}...",
                focusRequester = searchFocusRequester,
                modifier = Modifier.fillMaxWidth(),
                onMoveDown = onMoveDownFromSearch,
                onSearch = onSearch
        )
        Spacer(modifier = Modifier.height(16.dp))
        SidebarLabel(text = "BROWSE")
        Spacer(modifier = Modifier.height(8.dp))
        SidebarItem(
                label = "All",
                selected = primaryTab == BrowserPrimaryTab.ALL,
                focusRequester = primaryFocusRequester,
                onActivate = { onPrimarySelected(BrowserPrimaryTab.ALL) }
        )
        SidebarItem(
                label = "Favorites",
                selected = primaryTab == BrowserPrimaryTab.FAVORITES,
                focusRequester = null,
                onActivate = { onPrimarySelected(BrowserPrimaryTab.FAVORITES) }
        )
        SidebarItem(
                label = "Channels History",
                selected = primaryTab == BrowserPrimaryTab.HISTORY,
                focusRequester = null,
                onActivate = { onPrimarySelected(BrowserPrimaryTab.HISTORY) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        SidebarLabel(text = "CATEGORIES")
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoadingCategories) {
            Text(
                    text = "Loading categories...",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily
            )
        } else if (categoriesError != null) {
            Text(
                    text = categoriesError,
                    color = AppTheme.colors.error,
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily
            )
        } else {
            val scrollState = rememberScrollState()
            Column(
                    modifier = Modifier.fillMaxHeight().verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    SidebarItem(
                            label = category.name,
                            selected = selectedCategory?.id == category.id,
                            focusRequester = null,
                            onActivate = { onCategorySelected(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondarySidebar(
        contentType: ContentType,
        primaryTab: BrowserPrimaryTab,
        selectedCategory: CategoryItem?,
        selectedSeries: ContentItem?,
        onSeriesSelected: (ContentItem) -> Unit,
        favorites: List<ContentItem>,
        history: List<ContentItem>,
        allItems: androidx.paging.compose.LazyPagingItems<ContentItem>?,
        categoryItems: androidx.paging.compose.LazyPagingItems<ContentItem>?,
        seriesItems: androidx.paging.compose.LazyPagingItems<ContentItem>?,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onItemFocused: (ContentItem) -> Unit,
        isFavorite: (ContentItem) -> Boolean,
        focusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onMoveLeft: () -> Unit,
        onMoveRight: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val listTitle =
            when {
                selectedSeries != null -> "Episodes"
                primaryTab == BrowserPrimaryTab.FAVORITES -> "Favorites"
                primaryTab == BrowserPrimaryTab.HISTORY -> "Recently Played"
                primaryTab == BrowserPrimaryTab.CATEGORY -> selectedCategory?.name ?: "Category"
                else -> "All ${contentType.label}"
            }
    Column(
            modifier =
                    Modifier.width(320.dp)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(AppTheme.colors.surfaceMuted)
                            .border(1.dp, AppTheme.colors.panelBorder, shape)
                            .padding(16.dp)
    ) {
        Text(
                text = listTitle,
                color = AppTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        when {
            selectedSeries != null && seriesItems != null -> {
                PagedContentList(
                        lazyItems = seriesItems,
                        focusRequester = focusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        isFavorite = isFavorite,
                        onItemFocused = onItemFocused,
                        onActivate = { item -> onPlay(item, seriesItems.itemSnapshotList.items) },
                        onToggleFavorite = onToggleFavorite,
                        onMoveLeft = onMoveLeft,
                        onMoveRight = onMoveRight,
                        emptyLabel = "No episodes yet"
                )
            }
            primaryTab == BrowserPrimaryTab.FAVORITES -> {
                StaticContentList(
                        items = favorites,
                        focusRequester = focusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        isFavorite = isFavorite,
                        onItemFocused = onItemFocused,
                        onActivate = { item ->
                            if (contentType == ContentType.SERIES &&
                                            item.containerExtension.isNullOrBlank()
                            ) {
                                onSeriesSelected(item)
                            } else {
                                onPlay(item, favorites)
                            }
                        },
                        onToggleFavorite = onToggleFavorite,
                        onMoveLeft = onMoveLeft,
                        onMoveRight = onMoveRight,
                        emptyLabel = "No favorites yet"
                )
            }
            primaryTab == BrowserPrimaryTab.HISTORY -> {
                StaticContentList(
                        items = history,
                        focusRequester = focusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        isFavorite = isFavorite,
                        onItemFocused = onItemFocused,
                        onActivate = { item ->
                            if (contentType == ContentType.SERIES &&
                                            item.containerExtension.isNullOrBlank()
                            ) {
                                onSeriesSelected(item)
                            } else {
                                onPlay(item, history)
                            }
                        },
                        onToggleFavorite = onToggleFavorite,
                        onMoveLeft = onMoveLeft,
                        onMoveRight = onMoveRight,
                        emptyLabel = "No recent activity"
                )
            }
            primaryTab == BrowserPrimaryTab.CATEGORY && categoryItems != null -> {
                PagedContentList(
                        lazyItems = categoryItems,
                        focusRequester = focusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        isFavorite = isFavorite,
                        onItemFocused = onItemFocused,
                        onActivate = { item ->
                            if (contentType == ContentType.SERIES &&
                                            item.containerExtension.isNullOrBlank()
                            ) {
                                onSeriesSelected(item)
                            } else {
                                onPlay(item, categoryItems.itemSnapshotList.items)
                            }
                        },
                        onToggleFavorite = onToggleFavorite,
                        onMoveLeft = onMoveLeft,
                        onMoveRight = onMoveRight,
                        emptyLabel = "No content yet"
                )
            }
            primaryTab == BrowserPrimaryTab.CATEGORY && categoryItems == null -> {
                Text(
                        text = "Select a category",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                )
            }
            else -> {
                if (allItems == null) {
                    Text(
                            text = "Select a tab to browse",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp,
                            fontFamily = AppTheme.fontFamily
                    )
                } else {
                    PagedContentList(
                            lazyItems = allItems,
                            focusRequester = focusRequester,
                            resumeFocusId = resumeFocusId,
                            resumeFocusRequester = resumeFocusRequester,
                            isFavorite = isFavorite,
                            onItemFocused = onItemFocused,
                            onActivate = { item ->
                                if (contentType == ContentType.SERIES &&
                                                item.containerExtension.isNullOrBlank()
                                ) {
                                    onSeriesSelected(item)
                                } else {
                                    onPlay(item, allItems.itemSnapshotList.items)
                                }
                            },
                            onToggleFavorite = onToggleFavorite,
                            onMoveLeft = onMoveLeft,
                            onMoveRight = onMoveRight,
                            emptyLabel = "No content yet"
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.PreviewPanel(
        contentType: ContentType,
        selectedSeries: ContentItem?,
        item: ContentItem?,
        queueItems: List<ContentItem>,
        focusRequester: FocusRequester,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onMoveLeft: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val previewItem = item ?: selectedSeries
    Box(
            modifier =
                    Modifier.fillMaxHeight()
                            .weight(1f)
                            .clip(shape)
                            .background(AppTheme.colors.surfaceMuted)
                            .border(1.dp, AppTheme.colors.panelBorder, shape)
                            .padding(20.dp)
                            .focusRequester(focusRequester)
                            .focusable()
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft
                                ) {
                                    onMoveLeft()
                                    true
                                } else {
                                    false
                                }
                            }
    ) {
        if (previewItem == null) {
            Text(
                    text = "Select a ${contentType.label.lowercase()} to preview",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = AppTheme.fontFamily
            )
        } else {
            Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val context = LocalContext.current
                val imageRequest =
                        remember(previewItem.imageUrl) {
                            if (previewItem.imageUrl.isNullOrBlank()) {
                                null
                            } else {
                                ImageRequest.Builder(context)
                                        .data(previewItem.imageUrl)
                                        .size(600)
                                        .build()
                            }
                        }
                if (imageRequest != null) {
                    AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.Low,
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(AppTheme.colors.surfaceAlt)
                    )
                }
                Text(
                        text = previewItem.title,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 20.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                )
                Text(
                        text = previewItem.subtitle,
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                val canPlay =
                        previewItem.contentType != ContentType.SERIES ||
                                !previewItem.containerExtension.isNullOrBlank()
                if (canPlay) {
                    val playbackItems =
                            if (queueItems.isEmpty()) {
                                listOf(previewItem)
                            } else {
                                queueItems
                            }
                    TopBarButton(
                            label = "PLAY",
                            onActivate = { onPlay(previewItem, playbackItems) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieInfoDialog(
        item: ContentItem,
        info: MovieInfo?,
        queueItems: List<ContentItem>,
        isFavorite: Boolean,
        onToggleFavorite: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onPlayWithPosition: (ContentItem, List<ContentItem>, Long?) -> Unit,
        resumePositionMs: Long?,
        showClearContinueWatching: Boolean,
        onClearContinueWatching: (() -> Unit)?,
        onDismiss: () -> Unit
) {
    BackHandler(enabled = true) { onDismiss() }
    AppDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colors = AppTheme.colors
        val releaseLabel = formatReleaseYear(info?.releaseDate, info?.year)
        val playFocusRequester = remember { FocusRequester() }
        val clearFocusRequester = remember { FocusRequester() }
        val appScale = LocalAppScale.current
        val baseDensity = LocalAppBaseDensity.current ?: LocalDensity.current
        val movieDetailsScale = remember(appScale.uiScale) {
            (1.05f * (1f + (appScale.uiScale - 1f) * 0.5f)).coerceIn(0.9f, 1.25f)
        }
        val movieDetailsDensity = remember(movieDetailsScale, baseDensity, appScale.fontScale) {
            Density(
                density = baseDensity.density * movieDetailsScale,
                fontScale = baseDensity.fontScale * movieDetailsScale * appScale.fontScale
            )
        }
        var showPlotDialog by remember { mutableStateOf(false) }
        var plotOverflow by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { playFocusRequester.requestFocus() }
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(colors.surface)
                                .padding(28.dp)
        ) {
            Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = item.title,
                            color = colors.textPrimary,
                            fontSize = 22.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                    )
                    TopBarButton(label = "CLOSE", onActivate = onDismiss)
                }

                Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val context = LocalContext.current
                    val imageRequest =
                            remember(item.imageUrl) {
                                if (item.imageUrl.isNullOrBlank()) {
                                    null
                                } else {
                                    ImageRequest.Builder(context)
                                            .data(item.imageUrl)
                                            .size(600)
                                            .build()
                                }
                            }
                    if (imageRequest != null) {
                        AsyncImage(
                                model = imageRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.Low,
                                modifier =
                                        Modifier.width(220.dp)
                                                .aspectRatio(2f / 3f)
                                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Box(
                                modifier =
                                        Modifier.width(220.dp)
                                                .aspectRatio(2f / 3f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(colors.surfaceAlt)
                        )
                    }

                    CompositionLocalProvider(LocalDensity provides movieDetailsDensity) {
                        Column(
                                modifier = Modifier.fillMaxHeight().weight(1f)
                        ) {
                            val description =
                                    info?.description?.takeIf { it.isNotBlank() }
                                            ?: "No description available."
                            val showReadMore = plotOverflow
                            val readMoreInteraction = remember { MutableInteractionSource() }
                            val isReadMoreFocused by readMoreInteraction.collectIsFocusedAsState()
                            val readMoreFocusRequester = remember { FocusRequester() }

                            Column(verticalArrangement = Arrangement.spacedBy(DETAIL_ROW_SPACING)) {
                                MovieInfoRow(label = "Directed By:", value = info?.director)
                                MovieInfoRow(label = "Release Date:", value = releaseLabel)
                                MovieInfoRow(
                                        label = "Duration:",
                                        value = formatDuration(info?.duration)
                                )
                                MovieInfoRow(label = "Genre:", value = info?.genre)
                                MovieInfoRow(label = "Cast:", value = info?.cast, valueMaxLines = 2)
                                val ratingValue = ratingToStars(info?.rating)
                                if (ratingValue != null) {
                                    RatingStarsRow(label = "Rating:", rating = ratingValue)
                                } else {
                                    MovieInfoRow(label = "Rating:", value = null)
                                }
                                Spacer(modifier = Modifier.height(DETAIL_SECTION_SPACING))

                                if (showReadMore) {
                                    Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Bottom,
                                            modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                                text = description,
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontFamily = AppTheme.fontFamily,
                                                lineHeight = 18.sp,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                                onTextLayout = { plotOverflow = it.hasVisualOverflow },
                                                modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                                modifier =
                                                        Modifier.focusRequester(readMoreFocusRequester)
                                                                .focusable(interactionSource = readMoreInteraction)
                                                                .onKeyEvent {
                                                                    if (it.type != KeyEventType.KeyDown) {
                                                                        false
                                                                    } else if (it.key == Key.Enter ||
                                                                                    it.key == Key.NumPadEnter ||
                                                                                    it.key == Key.DirectionCenter
                                                                    ) {
                                                                        showPlotDialog = true
                                                                        true
                                                                    } else {
                                                                        false
                                                                    }
                                                                }
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .then(
                                                                        if (isReadMoreFocused) {
                                                                            Modifier.border(1.dp, colors.focus, RoundedCornerShape(6.dp))
                                                                        } else {
                                                                            Modifier
                                                                        }
                                                                )
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                .clickable(
                                                                        interactionSource = readMoreInteraction,
                                                                        indication = null
                                                                ) { showPlotDialog = true }
                                        ) {
                                            Text(
                                                    text = "Read more",
                                                    color = colors.accent,
                                                    fontSize = 12.sp,
                                                    fontFamily = AppTheme.fontFamily,
                                                    fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                            text = description,
                                            color = colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontFamily = AppTheme.fontFamily,
                                            lineHeight = 18.sp,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis,
                                            onTextLayout = { plotOverflow = it.hasVisualOverflow }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(DETAIL_SECTION_SPACING))

                            Column(verticalArrangement = Arrangement.spacedBy(DETAIL_ROW_SPACING)) {
                                Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isResume = resumePositionMs != null && resumePositionMs > 0
                                    FocusableButton(
                                            onClick = {
                                                if (isResume) {
                                                    onPlayWithPosition(item, queueItems, resumePositionMs)
                                                } else {
                                                    onPlay(item, queueItems)
                                                }
                                            },
                                            modifier = Modifier.width(180.dp).focusRequester(playFocusRequester),
                                            colors =
                                                    ButtonDefaults.buttonColors(
                                                            containerColor = colors.accent,
                                                            contentColor = colors.textOnAccent
                                                    )
                                    ) {
                                        Text(
                                                text = if (isResume) "Resume Playing" else "Play",
                                                fontSize = 14.sp,
                                                fontFamily = AppTheme.fontFamily,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    FocusableButton(
                                            onClick = { onToggleFavorite(item) },
                                            modifier = Modifier.size(48.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors =
                                                    ButtonDefaults.buttonColors(
                                                            containerColor = colors.surfaceAlt,
                                                            contentColor = colors.textPrimary
                                                    )
                                    ) {
                                        Icon(
                                                imageVector =
                                                        if (isFavorite) {
                                                            Icons.Filled.Favorite
                                                        } else {
                                                            Icons.Outlined.FavoriteBorder
                                                        },
                                                contentDescription = "Favorite",
                                                tint = if (isFavorite) Color(0xFFEF5350) else colors.textPrimary,
                                                modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    if (showClearContinueWatching && onClearContinueWatching != null) {
                                        FocusableButton(
                                                onClick = onClearContinueWatching,
                                                modifier = Modifier.height(48.dp).focusRequester(clearFocusRequester),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = colors.surfaceAlt,
                                                                contentColor = colors.textPrimary
                                                        )
                                        ) {
                                            Text(
                                                    text = "Clear from list",
                                                    fontSize = 11.sp,
                                                    fontFamily = AppTheme.fontFamily,
                                                    fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                            text = "Video: ${formatVideoSummary(info)}",
                                            color = colors.textSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = AppTheme.fontFamily,
                                            lineHeight = 13.sp
                                    )
                                    Text(
                                            text = "Audio: ${formatAudioSummary(info)}",
                                            color = colors.textSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = AppTheme.fontFamily,
                                            lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPlotDialog) {
            val description =
                    info?.description?.takeIf { it.isNotBlank() }
                            ?: "No description available."
            PlotDialog(
                    title = item.title,
                    plot = description,
                    onDismiss = { showPlotDialog = false }
            )
        }
    }
}

@Composable
private fun MovieInfoRow(
        label: String,
        value: String?,
        valueMaxLines: Int = Int.MAX_VALUE
) {
    val displayValue = value?.takeIf { it.isNotBlank() } ?: "N/A"
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
                text = label,
                color = AppTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily
        )
        Text(
                text = displayValue,
                color = AppTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Medium,
                maxLines = valueMaxLines,
                overflow = if (valueMaxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis
        )
    }
}

private fun formatVideoSummary(info: MovieInfo?): String {
    if (info == null) return "N/A"
    val hasAny =
        !info.videoResolution.isNullOrBlank() ||
            !info.videoCodec.isNullOrBlank() ||
            !info.videoHdr.isNullOrBlank()
    if (!hasAny) {
        return "Resolution: N/A \u2022 Codec: N/A \u2022 Dynamic Range: N/A"
    }
    val resolution = info.videoResolution?.takeIf { it.isNotBlank() } ?: "N/A"
    val codec = info.videoCodec?.takeIf { it.isNotBlank() } ?: "N/A"
    val range = info.videoHdr?.takeIf { it.isNotBlank() } ?: "SDR"
    return "Resolution: $resolution \u2022 Codec: $codec \u2022 Dynamic Range: $range"
}

private fun formatAudioSummary(info: MovieInfo?): String {
    if (info == null) return "N/A"
    val hasAny =
        !info.audioCodec.isNullOrBlank() ||
            !info.audioChannels.isNullOrBlank() ||
            info.audioLanguages.any { it.isNotBlank() }
    if (!hasAny) {
        return "Codec: N/A \u2022 Channels: N/A \u2022 Language: N/A"
    }
    val codec = info.audioCodec?.takeIf { it.isNotBlank() } ?: "N/A"
    val channels = info.audioChannels?.takeIf { it.isNotBlank() } ?: "N/A"
    val languages =
        info.audioLanguages.filter { it.isNotBlank() }.distinct().takeIf { it.isNotEmpty() }
            ?: listOf("N/A")
    return "Codec: $codec \u2022 Channels: $channels \u2022 Language: ${languages.joinToString(", ")}"
}

@Composable
private fun RatingStarsRow(
        label: String,
        rating: Float
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
                text = label,
                color = AppTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily
        )
        RatingStars(
                rating = rating,
                starSize = 16.dp,
                spacing = 3.dp
        )
    }
}

@Composable
private fun RatingStars(
        rating: Float,
        starSize: Dp,
        spacing: Dp
) {
    val clamped = rating.coerceIn(0f, 5f)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
        repeat(5) { index ->
            val fill = (clamped - index).coerceIn(0f, 1f)
            Box(modifier = Modifier.size(starSize)) {
                Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxSize()
                )
                if (fill > 0f) {
                    Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier =
                                    Modifier.fillMaxSize()
                                            .drawWithContent {
                                                clipRect(right = size.width * fill) {
                                                    this@drawWithContent.drawContent()
                                                }
                                            }
                    )
                }
            }
        }
    }
}

private fun ratingToStars(rawRating: String?): Float? {
    val raw = rawRating?.trim().orEmpty()
    if (raw.isBlank()) return null
    val match = Regex("(\\d+(?:\\.\\d+)?)").find(raw) ?: return null
    val value = match.value.toFloatOrNull() ?: return null
    if (value <= 0f) return null
    return when {
        value <= 5f -> value
        value <= 10f -> value / 2f
        value <= 100f -> value / 20f
        else -> null
    }
}

private fun formatDuration(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isBlank()) return null
    val parts = text.split(":").map { it.trim() }
    if (parts.size >= 2 && parts.all { it.matches(Regex("\\d+")) }) {
        val numbers = parts.map { it.toInt() }
        val hours: Int
        val minutes: Int
        if (parts.size >= 3) {
            hours = numbers[0]
            minutes = numbers[1]
        } else {
            val first = numbers[0]
            val second = numbers[1]
            if (first >= 60) {
                hours = first / 60
                minutes = first % 60
            } else {
                hours = first
                minutes = second
            }
        }
        if (hours <= 0 && minutes <= 0) return null
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
    val numeric = Regex("(\\d+)").find(text)?.value?.toIntOrNull()
    if (numeric != null) {
        if (numeric <= 0) return null
        val hours = numeric / 60
        val minutes = numeric % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
    return text
}

private fun formatReleaseYear(releaseDate: String?, year: String?): String? {
    val yearText = year?.trim().orEmpty()
    if (yearText.isNotBlank()) return yearText
    val raw = releaseDate?.trim().orEmpty()
    if (raw.isBlank()) return null
    val match = Regex("(\\d{4})").find(raw)
    return match?.value ?: raw
}

internal fun cacheDirSizeBytes(dir: File): Long {
    if (!dir.exists()) return 0L
    if (dir.isFile) return dir.length()
    return dir.listFiles()?.sumOf { file -> cacheDirSizeBytes(file) } ?: 0L
}

internal fun clearCacheDir(dir: File) {
    dir.listFiles()?.forEach { file ->
        runCatching { file.deleteRecursively() }
    }
}

@Composable
private fun SidebarLabel(text: String) {
    Text(
            text = text,
            color = AppTheme.colors.textTertiary,
            fontSize = 11.sp,
            fontFamily = AppTheme.fontFamily,
            letterSpacing = 1.sp
    )
}

@Composable
private fun SidebarItem(
        label: String,
        selected: Boolean,
        focusRequester: FocusRequester?,
        onActivate: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val background =
            when {
                isFocused ->
                        Brush.horizontalGradient(
                                colors = listOf(AppTheme.colors.accent, AppTheme.colors.accentAlt)
                        )
                selected ->
                        Brush.horizontalGradient(
                                colors = listOf(AppTheme.colors.accentSelected, AppTheme.colors.accentSelectedAlt)
                        )
                else ->
                        Brush.horizontalGradient(
                                colors = listOf(AppTheme.colors.accentMutedAlt, AppTheme.colors.surfaceAlt)
                        )
            }
    val textColor = if (isFocused) AppTheme.colors.textOnAccent else AppTheme.colors.textPrimary
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .height(44.dp)
                            .then(
                                    if (focusRequester != null)
                                            Modifier.focusRequester(focusRequester)
                                    else Modifier
                            )
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                if (it.type != KeyEventType.KeyDown) {
                                    false
                                } else if (it.key == Key.Enter ||
                                                it.key == Key.NumPadEnter ||
                                                it.key == Key.DirectionCenter
                                ) {
                                    onActivate()
                                    true
                                } else {
                                    false
                                }
                            }
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onActivate
                            )
                            .clip(shape)
                            .background(background)
                            .border(
                                    1.dp,
                                    if (isFocused) AppTheme.colors.focus else AppTheme.colors.border,
                                    shape
                            ),
            contentAlignment = Alignment.CenterStart
    ) {
        Text(
                text = label,
                color = textColor,
                fontSize = 14.sp,
                fontFamily = AppTheme.fontFamily,
                modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun StaticContentList(
        items: List<ContentItem>,
        focusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        isFavorite: (ContentItem) -> Boolean,
        onItemFocused: (ContentItem) -> Unit,
        onActivate: (ContentItem) -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onMoveLeft: () -> Unit,
        onMoveRight: () -> Unit,
        emptyLabel: String
) {
    if (items.isEmpty()) {
        Text(
                text = emptyLabel,
                color = AppTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily
        )
        return
    }
    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
                count = items.size,
                key = { index -> items[index].id }
        ) { index ->
            val item = items[index]
            val requester =
                    when {
                        item.id == resumeFocusId -> resumeFocusRequester
                        index == 0 -> focusRequester
                        else -> null
                    }
            ContentListItem(
                    item = item,
                    focusRequester = requester,
                    isFavorite = isFavorite(item),
                    onActivate = { onActivate(item) },
                    onFocused = { onItemFocused(item) },
                    onMoveLeft = onMoveLeft,
                    onMoveRight = onMoveRight,
                    onLongClick = { onToggleFavorite(item) }
            )
        }
    }
}

@Composable
private fun PagedContentList(
        lazyItems: androidx.paging.compose.LazyPagingItems<ContentItem>,
        focusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        isFavorite: (ContentItem) -> Boolean,
        onItemFocused: (ContentItem) -> Unit,
        onActivate: (ContentItem) -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onMoveLeft: () -> Unit,
        onMoveRight: () -> Unit,
        emptyLabel: String
) {
    if (lazyItems.itemCount == 0 && lazyItems.loadState.refresh is LoadState.Loading) {
        Text(
                text = "Loading...",
                color = AppTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily
        )
        return
    }
    if (lazyItems.itemCount == 0 && lazyItems.loadState.refresh is LoadState.Error) {
        Text(
                text = "Content failed to load",
                color = AppTheme.colors.error,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily
        )
        return
    }
    if (lazyItems.itemCount == 0) {
        Text(
                text = emptyLabel,
                color = AppTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily
        )
        return
    }
    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
                count = lazyItems.itemCount,
                key = { index -> lazyItems[index]?.id ?: "item-$index" }
        ) { index ->
            val item = lazyItems[index]
            if (item != null) {
                val requester =
                        when {
                            item.id == resumeFocusId -> resumeFocusRequester
                            index == 0 -> focusRequester
                            else -> null
                        }
                ContentListItem(
                        item = item,
                        focusRequester = requester,
                        isFavorite = isFavorite(item),
                        onActivate = { onActivate(item) },
                        onFocused = { onItemFocused(item) },
                        onMoveLeft = onMoveLeft,
                        onMoveRight = onMoveRight,
                        onLongClick = { onToggleFavorite(item) }
                )
            }
        }
    }
}

@Composable
private fun ContentListItem(
        item: ContentItem,
        focusRequester: FocusRequester?,
        isFavorite: Boolean,
        onActivate: () -> Unit,
        onFocused: () -> Unit,
        onMoveLeft: () -> Unit,
        onMoveRight: () -> Unit,
        onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isFocused) AppTheme.colors.focus else AppTheme.colors.borderStrong
    val background = if (isFocused) AppTheme.colors.surfaceFocused else AppTheme.colors.surfaceMuted
    var keyDownArmed by remember { mutableStateOf(false) }
    var keyClickHandled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val listThumbnailPx = remember(density) { with(density) { 52.dp.roundToPx() } }
    val imageRequest =
            remember(item.imageUrl, listThumbnailPx) {
                if (item.imageUrl.isNullOrBlank()) {
                    null
                } else {
                    ImageRequest.Builder(context)
                            .data(item.imageUrl)
                            .size(listThumbnailPx)
                            .build()
                }
            }
    LaunchedEffect(isFocused) {
        if (!isFocused) {
            keyDownArmed = false
            keyClickHandled = false
        }
    }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused()
        }
    }
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .height(72.dp)
                            .then(
                                    if (focusRequester != null)
                                            Modifier.focusRequester(focusRequester)
                                    else Modifier
                            )
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                val isSelectKey =
                                        it.key == Key.Enter ||
                                                it.key == Key.NumPadEnter ||
                                                it.key == Key.DirectionCenter
                                when (it.type) {
                                    KeyEventType.KeyDown -> {
                                        when {
                                            it.key == Key.DirectionLeft -> {
                                                onMoveLeft()
                                                true
                                            }
                                            it.key == Key.DirectionRight -> {
                                                onMoveRight()
                                                true
                                            }
                                            isSelectKey -> {
                                                if (onLongClick != null &&
                                                                (it.nativeKeyEvent.isLongPress ||
                                                                        it.nativeKeyEvent
                                                                                .repeatCount > 0)
                                                ) {
                                                    onLongClick()
                                                    true
                                                } else {
                                                    keyDownArmed = true
                                                    true
                                                }
                                            }
                                            else -> false
                                        }
                                    }
                                    KeyEventType.KeyUp -> {
                                        if (isSelectKey) {
                                            if (keyDownArmed) {
                                                keyDownArmed = false
                                                keyClickHandled = true
                                                onActivate()
                                                true
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                    else -> false
                                }
                            }
                            .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {
                                        if (keyClickHandled) {
                                            keyClickHandled = false
                                        } else {
                                            onActivate()
                                        }
                                    },
                                    onLongClick = { onLongClick?.invoke() }
                            )
                            .clip(shape)
                            .background(background)
                            .border(1.dp, borderColor, shape)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (imageRequest != null) {
                AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Low,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                        modifier =
                                Modifier.size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AppTheme.colors.surfaceAlt)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = item.title,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        maxLines = 1
                )
                Text(
                        text = item.subtitle,
                        color = AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = AppTheme.fontFamily,
                        maxLines = 1
                )
            }
            if (isFavorite) {
                Icon(
                        painter = painterResource(R.drawable.ic_favorite),
                        contentDescription = "Favorite",
                        tint = AppTheme.colors.warning,
                        modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentCard(
        item: ContentItem?,
        subtitleOverride: String? = null,
        focusRequester: FocusRequester?,
        isLeftEdge: Boolean,
        isFavorite: Boolean,
        onActivate: (() -> Unit)?,
        onFocused: ((ContentItem) -> Unit)?,
        onMoveLeft: () -> Unit,
        onMoveUp: (() -> Unit)? = null,
        onLongClick: ((ContentItem) -> Unit)? = null,
        titleFontSize: TextUnit = 16.sp,
        subtitleFontSize: TextUnit = 12.sp,
        forceDarkText: Boolean = false,
        useContrastText: Boolean = false,
        isPoster: Boolean = false,
        fontScaleFactor: Float = 1f,
        enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var longPressTriggered by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val colors = AppTheme.colors
    val borderColor = if (isFocused) AppTheme.colors.focus else AppTheme.colors.borderStrong
    val backgroundColor = if (isFocused) AppTheme.colors.surfaceAlt else AppTheme.colors.surface
    val overlayBaseColor =
            if (useContrastText) {
                bestContrastText(colors.overlay, colors.textPrimary, colors.textOnAccent)
            } else if (forceDarkText) {
                colors.textPrimary
            } else {
                cardTitleColor(colors)
            }
    val titleColor = overlayBaseColor
    val subtitleColor =
            if (useContrastText) overlayBaseColor.copy(alpha = 0.75f)
            else if (forceDarkText) colors.textSecondary
            else cardSubtitleColor(colors)
    val title = item?.title ?: "Loading..."
    val subtitle = subtitleOverride ?: item?.subtitle ?: "Please wait"
    val scaledTitleSize = scaleTextSize(titleFontSize, fontScaleFactor)
    val scaledSubtitleSize = scaleTextSize(subtitleFontSize, fontScaleFactor)
    val imageUrl = item?.imageUrl
    val context = LocalContext.current
    val isSeriesTitle = item?.contentType == ContentType.SERIES && item.containerExtension.isNullOrBlank()
    val isNaturalPoster = item?.contentType == ContentType.MOVIES || isSeriesTitle
    val posterStyle = isNaturalPoster || isPoster
    val isLiveCard = item?.contentType == ContentType.LIVE && !posterStyle
    val showBackdrop = (posterStyle && item != null && !isNaturalPoster) || isLiveCard
    val cardAspectRatio = if (posterStyle) POSTER_ASPECT_RATIO else LANDSCAPE_ASPECT_RATIO
    val imageContentScale = if (posterStyle || isLiveCard) ContentScale.Fit else ContentScale.Crop
    val contentPadding = 2.dp
    val overlayPadding = if (posterStyle) 10.dp else 12.dp
    val labelStripColor = cardTextStripColor(colors, titleColor)
    val liveBackdropBlur = 18.dp
    val liveBackdropAlpha = 0.65f
    val liveImageAlpha = 1f
    val imageRequest =
            remember(imageUrl) {
                if (imageUrl.isNullOrBlank()) {
                    null
                } else {
                    ImageRequest.Builder(context).data(imageUrl).size(600).build()
                }
            }
    LaunchedEffect(isFocused, item?.id) {
        if (isFocused && item != null) {
            onFocused?.invoke(item)
        }
    }
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .aspectRatio(cardAspectRatio)
                            .then(
                                    if (focusRequester != null) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    }
                            )
                            .focusable(enabled = enabled, interactionSource = interactionSource)
                            .then(
                                    if (enabled) {
                                        Modifier.onPreviewKeyEvent {
                                                    if (it.type != KeyEventType.KeyDown || onMoveUp == null) {
                                                        false
                                                    } else if (it.key == Key.DirectionUp) {
                                                        onMoveUp()
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                .onKeyEvent {
                                                    val isSelectKey =
                                                            it.key == Key.Enter ||
                                                                    it.key == Key.NumPadEnter ||
                                                                    it.key == Key.DirectionCenter
                                                    when (it.type) {
                                                        KeyEventType.KeyDown -> {
                                                            when {
                                                                isLeftEdge &&
                                                                        it.key == Key.DirectionLeft -> {
                                                                    onMoveLeft()
                                                                    true
                                                                }
                                                                onMoveUp != null &&
                                                                        it.key == Key.DirectionUp -> {
                                                                    onMoveUp()
                                                                    true
                                                                }
                                                                isSelectKey &&
                                                                        (onActivate != null ||
                                                                                onLongClick != null) -> {
                                                                    if (item != null &&
                                                                                    onLongClick != null &&
                                                                                    (it.nativeKeyEvent.isLongPress ||
                                                                                            it.nativeKeyEvent
                                                                                                    .repeatCount > 0)
                                                                    ) {
                                                                        if (!longPressTriggered) {
                                                                            onLongClick(item)
                                                                            longPressTriggered = true
                                                                        }
                                                                        true
                                                                    } else {
                                                                        true
                                                                    }
                                                                }
                                                                else -> false
                                                            }
                                                        }
                                                        KeyEventType.KeyUp -> {
                                                            if (isSelectKey &&
                                                                            (onActivate != null ||
                                                                                    onLongClick != null)
                                                            ) {
                                                                if (longPressTriggered) {
                                                                    longPressTriggered = false
                                                                    true
                                                                } else if (onActivate != null) {
                                                                    onActivate()
                                                                    true
                                                                } else {
                                                                    false
                                                                }
                                                            } else {
                                                                false
                                                            }
                                                        }
                                                        else -> false
                                                    }
                                                }
                                    } else {
                                        Modifier
                                    }
                            )
                            .then(
                                    if (enabled && onActivate != null) {
                                        Modifier.combinedClickable(
                                                interactionSource = interactionSource,
                                                indication = null,
                                                onClick = onActivate,
                                                onLongClick = {
                                                    if (item != null) {
                                                        onLongClick?.invoke(item)
                                                    }
                                                }
                                        )
                                    } else {
                                        Modifier
                                    }
                            )
                            .clip(shape)
                            .background(backgroundColor)
                            .border(1.dp, borderColor, shape)
                            .padding(2.dp),
            contentAlignment = Alignment.BottomStart
    ) {
        if (imageRequest != null) {
            if (showBackdrop) {
                val backdropBlur = if (isLiveCard) liveBackdropBlur else 18.dp
                val backdropAlpha = if (isLiveCard) liveBackdropAlpha else 0.65f
                AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Low,
                        modifier = Modifier.fillMaxSize().blur(backdropBlur).alpha(backdropAlpha)
                )
            }
            AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = imageContentScale,
                    filterQuality = FilterQuality.Low,
                    modifier = Modifier.fillMaxSize().alpha(liveImageAlpha)
            )
        }
        if (isFavorite) {
            FavoriteIndicator(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(labelStripColor)
                                .padding(
                                        start = overlayPadding,
                                        end = 10.dp,
                                        top = 10.dp,
                                        bottom = 10.dp
                                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                        text = title,
                        color = titleColor,
                        fontSize = scaledTitleSize,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                )
                Text(
                        text = subtitle,
                        color = subtitleColor,
                        fontSize = scaledSubtitleSize,
                        fontFamily = AppTheme.fontFamily,
                        letterSpacing = 0.2.sp
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
        entry: ContinueWatchingDisplayEntry,
        focusRequester: FocusRequester?,
        isLeftEdge: Boolean,
        isFavorite: Boolean,
        progressPercent: Int,
        fontScaleFactor: Float = 1f,
        onActivate: () -> Unit,
        onFocused: () -> Unit,
        onMoveLeft: () -> Unit,
        onLongClick: () -> Unit,
        onRemove: () -> Unit
) {
    val item = entry.displayItem
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var longPressTriggered by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val colors = AppTheme.colors
    val borderColor = if (isFocused) AppTheme.colors.focus else AppTheme.colors.borderStrong
    val backgroundColor = if (isFocused) AppTheme.colors.surfaceAlt else AppTheme.colors.surface
    val title = item.title
    val resumeLabel =
            if (item.contentType == ContentType.SERIES) {
                formatEpisodeLabel(item, separator = " - ")?.let { "Resume $it" }
            } else {
                null
            }
    val subtitle = resumeLabel ?: item.subtitle
    val imageUrl = item.imageUrl
    val context = LocalContext.current
    val labelStripColor = cardTextStripColor(colors, cardTitleColor(colors))
    val isSeriesTitle =
            item.contentType == ContentType.SERIES && item.containerExtension.isNullOrBlank()
    val isNaturalPoster = item.contentType == ContentType.MOVIES || isSeriesTitle
    val showBackdrop = !isNaturalPoster
    val cardAspectRatio = POSTER_ASPECT_RATIO
    val imageContentScale = ContentScale.Fit
    val titleSize = scaleTextSize(16.sp, fontScaleFactor)
    val subtitleSize = scaleTextSize(12.sp, fontScaleFactor)
    val contentPadding = 2.dp
    val imageRequest =
            remember(imageUrl) {
                if (imageUrl.isNullOrBlank()) {
                    null
                } else {
                    ImageRequest.Builder(context).data(imageUrl).size(600).build()
                }
            }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused()
        }
    }

    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .aspectRatio(cardAspectRatio)
                            .then(
                                    if (focusRequester != null) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    }
                            )
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                val isSelectKey =
                                        it.key == Key.Enter ||
                                                it.key == Key.NumPadEnter ||
                                                it.key == Key.DirectionCenter
                                when (it.type) {
                                    KeyEventType.KeyDown -> {
                                        when {
                                            isLeftEdge && it.key == Key.DirectionLeft -> {
                                                onMoveLeft()
                                                true
                                            }
                                            it.key == Key.Menu -> {
                                                onRemove()
                                                true
                                            }
                                            isSelectKey -> {
                                                if (it.nativeKeyEvent.isLongPress ||
                                                                it.nativeKeyEvent.repeatCount > 0
                                                ) {
                                                    if (!longPressTriggered) {
                                                        onLongClick()
                                                        longPressTriggered = true
                                                    }
                                                    true
                                                } else {
                                                    true
                                                }
                                            }
                                            else -> false
                                        }
                                    }
                                    KeyEventType.KeyUp -> {
                                        if (isSelectKey) {
                                            if (longPressTriggered) {
                                                longPressTriggered = false
                                                true
                                            } else {
                                                onActivate()
                                                true
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                    else -> false
                                }
                            }
                            .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onActivate,
                                    onLongClick = onLongClick
                            )
                            .clip(shape)
                            .background(backgroundColor)
                            .border(1.dp, borderColor, shape)
                            .padding(2.dp),
            contentAlignment = Alignment.BottomStart
    ) {
        if (imageRequest != null) {
            if (showBackdrop) {
                AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Low,
                        modifier = Modifier.fillMaxSize().blur(18.dp).alpha(0.65f)
                )
            }
            AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = imageContentScale,
                    filterQuality = FilterQuality.Low,
                    modifier = Modifier.fillMaxSize()
            )
        }
        if (isFavorite) {
            FavoriteIndicator(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        // Progress bar overlay
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter)
                                .background(AppTheme.colors.overlaySoft)
        ) {
            Box(
                    modifier =
                            Modifier.fillMaxWidth(progressPercent / 100f)
                                    .fillMaxHeight()
                                    .background(AppTheme.colors.accent)
            )
        }
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(labelStripColor)
                                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                        text = title,
                        color = cardTitleColor(colors),
                        fontSize = titleSize,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                )
                Text(
                        text = "$subtitle • $progressPercent% watched",
                        color = cardSubtitleColor(colors),
                        fontSize = subtitleSize,
                        fontFamily = AppTheme.fontFamily,
                        letterSpacing = 0.2.sp
                )
            }
        }
    }
}

@Composable
private fun rememberSeriesSubtitle(
        item: ContentItem?,
        contentRepository: ContentRepository,
        authConfig: AuthConfig
): String? {
    if (item == null) return null
    if (item.contentType != ContentType.SERIES || !item.containerExtension.isNullOrBlank()) {
        return null
    }
    val seasonCount by
            produceState<Int?>(initialValue = null, key1 = item.streamId, key2 = authConfig) {
                value =
                        runCatching {
                                    contentRepository.loadSeriesSeasonCount(
                                            item.streamId,
                                            authConfig
                                    )
                                }
                                .getOrNull()
            }
    val count = seasonCount
    return if (count != null && count > 0) {
        val label = if (count == 1) "Season" else "Seasons"
        "Series \u00b7 $count $label"
    } else {
        "Series"
    }
}

@Composable
private fun CategoryCard(
        label: String,
        focusRequester: FocusRequester?,
        isLeftEdge: Boolean,
        isFavorite: Boolean,
        thumbnail: ThumbnailSpec? = null,
        imageUrl: String? = null,
        onActivate: () -> Unit,
        onMoveLeft: () -> Unit,
        onMoveUp: (() -> Unit)? = null,
        onMoveDown: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
        forceDarkText: Boolean = false,
        useContrastText: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var longPressTriggered by remember { mutableStateOf(false) }
    var keyDownArmed by remember { mutableStateOf(false) }
    var keyClickHandled by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val colors = AppTheme.colors
    val borderColor = if (isFocused) colors.focus else colors.borderStrong
    val backgroundColor = if (isFocused) colors.surfaceAlt else colors.surface
    val lightTheme = isLightTheme(colors)
    val backdropBlur = if (lightTheme) 11.dp else 11.dp
    val backdropAlpha = if (lightTheme) 0.9f else 0.9f
    val backdropScrimAlpha = if (lightTheme) 0.2f else 0.2f
    val backdropScrimColor = if (lightTheme) Color.White else Color.Black
    val labelColor =
            when {
                useContrastText -> bestContrastText(backgroundColor, colors.textPrimary, colors.textOnAccent)
                forceDarkText -> colors.textPrimary
                else -> cardTitleColor(colors)
            }
    val context = LocalContext.current
    val density = LocalDensity.current
    val categoryThumbnailPx = remember(density) { with(density) { 54.dp.roundToPx() } }
    val imageRequest =
            remember(imageUrl, categoryThumbnailPx) {
                if (imageUrl.isNullOrBlank()) {
                    null
                } else {
                    ImageRequest.Builder(context)
                            .data(imageUrl)
                            .size(categoryThumbnailPx)
                            .build()
                }
            }
    LaunchedEffect(isFocused) {
        if (!isFocused) {
            longPressTriggered = false
            keyDownArmed = false
            keyClickHandled = false
        }
    }
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .then(
                                    if (focusRequester != null) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    }
                            )
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                val isSelectKey =
                                        it.key == Key.Enter ||
                                                it.key == Key.NumPadEnter ||
                                                it.key == Key.DirectionCenter
                                when (it.type) {
                                    KeyEventType.KeyDown -> {
                                        when {
                                            isLeftEdge && it.key == Key.DirectionLeft -> {
                                                onMoveLeft()
                                                true
                                            }
                                            onMoveUp != null && it.key == Key.DirectionUp -> {
                                                onMoveUp()
                                                true
                                            }
                                            onMoveDown != null && it.key == Key.DirectionDown -> {
                                                onMoveDown()
                                                true
                                            }
                                            isSelectKey -> {
                                                if (onLongClick != null &&
                                                                (it.nativeKeyEvent.isLongPress ||
                                                                        it.nativeKeyEvent
                                                                                .repeatCount > 0)
                                                ) {
                                                    if (!longPressTriggered) {
                                                        onLongClick()
                                                        longPressTriggered = true
                                                    }
                                                    true
                                                } else {
                                                    keyDownArmed = true
                                                    true
                                                }
                                            }
                                            else -> false
                                        }
                                    }
                                    KeyEventType.KeyUp -> {
                                        if (isSelectKey) {
                                            if (longPressTriggered) {
                                                longPressTriggered = false
                                                keyDownArmed = false
                                                true
                                            } else if (keyDownArmed) {
                                                keyDownArmed = false
                                                keyClickHandled = true
                                                onActivate()
                                                true
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                    else -> false
                                }
                            }
                            .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {
                                        if (keyClickHandled) {
                                            keyClickHandled = false
                                        } else {
                                            onActivate()
                                        }
                                    },
                                    onLongClick = { onLongClick?.invoke() }
                            )
                            .clip(shape)
                            .background(backgroundColor)
                            .border(1.dp, borderColor, shape)
                            .padding(2.dp),
            contentAlignment = Alignment.BottomStart
    ) {
        if (imageRequest != null) {
            AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low,
                    modifier =
                            Modifier.matchParentSize()
                                    .blur(backdropBlur)
                                    .alpha(backdropAlpha)
            )
        } else if (thumbnail != null) {
            Box(
                    modifier =
                            Modifier.matchParentSize()
                                    .alpha(backdropAlpha * 1.2f)
                                    .background(thumbnail.brush)
            )
        }
        if (imageRequest != null || thumbnail != null) {
            Box(
                    modifier =
                            Modifier.matchParentSize()
                                    .background(backdropScrimColor.copy(alpha = backdropScrimAlpha))
            )
        }
        if (imageRequest != null) {
            AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low,
                    modifier =
                            Modifier.align(Alignment.TopStart)
                                    .offset(x = 6.dp, y = 6.dp)
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                            1.dp,
                                            colors.textPrimary.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                    )
            )
        } else if (thumbnail != null) {
            Box(
                    modifier =
                            Modifier.align(Alignment.TopStart)
                                    .offset(x = 6.dp, y = 6.dp)
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(thumbnail.brush)
                                    .border(
                                            1.dp,
                                            colors.textPrimary.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                    )
            ) {
                Icon(
                        painter = painterResource(thumbnail.iconRes),
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.align(Alignment.Center).size(26.dp)
                )
            }
    }
    if (isFavorite) {
        FavoriteIndicator(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
    val labelStripColor = cardTextStripColor(colors, labelColor)
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(labelStripColor)
                            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Text(
                text = label,
                color = labelColor,
                fontSize = 16.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
        )
    }
    }
}

@Composable
private fun CategoryTypeTab(
        label: String,
        selected: Boolean,
        focusRequester: FocusRequester?,
        onFocused: (() -> Unit)? = null,
        onActivate: () -> Unit,
        onMoveLeft: (() -> Unit)? = null,
        onMoveRight: (() -> Unit)? = null,
        onMoveDown: (() -> Unit)? = null,
        onMoveUp: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val colors = AppTheme.colors
    val selectedTextColor =
            bestContrastText(colors.accentSelected, colors.textPrimary, colors.textOnAccent)
    val selectedOutlineColor = selectedTextColor.copy(alpha = 0.7f)
    val borderColor =
            when {
                isFocused -> colors.focus
                selected -> colors.accentSelected
                else -> colors.border
            }
    val backgroundColor =
            when {
                isFocused -> colors.accent
                selected -> colors.accentSelected
                else -> colors.surfaceAlt
            }
    val textColor =
            when {
                isFocused -> colors.textOnAccent
                selected -> selectedTextColor
                else -> colors.textPrimary
            }
    Box(
            modifier =
                    Modifier.then(
                                    if (focusRequester != null)
                                            Modifier.focusRequester(focusRequester)
                                    else Modifier
                            )
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    onFocused?.invoke()
                                }
                            }
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                val isSelectKey =
                                        it.key == Key.Enter ||
                                                it.key == Key.NumPadEnter ||
                                                it.key == Key.DirectionCenter
                                when (it.type) {
                                    KeyEventType.KeyUp -> isSelectKey
                                    KeyEventType.KeyDown -> {
                                        when {
                                            onMoveLeft != null && it.key == Key.DirectionLeft -> {
                                                onMoveLeft()
                                                true
                                            }
                                            onMoveRight != null && it.key == Key.DirectionRight -> {
                                                onMoveRight()
                                                true
                                            }
                                    onMoveDown != null && it.key == Key.DirectionDown -> {
                                        onMoveDown()
                                        true
                                    }
                                    onMoveUp != null && it.key == Key.DirectionUp -> {
                                        onMoveUp()
                                        true
                                    }
                                    isSelectKey -> {
                                        onActivate()
                                        true
                                    }
                                            else -> false
                                        }
                                    }
                                    else -> false
                                }
                            }
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onActivate
                            )
                            .clip(shape)
                            .background(backgroundColor)
                            .border(1.dp, borderColor, shape)
                            .then(
                                    if (selected) {
                                        Modifier.border(2.dp, selectedOutlineColor, shape)
                                    } else {
                                        Modifier
                                    }
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
                text = label,
                color = textColor,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun SearchInput(
        query: String,
        onQueryChange: (String) -> Unit,
        placeholder: String,
        focusRequester: FocusRequester,
        modifier: Modifier = Modifier,
        onMoveLeft: (() -> Unit)? = null,
        onMoveRight: (() -> Unit)? = null,
        onMoveDown: (() -> Unit)? = null,
        onMoveUp: (() -> Unit)? = null,
        onSearch: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val searchButtonFocusRequester = remember { FocusRequester() }
    val textFieldFocusRequester = remember { FocusRequester() }
    val wrapperInteractionSource = remember { MutableInteractionSource() }
    val isWrapperFocused by wrapperInteractionSource.collectIsFocusedAsState()
    var isTextFieldFocused by remember { mutableStateOf(false) }
    val searchButtonInteractionSource = remember { MutableInteractionSource() }
    val isSearchButtonFocused by searchButtonInteractionSource.collectIsFocusedAsState()
    val context = LocalContext.current
    val colors = AppTheme.colors
    val inputMethodManager =
            remember(context) {
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            }

    val isAnyFocused = isWrapperFocused || isTextFieldFocused || isSearchButtonFocused
    val borderColor = if (isAnyFocused) colors.focus else colors.borderStrong
    val backgroundColor = if (isAnyFocused) colors.surfaceAlt else colors.surface
    val triggerSearch = { onSearch?.invoke() }
    val showKeyboard = {
        // toggleSoftInput is more reliable on Android TV
        @Suppress("DEPRECATION")
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
    val activateTextField = {
        textFieldFocusRequester.requestFocus()
        showKeyboard()
    }

    Row(
            modifier =
                    modifier.clip(shape)
                            .background(backgroundColor)
                            .border(1.dp, borderColor, shape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Focusable wrapper that receives D-pad focus without showing keyboard
        Box(
                modifier =
                        Modifier.weight(1f)
                                .focusRequester(focusRequester)
                                .focusable(interactionSource = wrapperInteractionSource)
                                .onKeyEvent {
                                    if (it.type != KeyEventType.KeyDown) {
                                        false
                                    } else if (it.key == Key.Enter ||
                                                    it.key == Key.NumPadEnter ||
                                                    it.key == Key.DirectionCenter
                                    ) {
                                        activateTextField()
                                        true
                                    } else if (onMoveLeft != null && it.key == Key.DirectionLeft) {
                                        onMoveLeft()
                                        true
                                    } else if (it.key == Key.DirectionRight) {
                                        searchButtonFocusRequester.requestFocus()
                                        true
                                    } else if (onMoveUp != null && it.key == Key.DirectionUp) {
                                        onMoveUp()
                                        true
                                    } else if (onMoveDown != null && it.key == Key.DirectionDown) {
                                        onMoveDown()
                                        true
                                    } else {
                                        false
                                    }
                                },
                contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle =
                            TextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = AppTheme.fontFamily,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.3.sp
                            ),
                    cursorBrush = SolidColor(colors.focus),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { triggerSearch() }),
                    modifier =
                            Modifier.fillMaxWidth()
                                    .focusRequester(textFieldFocusRequester)
                                    .onKeyEvent {
                                        if (it.type != KeyEventType.KeyDown) {
                                            false
                                        } else if (onMoveLeft != null &&
                                                it.key == Key.DirectionLeft
                                        ) {
                                            onMoveLeft()
                                            true
                                        } else if (it.key == Key.DirectionRight) {
                                            searchButtonFocusRequester.requestFocus()
                                            true
                                        } else if (onMoveUp != null && it.key == Key.DirectionUp) {
                                            onMoveUp()
                                            true
                                        } else if (onMoveDown != null &&
                                                it.key == Key.DirectionDown
                                        ) {
                                            onMoveDown()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .onFocusChanged { state ->
                                        isTextFieldFocused = state.isFocused
                                        // Return to wrapper when text field loses focus
                                        if (!state.isFocused && isWrapperFocused.not()) {
                                            // Focus will naturally go elsewhere
                                        }
                                    },
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isBlank()) {
                                Text(
                                        text = placeholder,
                                        color = colors.textSecondary,
                                        fontSize = 13.sp,
                                        fontFamily = AppTheme.fontFamily,
                                        letterSpacing = 0.3.sp
                                )
                            }
                            innerTextField()
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Search button
        Box(
                modifier =
                        Modifier.size(24.dp)
                                .focusRequester(searchButtonFocusRequester)
                                .focusable(interactionSource = searchButtonInteractionSource)
                                .clickable(
                                        interactionSource = searchButtonInteractionSource,
                                        indication = null
                                ) { triggerSearch() }
                                .onKeyEvent {
                                    if (it.type != KeyEventType.KeyDown) {
                                        false
                                    } else if (it.key == Key.Enter ||
                                                    it.key == Key.NumPadEnter ||
                                                    it.key == Key.DirectionCenter
                                    ) {
                                        triggerSearch()
                                        true
                                    } else if (it.key == Key.DirectionLeft) {
                                        focusRequester.requestFocus()
                                        true
                                    } else if (onMoveRight != null && it.key == Key.DirectionRight
                                    ) {
                                        onMoveRight()
                                        true
                                    } else if (onMoveUp != null && it.key == Key.DirectionUp) {
                                        onMoveUp()
                                        true
                                    } else if (onMoveDown != null && it.key == Key.DirectionDown) {
                                        onMoveDown()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .background(
                                        color =
                                                if (isSearchButtonFocused) colors.focus
                                                else colors.accentMuted,
                                        shape = RoundedCornerShape(6.dp)
                                )
                                .padding(4.dp),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint =
                            if (isSearchButtonFocused) colors.surface
                            else colors.textPrimary,
                    modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SectionScreen(
        title: String,
        section: Section,
        contentRepository: ContentRepository,
        authConfig: AuthConfig,
        settings: SettingsState,
        navLayoutExpanded: Boolean,
        isPlaybackActive: Boolean,
        continueWatchingEntries: List<ContinueWatchingEntry>,
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onPlayWithPosition: (ContentItem, List<ContentItem>, Long?) -> Unit,
        onMovieInfo: (ContentItem, List<ContentItem>) -> Unit,
        onMoveLeft: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onRemoveContinueWatching: (ContentItem) -> Unit,
        onSeriesPlaybackStart: (ContentItem) -> Unit,
        isItemFavorite: (ContentItem) -> Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    val context = LocalContext.current
    // Live uses landscape cards (3 cols at 100%), Movies/Series use poster cards (4 cols at 100%)
    val baseColumns = remember(settings.uiScale, section) {
        if (section == Section.LIVE) {
            kotlin.math.ceil(3.0 / settings.uiScale).toInt().coerceIn(3, 6)
        } else {
            kotlin.math.ceil(4.0 / settings.uiScale).toInt().coerceIn(4, 8)
        }
    }
    val columns = rememberReflowColumns(baseColumns, navLayoutExpanded)
    val posterFontScale = remember(columns, section) {
        if (section == Section.LIVE) 3f / columns.toFloat() else 4f / columns.toFloat()
    }
    val searchState = rememberDebouncedSearchState(key = section)
    var selectedSeries by remember { mutableStateOf<ContentItem?>(null) }
    var pendingSeries by remember { mutableStateOf<ContentItem?>(null) }
    var pendingSeriesInfo by remember { mutableStateOf<SeriesInfo?>(null) }
    var pendingSeriesReturnFocus by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val episodesFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var pendingEpisodeFocus by remember { mutableStateOf(false) }
    LaunchedEffect(section) {
        selectedSeries = null
        pendingSeries = null
        pendingSeriesInfo = null
        pendingSeriesReturnFocus = false
        pendingEpisodeFocus = false
    }

    LaunchedEffect(pendingSeries?.streamId, authConfig) {
        val item = pendingSeries ?: return@LaunchedEffect
        val infoResult = runCatching { contentRepository.loadSeriesInfo(item, authConfig) }
        val info = infoResult.getOrNull()
        if (info == null) {
            val message =
                if (infoResult.exceptionOrNull() != null) {
                    "Failed to load content info"
                } else {
                    "No details available"
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        pendingSeriesInfo = info
        runCatching { contentRepository.loadSeriesSeasons(item.streamId, authConfig) }
        runCatching { contentRepository.loadSeriesEpisodes(item.streamId, authConfig) }
        if (pendingSeries?.streamId != item.streamId) return@LaunchedEffect
        pendingSeries = null
        selectedSeries = item
    }

    val activeQuery = searchState.debouncedQuery
    val pagerFlow =
            remember(section, authConfig, activeQuery) {
                if (activeQuery.isBlank()) {
                    contentRepository.pager(section, authConfig).flow
                } else {
                    contentRepository.searchPager(section, activeQuery, authConfig).flow
                }
            }
    val lazyItems = pagerFlow.collectAsLazyPagingItems()

    // Don't auto-focus content - user must press Right to navigate there

    BackHandler(enabled = selectedSeries != null) {
        selectedSeries?.let(onItemFocused)
        // Request focus immediately before state change to avoid focus flashing to MenuButton
        runCatching { contentItemFocusRequester.requestFocus() }
        pendingSeriesReturnFocus = true
        selectedSeries = null
    }

    LaunchedEffect(pendingSeriesReturnFocus, selectedSeries, lazyItems.itemCount, resumeFocusId) {
        if (pendingSeriesReturnFocus && selectedSeries == null) {
            // Wait for items to be available
            if (lazyItems.itemCount == 0) return@LaunchedEffect
            // Wait for composition to complete
            withFrameNanos {}
            delay(32)
            withFrameNanos {}
            val shouldResume =
                    resumeFocusId != null &&
                            lazyItems.itemSnapshotList.items.any { it.id == resumeFocusId }
            val requester =
                    if (shouldResume) {
                        resumeFocusRequester
                    } else {
                        contentItemFocusRequester
                    }
            // Retry focus request multiple times
            repeat(5) { attempt ->
                runCatching { requester.requestFocus() }
                if (attempt < 4) {
                    delay(32)
                    withFrameNanos {}
                }
            }
            pendingSeriesReturnFocus = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 12.dp)
                                .clip(shape)
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(AppTheme.colors.background, AppTheme.colors.backgroundAlt)
                                        )
                                )
                                .border(1.dp, AppTheme.colors.border, shape)
                                .padding(20.dp)
        ) {
            if (selectedSeries != null) {
                SeriesSeasonsScreen(
                        seriesItem = selectedSeries!!,
                        contentRepository = contentRepository,
                        authConfig = authConfig,
                        continueWatchingEntries = continueWatchingEntries,
                        contentItemFocusRequester = contentItemFocusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        episodesFocusRequester = episodesFocusRequester,
                        pendingEpisodeFocus = pendingEpisodeFocus,
                        onEpisodeFocusHandled = { pendingEpisodeFocus = false },
                        onItemFocused = onItemFocused,
                        onPlay = { playItem, items ->
                            onSeriesPlaybackStart(selectedSeries!!)
                            onPlay(playItem, items)
                        },
                        onPlayWithPosition = { playItem, items, position ->
                            onSeriesPlaybackStart(selectedSeries!!)
                            onPlayWithPosition(playItem, items, position)
                        },
                        onMoveLeft = onMoveLeft,
                        onBack = {
                            onItemFocused(selectedSeries!!)
                            runCatching { contentItemFocusRequester.requestFocus() }
                            pendingSeriesReturnFocus = true
                            selectedSeries = null
                            pendingSeriesInfo = null
                            pendingEpisodeFocus = false
                        },
                        onToggleFavorite = onToggleFavorite,
                        onRemoveContinueWatching = onRemoveContinueWatching,
                        isItemFavorite = isItemFavorite,
                        prefetchedInfo = pendingSeriesInfo
                )
            } else {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = title,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 20.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    SearchInput(
                            query = searchState.query,
                            onQueryChange = { searchState.query = it },
                            placeholder = "Search...",
                            focusRequester = searchFocusRequester,
                            modifier = Modifier.width(240.dp),
                            onMoveLeft = onMoveLeft,
                            onMoveDown = {
                                if (selectedSeries != null) {
                                    pendingEpisodeFocus = true
                                } else {
                                    contentItemFocusRequester.requestFocus()
                                }
                            },
                            onSearch = { searchState.performSearch() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (lazyItems.loadState.refresh is LoadState.Loading && lazyItems.itemCount == 0) {
                    Text(
                            text =
                                    if (activeQuery.isBlank()) "Loading content..."
                                    else "Searching...",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 14.sp,
                            fontFamily = AppTheme.fontFamily,
                            letterSpacing = 0.6.sp,
                            modifier =
                                    Modifier.focusRequester(contentItemFocusRequester).focusable()
                    )
                } else if (lazyItems.loadState.refresh is LoadState.Error) {
                    Text(
                            text =
                                    if (activeQuery.isBlank()) "Content failed to load"
                                    else "Search failed to load",
                            color = AppTheme.colors.error,
                            fontSize = 14.sp,
                            fontFamily = AppTheme.fontFamily,
                            letterSpacing = 0.6.sp,
                            modifier =
                                    Modifier.focusRequester(contentItemFocusRequester).focusable()
                    )
                } else if (lazyItems.itemCount == 0) {
                    Text(
                            text =
                                    if (activeQuery.isBlank()) "No content yet"
                                    else "No results found",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 14.sp,
                            fontFamily = AppTheme.fontFamily,
                            letterSpacing = 0.6.sp,
                            modifier =
                                    Modifier.focusRequester(contentItemFocusRequester).focusable()
                    )
                } else {
                    LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            state = gridState
                    ) {
                        items(
                                count = lazyItems.itemCount,
                                key = { index -> lazyItems[index]?.id ?: "item-$index" }
                        ) { index ->
                            val item = lazyItems[index]
                            val requester =
                                    when {
                                        item?.id != null && item.id == resumeFocusId ->
                                                resumeFocusRequester
                                        index == 0 -> contentItemFocusRequester
                                        else -> null
                                    }
                            val isLeftEdge = index % columns == 0
                            val isTopRow = index < columns
                            val subtitleOverride =
                                    rememberSeriesSubtitle(item, contentRepository, authConfig)
                            val useContrastText =
                                    section == Section.ALL &&
                                            (item?.contentType == ContentType.MOVIES ||
                                                    item?.contentType == ContentType.SERIES)
                            val posterHint =
                                    section == Section.ALL ||
                                            section == Section.MOVIES ||
                                            section == Section.SERIES
                            val forceDarkText =
                                    if (section == Section.ALL) {
                                        item?.contentType == ContentType.LIVE
                                    } else {
                                        section == Section.LIVE
                                    }
                            ContentCard(
                                    item = item,
                                    subtitleOverride = subtitleOverride,
                                    focusRequester = requester,
                                    isLeftEdge = isLeftEdge,
                                    isFavorite = item != null && isItemFavorite(item),
                                    onActivate =
                                            if (item != null) {
                                                {
                                                    if (item.contentType == ContentType.SERIES &&
                                                                    item.containerExtension
                                                                            .isNullOrBlank()
                                                    ) {
                                                        pendingSeries = item
                                                    } else if (item.contentType == ContentType.MOVIES) {
                                                        pendingSeries = null
                                                        onMovieInfo(
                                                                item,
                                                                lazyItems.itemSnapshotList.items
                                                        )
                                                    } else {
                                                        pendingSeries = null
                                                        onPlay(
                                                                item,
                                                                lazyItems.itemSnapshotList.items
                                                        )
                                                    }
                                                }
                                            } else {
                                                null
                                            },
                                    onFocused = onItemFocused,
                                    onMoveLeft = onMoveLeft,
                                    onMoveUp =
                                            if (isTopRow) {
                                                { searchFocusRequester.requestFocus() }
                                            } else {
                                                null
                                            },
                                    onLongClick =
                                            if (item != null) {
                                                { onToggleFavorite(item) }
                                            } else {
                                                null
                                            },
                                    forceDarkText = forceDarkText,
                                    useContrastText = useContrastText,
                                    isPoster = posterHint,
                                    fontScaleFactor = if (posterHint) posterFontScale else 1f,
                                    enabled = selectedSeries == null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LocalFilesScreen(
        title: String,
        settings: SettingsState,
        files: List<LocalFileItem>,
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onPickFiles: () -> Unit,
        onRefresh: () -> Unit,
        onPlayFile: (Int) -> Unit,
        onMoveLeft: () -> Unit,
        showBackButton: Boolean = false,
        onBack: () -> Unit = {}
) {
    val shape = RoundedCornerShape(18.dp)
    val backFocusRequester = remember { FocusRequester() }
    val scanFocusRequester = remember { FocusRequester() }
    val refreshFocusRequester = remember { FocusRequester() }
    val emptyFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val useStackedHeader = settings.uiScale >= 1.2f
    val handleMoveLeft: () -> Unit =
            if (showBackButton) {
                { backFocusRequester.requestFocus() }
            } else {
                onMoveLeft
            }

    // Focus is managed by user navigation - no auto-focus on screen load

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 12.dp)
                                .clip(shape)
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(AppTheme.colors.background, AppTheme.colors.backgroundAlt)
                                        )
                                )
                                .border(1.dp, AppTheme.colors.border, shape)
                                .padding(20.dp)
        ) {
            val backButton: @Composable () -> Unit = {
                if (showBackButton) {
                    FocusableButton(
                            onClick = onBack,
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = AppTheme.colors.accentMutedAlt,
                                            contentColor = AppTheme.colors.textPrimary
                                    ),
                            modifier =
                                    Modifier.height(40.dp)
                                            .focusRequester(backFocusRequester)
                                            .onPreviewKeyEvent {
                                                if (it.type != KeyEventType.KeyDown) {
                                                    false
                                                } else if (it.key == Key.DirectionRight ||
                                                        it.key == Key.DirectionDown
                                                ) {
                                                    if (it.key == Key.DirectionDown &&
                                                            files.isEmpty()
                                                    ) {
                                                        emptyFocusRequester.requestFocus()
                                                    } else {
                                                        contentItemFocusRequester.requestFocus()
                                                    }
                                                    true
                                                } else {
                                                    false
                                                }
                                            }
                    ) {
                        Text(
                                text = "Back",
                                fontSize = 13.sp,
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            val titleText: @Composable () -> Unit = {
                Text(
                        text = title,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 20.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                )
            }
            val scanButton: @Composable () -> Unit = {
                FocusableButton(
                        onClick = onPickFiles,
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = AppTheme.colors.accent,
                                        contentColor = AppTheme.colors.textOnAccent
                                ),
                        modifier =
                                Modifier.height(42.dp)
                                        .focusRequester(contentItemFocusRequester)
                                        .onKeyEvent {
                                            if (it.type != KeyEventType.KeyDown) {
                                                false
                                            } else if (it.key == Key.DirectionLeft) {
                                                handleMoveLeft()
                                                true
                                            } else if (it.key == Key.DirectionRight) {
                                                refreshFocusRequester.requestFocus()
                                                true
                                            } else if (it.key == Key.DirectionDown &&
                                                            files.isNotEmpty()
                                            ) {
                                                scanFocusRequester.requestFocus()
                                                true
                                            } else if (it.key == Key.DirectionDown &&
                                                            files.isEmpty()
                                            ) {
                                                emptyFocusRequester.requestFocus()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                ) {
                    Text(
                            text = "Scan for media",
                            fontSize = 14.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                    )
                }
            }
            val refreshButton: @Composable () -> Unit = {
                FocusableButton(
                        onClick = onRefresh,
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = AppTheme.colors.accentMuted,
                                        contentColor = AppTheme.colors.textPrimary
                                ),
                        modifier =
                                Modifier.height(42.dp)
                                        .focusRequester(refreshFocusRequester)
                                        .onKeyEvent {
                                            if (it.type != KeyEventType.KeyDown) {
                                                false
                                            } else if (it.key == Key.DirectionLeft) {
                                                contentItemFocusRequester.requestFocus()
                                                true
                                            } else if (it.key == Key.DirectionDown) {
                                                if (files.isNotEmpty()) {
                                                    scanFocusRequester.requestFocus()
                                                } else {
                                                    emptyFocusRequester.requestFocus()
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        }
                ) {
                    Text(
                            text = "Refresh",
                            fontSize = 14.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (useStackedHeader) {
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        backButton()
                        if (showBackButton) {
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        titleText()
                    }
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        scanButton()
                        refreshButton()
                    }
                }
            } else {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    backButton()
                    titleText()
                    Spacer(modifier = Modifier.weight(1f))
                    scanButton()
                    refreshButton()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (files.isEmpty()) {
                val emptyInteractionSource = remember { MutableInteractionSource() }
                val isEmptyFocused by emptyInteractionSource.collectIsFocusedAsState()
                val emptyBorderColor =
                        if (isEmptyFocused) AppTheme.colors.focus else AppTheme.colors.borderStrong
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .border(1.dp, emptyBorderColor, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                        .focusRequester(emptyFocusRequester)
                                        .focusable(interactionSource = emptyInteractionSource)
                                        .onKeyEvent {
                                            if (it.type != KeyEventType.KeyDown) {
                                                false
                                            } else if (it.key == Key.DirectionUp) {
                                                contentItemFocusRequester.requestFocus()
                                                true
                                            } else if (it.key == Key.DirectionLeft) {
                                                handleMoveLeft()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                ) {
                    Text(
                            text = "No media files found. Press the button to scan your device.",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 14.sp,
                            fontFamily = AppTheme.fontFamily,
                            letterSpacing = 0.6.sp
                    )
                }
            } else {
                // Group files by volume name
                val groupedFiles = files.groupBy { it.volumeName }
                var globalIndex = 0

                LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    groupedFiles.forEach { (volumeName, volumeFiles) ->
                        // Volume header
                        item(key = "header_$volumeName") {
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .padding(top = 8.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                        modifier =
                                                Modifier.weight(1f)
                                                        .height(1.dp)
                                                        .background(AppTheme.colors.borderStrong)
                                )
                                Text(
                                        text = volumeName,
                                        color = AppTheme.colors.textTertiary,
                                        fontSize = 12.sp,
                                        fontFamily = AppTheme.fontFamily,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Box(
                                        modifier =
                                                Modifier.weight(1f)
                                                        .height(1.dp)
                                                        .background(AppTheme.colors.borderStrong)
                                )
                            }
                        }

                        // Files in this volume
                        val startIndex = globalIndex
                        itemsIndexed(volumeFiles, key = { _, item -> item.uri }) { localIndex, item
                            ->
                            val itemIndex = startIndex + localIndex
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            var keyDownArmed by remember { mutableStateOf(false) }
                            var keyClickHandled by remember { mutableStateOf(false) }
                            val borderColor =
                                    if (isFocused) AppTheme.colors.focus else AppTheme.colors.borderStrong
                            val backgroundColor =
                                    if (isFocused) AppTheme.colors.border else AppTheme.colors.surface

                            val itemFocusRequester =
                                    when {
                                        item.uri.toString() == resumeFocusId -> resumeFocusRequester
                                        itemIndex == 0 -> scanFocusRequester
                                        else -> null
                                    }

                            val isFirstItem = itemIndex == 0
                            LaunchedEffect(isFocused) {
                                if (!isFocused) {
                                    keyDownArmed = false
                                    keyClickHandled = false
                                }
                            }

                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .then(
                                                            if (itemFocusRequester != null) {
                                                                Modifier.focusRequester(
                                                                        itemFocusRequester
                                                                )
                                                            } else {
                                                                Modifier
                                                            }
                                                    )
                                                    .focusable(
                                                            interactionSource = interactionSource
                                                    )
                                                    .onKeyEvent {
                                                        val isSelectKey =
                                                                it.key == Key.Enter ||
                                                                        it.key == Key.NumPadEnter ||
                                                                        it.key == Key.DirectionCenter
                                                        when (it.type) {
                                                            KeyEventType.KeyDown -> {
                                                                when {
                                                                    isSelectKey -> {
                                                                        keyDownArmed = true
                                                                        true
                                                                    }
                                                                    it.key == Key.DirectionLeft -> {
                                                                        handleMoveLeft()
                                                                        true
                                                                    }
                                                                    it.key == Key.DirectionUp &&
                                                                            isFirstItem -> {
                                                                        refreshFocusRequester.requestFocus()
                                                                        true
                                                                    }
                                                                    else -> false
                                                                }
                                                            }
                                                            KeyEventType.KeyUp -> {
                                                                if (isSelectKey && keyDownArmed) {
                                                                    keyDownArmed = false
                                                                    keyClickHandled = true
                                                                    onPlayFile(itemIndex)
                                                                    // Only suppress the next click briefly to avoid
                                                                    // eating a real pointer click after a key press.
                                                                    coroutineScope.launch {
                                                                        delay(120)
                                                                        keyClickHandled = false
                                                                    }
                                                                    true
                                                                } else {
                                                                    false
                                                                }
                                                            }
                                                            else -> false
                                                        }
                                                    }
                                                    .clickable(
                                                            interactionSource = interactionSource,
                                                            indication = null
                                                    ) {
                                                        if (keyClickHandled) {
                                                            keyClickHandled = false
                                                        } else {
                                                            onPlayFile(itemIndex)
                                                        }
                                                    }
                                                    .background(
                                                            backgroundColor,
                                                            RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                            1.dp,
                                                            borderColor,
                                                            RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Media type indicator
                                val mediaIcon =
                                        if (item.mediaType == LocalMediaType.AUDIO) "\uD83C\uDFB5"
                                        else "\uD83C\uDFAC"
                                Text(
                                        text = mediaIcon,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                        text = item.displayName,
                                        color = AppTheme.colors.textPrimary,
                                        fontSize = 15.sp,
                                        fontFamily = AppTheme.fontFamily,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        globalIndex += volumeFiles.size
                    }
                }
            }
        }
    }
}

private enum class FavoritesView {
    MENU,
    ITEMS,
    CATEGORIES
}

private data class ThumbnailSpec(val iconRes: Int, val brush: Brush)

private fun categoryThumbnail(type: ContentType): ThumbnailSpec {
    return when (type) {
        ContentType.LIVE ->
                ThumbnailSpec(
                        iconRes = R.drawable.ic_category_live,
                        brush =
                                Brush.linearGradient(
                                        colors = listOf(Color(0xFF00B3A6), Color(0xFF0E6E76))
                                )
                )
        ContentType.MOVIES ->
                ThumbnailSpec(
                        iconRes = R.drawable.ic_category_movies,
                        brush =
                                Brush.linearGradient(
                                        colors = listOf(Color(0xFFE9892A), Color(0xFF9E2A2B))
                                )
                )
        ContentType.SERIES ->
                ThumbnailSpec(
                        iconRes = R.drawable.ic_category_series,
                        brush =
                                Brush.linearGradient(
                                        colors = listOf(Color(0xFF7C6BFF), Color(0xFF2F2D6E))
                                )
                )
    }
}

@Composable
private fun favoritesMenuThumbnail(isItems: Boolean): ThumbnailSpec {
    return if (isItems) {
        ThumbnailSpec(
                iconRes = R.drawable.ic_favorites_items,
                brush = Brush.linearGradient(colors = listOf(AppTheme.colors.warning, Color(0xFFB86B1B)))
        )
    } else {
        ThumbnailSpec(
                iconRes = R.drawable.ic_favorites_categories,
                brush = Brush.linearGradient(colors = listOf(Color(0xFF70D6FF), Color(0xFF2D5B8A)))
        )
    }
}

@Composable
fun FavoritesScreen(
        title: String,
        contentRepository: ContentRepository,
        authConfig: AuthConfig,
        settings: SettingsState,
        navLayoutExpanded: Boolean,
        isPlaybackActive: Boolean,
        favoriteContentItems: List<ContentItem>,
        favoriteCategoryItems: List<CategoryItem>,
        hasFavoriteContentKeys: Boolean,
        hasFavoriteCategoryKeys: Boolean,
        continueWatchingEntries: List<ContinueWatchingEntry>,
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onPlayWithPosition: (ContentItem, List<ContentItem>, Long?) -> Unit,
        onMovieInfo: (ContentItem, List<ContentItem>) -> Unit,
        onMoveLeft: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onRemoveContinueWatching: (ContentItem) -> Unit,
        onToggleCategoryFavorite: (CategoryItem) -> Unit,
        isItemFavorite: (ContentItem) -> Boolean,
        onSeriesPlaybackStart: (ContentItem) -> Unit,
        isCategoryFavorite: (CategoryItem) -> Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    val context = LocalContext.current
    // Poster content scales with UI, categories stay fixed
    val basePosterColumns = remember(settings.uiScale) {
        kotlin.math.ceil(4.0 / settings.uiScale).toInt().coerceIn(4, 8)
    }
    val baseLiveColumns = remember(settings.uiScale) {
        kotlin.math.ceil(3.0 / settings.uiScale).toInt().coerceIn(3, 6)
    }
    val posterColumns = rememberReflowColumns(basePosterColumns, navLayoutExpanded)
    val liveColumns = rememberReflowColumns(baseLiveColumns, navLayoutExpanded)
    val posterFontScale = remember(posterColumns) { 4f / posterColumns.toFloat() }
    val categoryColumns = rememberReflowColumns(3, navLayoutExpanded)
    var activeView by remember { mutableStateOf(FavoritesView.MENU) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var selectedSeries by remember { mutableStateOf<ContentItem?>(null) }
    var pendingSeries by remember { mutableStateOf<ContentItem?>(null) }
    var pendingSeriesInfo by remember { mutableStateOf<SeriesInfo?>(null) }
    var pendingSeriesReturnFocus by remember { mutableStateOf(false) }
    var pendingViewFocus by remember { mutableStateOf(false) }
    var pendingCategoryEnterFocus by remember { mutableStateOf(false) }
    var lastMenuSelection by remember { mutableStateOf(FavoritesView.ITEMS) }
    val menuFocusRequesters = remember { listOf(FocusRequester(), FocusRequester()) }
    val backFocusRequester = remember { FocusRequester() }
    val episodesFocusRequester = remember { FocusRequester() }
    var pendingEpisodeFocus by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val categoryContentGridState = rememberLazyGridState()
    val sortedContent =
            remember(favoriteContentItems) {
                favoriteContentItems.sortedBy { it.title.lowercase() }
            }
    val sortedCategories =
            remember(favoriteCategoryItems) {
                favoriteCategoryItems.sortedBy { it.name.lowercase() }
            }

    LaunchedEffect(activeView) {
        if (activeView == FavoritesView.MENU) {
            selectedCategory = null
            selectedSeries = null
            pendingSeries = null
            pendingSeriesInfo = null
            pendingSeriesReturnFocus = false
            pendingCategoryEnterFocus = false
            pendingEpisodeFocus = false
        }
    }

    LaunchedEffect(pendingSeries?.streamId, authConfig) {
        val item = pendingSeries ?: return@LaunchedEffect
        val infoResult = runCatching { contentRepository.loadSeriesInfo(item, authConfig) }
        val info = infoResult.getOrNull()
        if (info == null) {
            val message =
                if (infoResult.exceptionOrNull() != null) {
                    "Failed to load content info"
                } else {
                    "No details available"
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        pendingSeriesInfo = info
        runCatching { contentRepository.loadSeriesSeasons(item.streamId, authConfig) }
        runCatching { contentRepository.loadSeriesEpisodes(item.streamId, authConfig) }
        if (pendingSeries?.streamId != item.streamId) return@LaunchedEffect
        pendingSeries = null
        selectedSeries = item
    }

    // Focus is managed by user navigation - no auto-focus on screen load

    BackHandler(
            enabled =
                    selectedSeries != null ||
                            selectedCategory != null ||
                            activeView != FavoritesView.MENU
    ) {
        if (selectedSeries != null) {
            onItemFocused(selectedSeries!!)
            // Request focus immediately before state change to avoid focus flashing to MenuButton
            runCatching { contentItemFocusRequester.requestFocus() }
            pendingSeriesReturnFocus = true
            selectedSeries = null
            pendingEpisodeFocus = false
        } else if (selectedCategory != null) {
            // Request focus immediately before state change
            runCatching { contentItemFocusRequester.requestFocus() }
            selectedCategory = null
            pendingViewFocus = true
            pendingCategoryEnterFocus = false
        } else {
            lastMenuSelection = activeView
            activeView = FavoritesView.MENU
            pendingViewFocus = true
            pendingCategoryEnterFocus = false
        }
    }

    LaunchedEffect(pendingSeriesReturnFocus, selectedSeries) {
        if (pendingSeriesReturnFocus && selectedSeries == null) {
            // Wait for composition to complete
            withFrameNanos {}
            delay(32)
            withFrameNanos {}
            val requester =
                    if (resumeFocusId != null) {
                        resumeFocusRequester
                    } else {
                        contentItemFocusRequester
                    }
            // Retry focus request multiple times
            repeat(5) { attempt ->
                runCatching { requester.requestFocus() }
                if (attempt < 4) {
                    delay(32)
                    withFrameNanos {}
                }
            }
            pendingSeriesReturnFocus = false
        }
    }

    LaunchedEffect(
            pendingViewFocus,
            activeView,
            selectedCategory,
            selectedSeries,
            sortedContent.size,
            sortedCategories.size,
            resumeFocusId
    ) {
        if (!pendingViewFocus || selectedSeries != null) return@LaunchedEffect
        if (activeView == FavoritesView.CATEGORIES && selectedCategory != null)
                return@LaunchedEffect
        withFrameNanos {}
        when (activeView) {
            FavoritesView.MENU -> {
                val requester =
                        if (lastMenuSelection == FavoritesView.CATEGORIES) {
                            menuFocusRequesters[1]
                        } else {
                            contentItemFocusRequester
                        }
                requester.requestFocus()
            }
            FavoritesView.ITEMS -> {
                val shouldResume =
                        resumeFocusId != null && sortedContent.any { it.id == resumeFocusId }
                val requester =
                        if (sortedContent.isEmpty()) {
                            contentItemFocusRequester
                        } else if (shouldResume) {
                            resumeFocusRequester
                        } else {
                            contentItemFocusRequester
                        }
                requester.requestFocus()
            }
            FavoritesView.CATEGORIES -> {
                val requester =
                        contentItemFocusRequester
                requester.requestFocus()
            }
        }
        pendingViewFocus = false
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 12.dp)
                                .clip(shape)
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(AppTheme.colors.background, AppTheme.colors.backgroundAlt)
                                        )
                                )
                                .border(1.dp, AppTheme.colors.border, shape)
                                .padding(20.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = title,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 20.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                if (activeView != FavoritesView.MENU) {
                    val backMoveDown: () -> Unit = { contentItemFocusRequester.requestFocus() }
                    CategoryTypeTab(
                            label = "Back",
                            selected = false,
                            focusRequester = backFocusRequester,
                            onActivate = {
                                when {
                                    selectedSeries != null -> {
                                        onItemFocused(selectedSeries!!)
                                        runCatching { contentItemFocusRequester.requestFocus() }
                                        pendingSeriesReturnFocus = true
                                        selectedSeries = null
                                    }
                                    selectedCategory != null -> {
                                        selectedCategory = null
                                        pendingCategoryEnterFocus = false
                                        pendingViewFocus = true
                                    }
                                    else -> {
                                        lastMenuSelection = activeView
                                        activeView = FavoritesView.MENU
                                        pendingCategoryEnterFocus = false
                                        pendingViewFocus = true
                                    }
                                }
                            },
                            onMoveLeft = onMoveLeft,
                            onMoveDown = backMoveDown
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (selectedSeries != null) {
                SeriesSeasonsScreen(
                        seriesItem = selectedSeries!!,
                        contentRepository = contentRepository,
                        authConfig = authConfig,
                        continueWatchingEntries = continueWatchingEntries,
                        contentItemFocusRequester = contentItemFocusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        episodesFocusRequester = episodesFocusRequester,
                        pendingEpisodeFocus = pendingEpisodeFocus,
                        onEpisodeFocusHandled = { pendingEpisodeFocus = false },
                        onItemFocused = onItemFocused,
                        onPlay = { playItem, items ->
                            onSeriesPlaybackStart(selectedSeries!!)
                            onPlay(playItem, items)
                        },
                        onPlayWithPosition = { playItem, items, position ->
                            onSeriesPlaybackStart(selectedSeries!!)
                            onPlayWithPosition(playItem, items, position)
                        },
                        onMoveLeft = onMoveLeft,
                        onBack = {
                            onItemFocused(selectedSeries!!)
                            runCatching { contentItemFocusRequester.requestFocus() }
                            pendingSeriesReturnFocus = true
                            selectedSeries = null
                            pendingSeriesInfo = null
                            pendingEpisodeFocus = false
                        },
                        onToggleFavorite = onToggleFavorite,
                        onRemoveContinueWatching = onRemoveContinueWatching,
                        isItemFavorite = isItemFavorite,
                        prefetchedInfo = pendingSeriesInfo
                )
            } else if (activeView == FavoritesView.MENU) {
                LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    item {
                        CategoryCard(
                                label = "Items",
                                focusRequester = contentItemFocusRequester,
                                isLeftEdge = true,
                                isFavorite = false,
                                thumbnail = favoritesMenuThumbnail(isItems = true),
                                imageUrl = null,
                                onActivate = {
                                    lastMenuSelection = FavoritesView.ITEMS
                                    selectedSeries = null
                                    selectedCategory = null
                                    activeView = FavoritesView.ITEMS
                                    pendingViewFocus = true
                                },
                                onMoveLeft = onMoveLeft,
                                onMoveDown = {
                                    lastMenuSelection = FavoritesView.ITEMS
                                    selectedSeries = null
                                    selectedCategory = null
                                    activeView = FavoritesView.ITEMS
                                    pendingViewFocus = true
                                },
                                forceDarkText = isLightTheme(AppTheme.colors)
                        )
                    }
                    item {
                        CategoryCard(
                                label = "Categories",
                                focusRequester = menuFocusRequesters[1],
                                isLeftEdge = false,
                                isFavorite = false,
                                thumbnail = favoritesMenuThumbnail(isItems = false),
                                imageUrl = null,
                                onActivate = {
                                    lastMenuSelection = FavoritesView.CATEGORIES
                                    selectedSeries = null
                                    selectedCategory = null
                                    activeView = FavoritesView.CATEGORIES
                                    pendingViewFocus = true
                                },
                                onMoveLeft = onMoveLeft,
                                onMoveDown = {
                                    lastMenuSelection = FavoritesView.CATEGORIES
                                    selectedSeries = null
                                    selectedCategory = null
                                    activeView = FavoritesView.CATEGORIES
                                    pendingViewFocus = true
                                },
                                forceDarkText = isLightTheme(AppTheme.colors)
                        )
                    }
                }
            } else if (activeView == FavoritesView.ITEMS) {
                if (sortedContent.isEmpty()) {
                    val message =
                            if (hasFavoriteContentKeys) {
                                "Favorites saved, but details are missing. Open items to refresh."
                            } else {
                                "No favorite items yet"
                            }
                    EmptyFavoritesState(
                            message = message,
                            focusRequester = contentItemFocusRequester,
                            onMoveUp = { backFocusRequester.requestFocus() },
                            onMoveLeft = onMoveLeft
                    )
                } else {
                    LazyVerticalGrid(
                            columns = GridCells.Fixed(posterColumns),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            state = gridState
                    ) {
                        items(
                                count = sortedContent.size,
                                key = { index -> sortedContent[index].id }
                        ) { index ->
                            val item = sortedContent[index]
                            val requester =
                                    when {
                                        item.id == resumeFocusId -> resumeFocusRequester
                                        index == 0 -> contentItemFocusRequester
                                        else -> null
                                    }
                            val isLeftEdge = index % posterColumns == 0
                            val isTopRow = index < posterColumns
                            val subtitleOverride =
                                    rememberSeriesSubtitle(item, contentRepository, authConfig)
                            val posterHint =
                                    item.contentType == ContentType.MOVIES ||
                                            (item.contentType == ContentType.SERIES &&
                                                    item.containerExtension.isNullOrBlank())
                            ContentCard(
                                    item = item,
                                    subtitleOverride = subtitleOverride,
                                    focusRequester = requester,
                                    isLeftEdge = isLeftEdge,
                                    isFavorite = isItemFavorite(item),
                                    onActivate = {
                                        if (item.contentType == ContentType.SERIES &&
                                                        item.containerExtension.isNullOrBlank()
                                        ) {
                                            pendingSeries = item
                                        } else if (item.contentType == ContentType.MOVIES) {
                                            pendingSeries = null
                                            onMovieInfo(item, sortedContent)
                                        } else {
                                            pendingSeries = null
                                            onPlay(item, sortedContent)
                                        }
                                    },
                                    onFocused = onItemFocused,
                                    onMoveLeft = onMoveLeft,
                                    onMoveUp =
                                            if (isTopRow) {
                                                { backFocusRequester.requestFocus() }
                                            } else {
                                                null
                                            },
                                    onLongClick = { onToggleFavorite(item) },
                                    isPoster = posterHint,
                                    fontScaleFactor = if (posterHint) posterFontScale else 1f
                            )
                        }
                    }
                }
            } else {
                if (selectedCategory != null) {
                    val category = selectedCategory!!

                    // Create pagerFlow and lazyItems OUTSIDE the selectedSeries conditional
                    // so they persist when entering/exiting SeriesSeasonsScreen
                    val pagerFlow =
                            remember(category.id, authConfig) {
                                contentRepository.categoryPager(
                                                category.type,
                                                category.id,
                                                authConfig
                                        )
                                        .flow
                            }
                    val lazyItems = pagerFlow.collectAsLazyPagingItems()

                    LaunchedEffect(
                            pendingCategoryEnterFocus,
                            lazyItems.itemCount,
                            lazyItems.loadState.refresh,
                            selectedSeries
                    ) {
                        if (!pendingCategoryEnterFocus || selectedSeries != null) {
                            return@LaunchedEffect
                        }
                        // Wait for items to be available before attempting focus
                        if (lazyItems.itemCount == 0) return@LaunchedEffect
                        withFrameNanos {}
                        delay(32)
                        withFrameNanos {}
                        repeat(5) { attempt ->
                            runCatching { contentItemFocusRequester.requestFocus() }
                            if (attempt < 4) {
                                delay(32)
                                withFrameNanos {}
                            }
                        }
                        pendingCategoryEnterFocus = false
                    }
                    LaunchedEffect(pendingSeriesReturnFocus, selectedSeries, lazyItems.itemCount, resumeFocusId) {
                        if (pendingSeriesReturnFocus && selectedSeries == null && selectedCategory != null) {
                            // Wait for items to be available
                            if (lazyItems.itemCount == 0) return@LaunchedEffect
                            // Wait for composition to complete
                            withFrameNanos {}
                            delay(32)
                            withFrameNanos {}
                            val shouldResume =
                                    resumeFocusId != null &&
                                            lazyItems.itemSnapshotList.items.any { it?.id == resumeFocusId }
                            val requester =
                                    if (shouldResume) {
                                        resumeFocusRequester
                                    } else {
                                        contentItemFocusRequester
                                    }
                            // Retry focus request multiple times
                            repeat(5) { attempt ->
                                runCatching { requester.requestFocus() }
                                if (attempt < 4) {
                                    delay(32)
                                    withFrameNanos {}
                                }
                            }
                            pendingSeriesReturnFocus = false
                        }
                    }

                    if (selectedSeries != null) {
                        SeriesSeasonsScreen(
                                seriesItem = selectedSeries!!,
                                contentRepository = contentRepository,
                                authConfig = authConfig,
                                continueWatchingEntries = continueWatchingEntries,
                                contentItemFocusRequester = contentItemFocusRequester,
                                resumeFocusId = resumeFocusId,
                                resumeFocusRequester = resumeFocusRequester,
                                episodesFocusRequester = episodesFocusRequester,
                                pendingEpisodeFocus = pendingEpisodeFocus,
                                onEpisodeFocusHandled = { pendingEpisodeFocus = false },
                                onItemFocused = onItemFocused,
                                onPlay = { playItem, items ->
                                    onSeriesPlaybackStart(selectedSeries!!)
                                    onPlay(playItem, items)
                                },
                                onPlayWithPosition = { playItem, items, position ->
                                    onSeriesPlaybackStart(selectedSeries!!)
                                    onPlayWithPosition(playItem, items, position)
                                },
                                onMoveLeft = onMoveLeft,
                                onBack = {
                                    onItemFocused(selectedSeries!!)
                                    runCatching { contentItemFocusRequester.requestFocus() }
                                    pendingSeriesReturnFocus = true
                                    selectedSeries = null
                                    pendingEpisodeFocus = false
                                },
                                onToggleFavorite = onToggleFavorite,
                                onRemoveContinueWatching = onRemoveContinueWatching,
                                isItemFavorite = isItemFavorite
                        )
                    } else {
                        // Focus is managed by user navigation - no auto-focus on content load
                        Text(
                                text = category.name,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (lazyItems.loadState.refresh is LoadState.Loading &&
                                        lazyItems.itemCount == 0
                        ) {
                            Text(
                                    text = "Loading content...",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily,
                                    letterSpacing = 0.6.sp,
                                    modifier =
                                            Modifier.focusRequester(contentItemFocusRequester)
                                                    .focusable()
                            )
                        } else if (lazyItems.loadState.refresh is LoadState.Error) {
                            Text(
                                    text = "Content failed to load",
                                    color = AppTheme.colors.error,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily,
                                    letterSpacing = 0.6.sp,
                                    modifier =
                                            Modifier.focusRequester(contentItemFocusRequester)
                                                    .focusable()
                            )
                        } else if (lazyItems.itemCount == 0) {
                            Text(
                                    text = "No content yet",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily,
                                    letterSpacing = 0.6.sp,
                                    modifier =
                                            Modifier.focusRequester(contentItemFocusRequester)
                                                    .focusable()
                            )
                        } else {
                            val contentColumns =
                                    if (category.type == ContentType.LIVE) {
                                        liveColumns
                                    } else {
                                        posterColumns
                                    }
                            LazyVerticalGrid(
                                    columns = GridCells.Fixed(contentColumns),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    state = categoryContentGridState
                            ) {
                                items(
                                        count = lazyItems.itemCount,
                                        key = { index ->
                                            lazyItems[index]?.id ?: "fav-cat-item-$index"
                                        }
                                ) { index ->
                                    val item = lazyItems[index]
                                    val requester =
                                            when {
                                                item?.id != null && item.id == resumeFocusId ->
                                                        resumeFocusRequester
                                                index == 0 -> contentItemFocusRequester
                                                else -> null
                                            }
                                    val isLeftEdge = index % contentColumns == 0
                                    val isTopRow = index < contentColumns
                                    val subtitleOverride =
                                            rememberSeriesSubtitle(
                                                    item,
                                                    contentRepository,
                                                    authConfig
                                            )
                                    val posterHint =
                                            category.type == ContentType.MOVIES ||
                                                    category.type == ContentType.SERIES
                                    ContentCard(
                                            item = item,
                                            subtitleOverride = subtitleOverride,
                                            focusRequester = requester,
                                            isLeftEdge = isLeftEdge,
                                            isFavorite = item != null && isItemFavorite(item),
                                            onActivate =
                                                    if (item != null) {
                                                        {
                                                            if (category.type ==
                                                                            ContentType.SERIES &&
                                                                            item.containerExtension
                                                                                    .isNullOrBlank()
                                                            ) {
                                                                pendingSeries = item
                                                            } else if (category.type == ContentType.MOVIES) {
                                                                pendingSeries = null
                                                                onMovieInfo(
                                                                        item,
                                                                        lazyItems
                                                                                .itemSnapshotList
                                                                                .items
                                                                )
                                                            } else {
                                                                pendingSeries = null
                                                                onPlay(
                                                                        item,
                                                                        lazyItems
                                                                                .itemSnapshotList
                                                                                .items
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        null
                                                    },
                                            onFocused = onItemFocused,
                                            onMoveLeft = onMoveLeft,
                                            onMoveUp =
                                                    if (isTopRow) {
                                                        { backFocusRequester.requestFocus() }
                                                    } else {
                                                        null
                                                    },
                                            onLongClick =
                                                    if (item != null) {
                                                        { onToggleFavorite(item) }
                                                    } else {
                                                        null
                                                    },
                                            forceDarkText = category.type == ContentType.LIVE,
                                            useContrastText =
                                                    category.type == ContentType.MOVIES ||
                                                            category.type == ContentType.SERIES,
                                            isPoster = posterHint,
                                            fontScaleFactor = if (posterHint) posterFontScale else 1f
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (sortedCategories.isEmpty()) {
                        val message =
                                if (hasFavoriteCategoryKeys) {
                                    "Favorites saved, but details are missing. Open categories to refresh."
                                } else {
                                    "No favorite categories yet"
                                }
                        EmptyFavoritesState(
                                message = message,
                                focusRequester = contentItemFocusRequester,
                                onMoveUp = { backFocusRequester.requestFocus() },
                                onMoveLeft = onMoveLeft
                        )
                    } else {
                        LazyVerticalGrid(
                                columns = GridCells.Fixed(categoryColumns),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                state = gridState
                        ) {
                            items(sortedCategories.size) { index ->
                                val category = sortedCategories[index]
                                val requester = if (index == 0) contentItemFocusRequester else null
                                val isLeftEdge = index % categoryColumns == 0
                                val isTopRow = index < categoryColumns
                                val categoryThumbnailUrl by
                                        produceState<String?>(
                                                initialValue = null,
                                                key1 = category.id,
                                                key2 = category.type,
                                                key3 = authConfig
                                        ) {
                                            value =
                                                    contentRepository.categoryThumbnail(
                                                            category.type,
                                                            category.id,
                                                            authConfig
                                                    )
                                        }
                                CategoryCard(
                                        label = category.name,
                                        focusRequester = requester,
                                        isLeftEdge = isLeftEdge,
                                        isFavorite = isCategoryFavorite(category),
                                        thumbnail = categoryThumbnail(category.type),
                                        imageUrl = categoryThumbnailUrl,
                                        onActivate = {
                                            selectedSeries = null
                                            selectedCategory = category
                                            pendingCategoryEnterFocus = true
                                        },
                                        onMoveLeft = onMoveLeft,
                                        onMoveUp =
                                                if (isTopRow) {
                                                    { backFocusRequester.requestFocus() }
                                                } else {
                                                    null
                                                },
                                        onLongClick = { onToggleCategoryFavorite(category) },
                                        forceDarkText = category.type == ContentType.LIVE,
                                        useContrastText =
                                                category.type == ContentType.MOVIES ||
                                                        category.type == ContentType.SERIES
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.EmptyFavoritesState(
        message: String,
        focusRequester: FocusRequester,
        onMoveUp: (() -> Unit)? = null,
        onMoveLeft: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (isFocused) AppTheme.colors.focus else AppTheme.colors.borderStrong
    val backgroundColor = AppTheme.colors.surface
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                if (it.type != KeyEventType.KeyDown) {
                                    false
                                } else if (it.key == Key.DirectionUp && onMoveUp != null) {
                                    onMoveUp()
                                    true
                                } else if (it.key == Key.DirectionLeft && onMoveLeft != null) {
                                    onMoveLeft()
                                    true
                                } else {
                                    false
                                }
                            }
                            .clip(shape)
                            .background(backgroundColor)
                            .border(1.dp, borderColor, shape)
                            .padding(16.dp),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = message,
                color = AppTheme.colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = AppTheme.fontFamily,
                letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun CategorySectionScreen(
        title: String,
        contentRepository: ContentRepository,
        authConfig: AuthConfig,
        settings: SettingsState,
        navLayoutExpanded: Boolean,
        isPlaybackActive: Boolean,
        continueWatchingEntries: List<ContinueWatchingEntry>,
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onPlayWithPosition: (ContentItem, List<ContentItem>, Long?) -> Unit,
        onMovieInfo: (ContentItem, List<ContentItem>) -> Unit,
        onMoveLeft: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onRemoveContinueWatching: (ContentItem) -> Unit,
        onToggleCategoryFavorite: (CategoryItem) -> Unit,
        isItemFavorite: (ContentItem) -> Boolean,
        onSeriesPlaybackStart: (ContentItem) -> Unit,
        isCategoryFavorite: (CategoryItem) -> Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    val context = LocalContext.current
    var activeType by remember { mutableStateOf(ContentType.LIVE) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var selectedSeries by remember { mutableStateOf<ContentItem?>(null) }
    var pendingSeries by remember { mutableStateOf<ContentItem?>(null) }
    var pendingSeriesInfo by remember { mutableStateOf<SeriesInfo?>(null) }
    var pendingSeriesReturnFocus by remember { mutableStateOf(false) }
    var pendingCategoryReturnFocus by remember { mutableStateOf(false) }
    var pendingCategoryEnterFocus by remember { mutableStateOf(false) }
    var lastCategoryContentIndex by remember { mutableIntStateOf(0) }
    var lastCategoryContentId by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val searchState = rememberDebouncedSearchState(key = activeType)
    // Live uses landscape cards (3 cols at 100%), Movies/Series use poster cards (4 cols at 100%)
    val basePosterColumns = remember(settings.uiScale, activeType) {
        if (activeType == ContentType.LIVE) {
            kotlin.math.ceil(3.0 / settings.uiScale).toInt().coerceIn(3, 6)
        } else {
            kotlin.math.ceil(4.0 / settings.uiScale).toInt().coerceIn(4, 8)
        }
    }
    val posterColumns = rememberReflowColumns(basePosterColumns, navLayoutExpanded)
    val posterFontScale = remember(posterColumns, activeType) {
        if (activeType == ContentType.LIVE) 3f / posterColumns.toFloat() else 4f / posterColumns.toFloat()
    }
    val categoryColumns = rememberReflowColumns(3, navLayoutExpanded)
    val categoryGridState = rememberLazyGridState()
    val contentGridState = rememberLazyGridState()
    val tabFocusRequesters = remember { ContentType.values().map { FocusRequester() } }
    val backTabFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val searchDownCategoryFocusRequester = remember { FocusRequester() }
    val searchDownContentFocusRequester = remember { FocusRequester() }
    val episodesFocusRequester = remember { FocusRequester() }
    var pendingEpisodeFocus by remember { mutableStateOf(false) }
    val useStackedHeader = settings.uiScale >= 1.2f
    val selectedTypeTabFocusRequester = tabFocusRequesters.getOrNull(activeType.ordinal)

    val activeQuery = searchState.debouncedQuery

    BackHandler(enabled = selectedCategory != null && selectedSeries == null) {
        // Request focus immediately before state change to avoid focus flashing to MenuButton
        runCatching { contentItemFocusRequester.requestFocus() }
        selectedCategory = null
        pendingCategoryReturnFocus = true
    }

    LaunchedEffect(activeType, authConfig) {
        isLoading = true
        errorMessage = null
        selectedCategory = null
        selectedSeries = null
        pendingSeries = null
        pendingSeriesInfo = null
        pendingSeriesReturnFocus = false
        pendingCategoryReturnFocus = false
        pendingCategoryEnterFocus = false
        pendingEpisodeFocus = false
        lastCategoryContentIndex = 0
        lastCategoryContentId = null
        runCatching { contentRepository.loadCategories(activeType, authConfig) }
                .onSuccess { categories = it }
                .onFailure { errorMessage = it.message ?: "Failed to load categories" }
        isLoading = false
    }

    LaunchedEffect(pendingSeries?.streamId, authConfig) {
        val item = pendingSeries ?: return@LaunchedEffect
        val infoResult = runCatching { contentRepository.loadSeriesInfo(item, authConfig) }
        val info = infoResult.getOrNull()
        if (info == null) {
            val message =
                if (infoResult.exceptionOrNull() != null) {
                    "Failed to load content info"
                } else {
                    "No details available"
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        pendingSeriesInfo = info
        runCatching { contentRepository.loadSeriesSeasons(item.streamId, authConfig) }
        runCatching { contentRepository.loadSeriesEpisodes(item.streamId, authConfig) }
        if (pendingSeries?.streamId != item.streamId) return@LaunchedEffect
        pendingSeries = null
        selectedSeries = item
    }

    // Focus restore logic moved inside content grid block to access lazyItems

    LaunchedEffect(
            pendingCategoryReturnFocus,
            selectedCategory,
            selectedSeries,
            categories.size,
            activeQuery
    ) {
        if (!pendingCategoryReturnFocus || selectedCategory != null || selectedSeries != null) {
            return@LaunchedEffect
        }
        // Wait for categories to be available
        if (categories.isEmpty()) return@LaunchedEffect
        // Wait for composition to complete
        withFrameNanos {}
        delay(32)
        withFrameNanos {}
        // Retry focus request multiple times
        repeat(5) { attempt ->
            runCatching { contentItemFocusRequester.requestFocus() }
            if (attempt < 4) {
                delay(32)
                withFrameNanos {}
            }
        }
        pendingCategoryReturnFocus = false
    }

    // Don't auto-focus content when category changes - user must press Right to navigate there

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 12.dp)
                                .clip(shape)
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(AppTheme.colors.background, AppTheme.colors.backgroundAlt)
                                        )
                                )
                                .border(1.dp, AppTheme.colors.border, shape)
                                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val tabsContent: @Composable () -> Unit = tabs@{
                    if (selectedSeries != null) return@tabs
                    if (selectedCategory != null) {
                        CategoryTypeTab(
                                label = "Back",
                                selected = false,
                                focusRequester = backTabFocusRequester,
                                onActivate = {
                                    if (selectedSeries != null) {
                                        val seriesItem = selectedSeries!!
                                        onItemFocused(seriesItem)
                                        runCatching { contentItemFocusRequester.requestFocus() }
                                        pendingSeriesReturnFocus = true
                                        selectedSeries = null
                                    } else {
                                        selectedCategory = null
                                        pendingCategoryReturnFocus = true
                                    }
                                },
                                onMoveLeft = { searchFocusRequester.requestFocus() },
                                onMoveRight = { tabFocusRequesters.first().requestFocus() }
                        )
                    }
                    ContentType.values().forEachIndexed { index, type ->
                        val requester = tabFocusRequesters[index]
                        CategoryTypeTab(
                                label = type.label,
                                selected = activeType == type,
                                focusRequester = requester,
                                onActivate = { activeType = type },
                                onMoveLeft =
                                        if (index > 0) {
                                            { tabFocusRequesters[index - 1].requestFocus() }
                                        } else if (selectedCategory != null) {
                                            { backTabFocusRequester.requestFocus() }
                                        } else {
                                            { searchFocusRequester.requestFocus() }
                                        },
                                onMoveRight =
                                        if (index < tabFocusRequesters.lastIndex) {
                                            { tabFocusRequesters[index + 1].requestFocus() }
                                        } else {
                                            null
                                        }
                        )
                    }
                }

                if (useStackedHeader) {
                    Text(
                            text = title,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 20.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (selectedSeries == null) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                tabsContent()
                            }
                        }
                    }
                } else {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                text = title,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 20.sp,
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (selectedSeries == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                tabsContent()
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedSeries == null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        SearchInput(
                                query = searchState.query,
                                onQueryChange = { searchState.query = it },
                                placeholder = "Search...",
                                focusRequester = searchFocusRequester,
                                modifier = Modifier.width(240.dp),
                                onMoveLeft = onMoveLeft,
                                onMoveRight = {
                                    if (selectedCategory != null) {
                                        backTabFocusRequester.requestFocus()
                                    } else {
                                        selectedTypeTabFocusRequester?.requestFocus()
                                                ?: tabFocusRequesters.firstOrNull()?.requestFocus()
                                    }
                                },
                                onMoveUp = {
                                    if (selectedCategory != null) {
                                        backTabFocusRequester.requestFocus()
                                    } else {
                                        selectedTypeTabFocusRequester?.requestFocus()
                                                ?: tabFocusRequesters.firstOrNull()?.requestFocus()
                                    }
                                },
                                onMoveDown = {
                                    if (selectedSeries != null) {
                                        pendingEpisodeFocus = true
                                    } else if (activeType == ContentType.SERIES) {
                                        contentItemFocusRequester.requestFocus()
                                    } else if (selectedCategory != null) {
                                        searchDownContentFocusRequester.requestFocus()
                                    } else {
                                        searchDownCategoryFocusRequester.requestFocus()
                                    }
                                },
                                onSearch = { searchState.performSearch() }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (selectedCategory != null) {
                val category = selectedCategory!!
                val useContrastText = activeType == ContentType.MOVIES || activeType == ContentType.SERIES
                val forceDarkText = activeType == ContentType.LIVE

                // Create pagerFlow and lazyItems OUTSIDE the selectedSeries conditional
                // so they persist when entering/exiting SeriesSeasonsScreen
                val pagerFlow =
                        remember(category.id, activeType, authConfig, activeQuery) {
                            if (activeQuery.isBlank()) {
                                contentRepository.categoryPager(
                                                activeType,
                                                category.id,
                                                authConfig
                                        )
                                        .flow
                            } else {
                                contentRepository.categorySearchPager(
                                                activeType,
                                                category.id,
                                                activeQuery,
                                                authConfig
                                        )
                                        .flow
                            }
                        }
                val lazyItems = pagerFlow.collectAsLazyPagingItems()

                LaunchedEffect(
                        pendingCategoryEnterFocus,
                        lazyItems.itemCount,
                        lazyItems.loadState.refresh
                ) {
                    if (!pendingCategoryEnterFocus || selectedSeries != null)
                            return@LaunchedEffect
                    // Wait for items to be available before attempting focus
                    if (lazyItems.itemCount == 0) return@LaunchedEffect
                    // Wait for composition to complete with multiple frame delays
                    withFrameNanos {}
                    delay(32)
                    withFrameNanos {}
                    // Retry focus request multiple times to handle composition timing
                    repeat(5) { attempt ->
                        runCatching { contentItemFocusRequester.requestFocus() }
                        if (attempt < 4) {
                            delay(32)
                            withFrameNanos {}
                        }
                    }
                    pendingCategoryEnterFocus = false
                }
                LaunchedEffect(
                        pendingSeriesReturnFocus,
                        selectedSeries,
                        lazyItems.itemCount,
                        resumeFocusId
                ) {
                    if (pendingSeriesReturnFocus && selectedSeries == null) {
                        // Wait for items to be available
                        if (lazyItems.itemCount == 0) return@LaunchedEffect
                        val itemsSnapshot = lazyItems.itemSnapshotList.items
                        val lastId = lastCategoryContentId
                        val lastIdIndex =
                                if (lastId != null) {
                                    itemsSnapshot.indexOfFirst { it?.id == lastId }
                                } else {
                                    -1
                                }
                        val targetIndex =
                                when {
                                    lastIdIndex >= 0 -> lastIdIndex
                                    lastCategoryContentIndex > 0 ->
                                            lastCategoryContentIndex.coerceAtMost(
                                                    (lazyItems.itemCount - 1).coerceAtLeast(0)
                                            )
                                    else -> 0
                                }
                        if (targetIndex > 0) {
                            contentGridState.scrollToItem(targetIndex)
                        }
                        // Wait for composition to complete
                        withFrameNanos {}
                        delay(32)
                        withFrameNanos {}
                        val resumeId = lastId ?: resumeFocusId
                        val shouldResume =
                                resumeId != null && itemsSnapshot.any { it?.id == resumeId }
                        val requester =
                                if (shouldResume) {
                                    resumeFocusRequester
                                } else {
                                    contentItemFocusRequester
                                }
                        // Retry focus request multiple times
                        repeat(5) { attempt ->
                            runCatching { requester.requestFocus() }
                            if (attempt < 4) {
                                delay(32)
                                withFrameNanos {}
                            }
                        }
                        pendingSeriesReturnFocus = false
                    }
                }

                // Use Box to overlay SeriesSeasonsScreen while keeping content grid in composition
                // This preserves scroll position when navigating back from series detail
                Box(modifier = Modifier.weight(1f)) {
                    // Content grid column - always in composition
                    Column(
                            modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(if (selectedSeries != null) 0f else 1f)
                    ) {
                        // Don't auto-focus content - user must press Right to navigate there
                        Text(
                                text = if (activeQuery.isBlank()) category.name else "Search results",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (lazyItems.loadState.refresh is LoadState.Loading && lazyItems.itemCount == 0
                        ) {
                            Text(
                                    text =
                                            if (activeQuery.isBlank()) "Loading content..."
                                            else "Searching...",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily,
                                    letterSpacing = 0.6.sp,
                                    modifier =
                                            if (selectedSeries == null)
                                                Modifier.focusRequester(contentItemFocusRequester)
                                                        .focusable()
                                            else Modifier
                            )
                        } else if (lazyItems.loadState.refresh is LoadState.Error) {
                            Text(
                                    text =
                                            if (activeQuery.isBlank()) "Content failed to load"
                                            else "Search failed to load",
                                    color = AppTheme.colors.error,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily,
                                    letterSpacing = 0.6.sp,
                                    modifier =
                                            if (selectedSeries == null)
                                                Modifier.focusRequester(contentItemFocusRequester)
                                                        .focusable()
                                            else Modifier
                            )
                        } else if (lazyItems.itemCount == 0) {
                            Text(
                                    text =
                                            if (activeQuery.isBlank()) "No content yet"
                                            else "No results found",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily,
                                    letterSpacing = 0.6.sp,
                                    modifier =
                                            if (selectedSeries == null)
                                                Modifier.focusRequester(contentItemFocusRequester)
                                                        .focusable()
                                            else Modifier
                            )
                        } else {
                            LazyVerticalGrid(
                                    columns = GridCells.Fixed(posterColumns),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    state = contentGridState,
                                    userScrollEnabled = selectedSeries == null
                            ) {
                                items(
                                        count = lazyItems.itemCount,
                                        key = { index -> lazyItems[index]?.id ?: "cat-item-$index" }
                                ) { index ->
                                    val item = lazyItems[index]
                            val isLeftEdge = index % posterColumns == 0
                            val isTopRow = index < posterColumns
                                    val searchDownIndex =
                                            if (activeType != ContentType.SERIES && lazyItems.itemCount > 1) {
                                                (posterColumns - 1).coerceAtMost(lazyItems.itemCount - 1)
                                            } else {
                                                -1
                                            }
                                    val requester =
                                            if (selectedSeries == null) {
                                                when {
                                                    item?.id != null &&
                                                            (item.id == lastCategoryContentId ||
                                                                    item.id == resumeFocusId) ->
                                                            resumeFocusRequester
                                                    index == searchDownIndex ->
                                                            searchDownContentFocusRequester
                                                    index == 0 -> contentItemFocusRequester
                                                    else -> null
                                                }
                                            } else null
                                    val subtitleOverride =
                                            rememberSeriesSubtitle(
                                                    item,
                                                    contentRepository,
                                                    authConfig
                                            )
                                    val posterHint =
                                            activeType == ContentType.MOVIES ||
                                                    activeType == ContentType.SERIES
                                    ContentCard(
                                            item = item,
                                            subtitleOverride = subtitleOverride,
                                            focusRequester = requester,
                                            isLeftEdge = isLeftEdge,
                                            isFavorite = item != null && isItemFavorite(item),
                                            onActivate =
                                                    if (item != null && selectedSeries == null) {
                                                        {
                                                            if (activeType == ContentType.SERIES &&
                                                                            item.containerExtension
                                                                                    .isNullOrBlank()
                                                            ) {
                                                                pendingSeries = item
                                                            } else if (activeType == ContentType.MOVIES) {
                                                                pendingSeries = null
                                                                onMovieInfo(
                                                                        item,
                                                                        lazyItems.itemSnapshotList.items
                                                                )
                                                            } else {
                                                                pendingSeries = null
                                                                onPlay(
                                                                        item,
                                                                        lazyItems.itemSnapshotList.items
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        null
                                                    },
                                            onFocused =
                                                    if (selectedSeries == null) {
                                                        { focusedItem ->
                                                            lastCategoryContentIndex = index
                                                            lastCategoryContentId = focusedItem.id
                                                            onItemFocused(focusedItem)
                                                        }
                                                    } else {
                                                        { _ -> }
                                                    },
                                            onMoveLeft =
                                                    { if (selectedSeries == null) onMoveLeft() },
                                            onMoveUp =
                                                    if (isTopRow && selectedSeries == null) {
                                                        { searchFocusRequester.requestFocus() }
                                                    } else {
                                                        null
                                                    },
                                            onLongClick =
                                                    if (item != null && selectedSeries == null) {
                                                        { onToggleFavorite(item) }
                                                    } else {
                                                        null
                                            },
                                            forceDarkText = forceDarkText,
                                            useContrastText = useContrastText,
                                            isPoster = posterHint,
                                            fontScaleFactor = if (posterHint) posterFontScale else 1f
                                    )
                                }
                            }
                        }
                    }

                    // SeriesSeasonsScreen overlay - shown on top when selected
                    if (selectedSeries != null) {
                        SeriesSeasonsScreen(
                                seriesItem = selectedSeries!!,
                                contentRepository = contentRepository,
                                authConfig = authConfig,
                                continueWatchingEntries = continueWatchingEntries,
                                contentItemFocusRequester = contentItemFocusRequester,
                                resumeFocusId = resumeFocusId,
                                resumeFocusRequester = resumeFocusRequester,
                                episodesFocusRequester = episodesFocusRequester,
                                pendingEpisodeFocus = pendingEpisodeFocus,
                                onEpisodeFocusHandled = { pendingEpisodeFocus = false },
                                onItemFocused = onItemFocused,
                                onPlay = { playItem, items ->
                                    onSeriesPlaybackStart(selectedSeries!!)
                                    onPlay(playItem, items)
                                },
                                onPlayWithPosition = { playItem, items, position ->
                                    onSeriesPlaybackStart(selectedSeries!!)
                                    onPlayWithPosition(playItem, items, position)
                                },
                                onMoveLeft = onMoveLeft,
                                onBack = {
                                    onItemFocused(selectedSeries!!)
                                    runCatching { contentItemFocusRequester.requestFocus() }
                                    pendingSeriesReturnFocus = true
                                    selectedSeries = null
                                    pendingSeriesInfo = null
                                    pendingEpisodeFocus = false
                                },
                                onToggleFavorite = onToggleFavorite,
                                onRemoveContinueWatching = onRemoveContinueWatching,
                                isItemFavorite = isItemFavorite,
                                prefetchedInfo = pendingSeriesInfo,
                                forceDarkText = forceDarkText,
                                onMoveUpFromTop = { searchFocusRequester.requestFocus() }
                        )
                    }
                }
            } else if (isLoading) {
                Text(
                        text = "Loading categories...",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.focusRequester(contentItemFocusRequester).focusable()
                )
            } else if (errorMessage != null) {
                Text(
                        text = errorMessage ?: "Failed to load categories",
                        color = AppTheme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.focusRequester(contentItemFocusRequester).focusable()
                )
            } else {
                val filteredCategoriesState =
                        remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
                LaunchedEffect(categories, activeQuery) {
                    filteredCategoriesState.value =
                            if (activeQuery.isBlank()) {
                                categories
                            } else {
                                withContext(Dispatchers.Default) {
                                    categories.filter {
                                        SearchNormalizer.matchesTitle(it.name, activeQuery)
                                    }
                                }
                            }
                }
                val filteredCategories = filteredCategoriesState.value
                // Focus is managed by user navigation - no auto-focus on content load
                if (activeQuery.isNotBlank() && filteredCategories.isEmpty()) {
                    Text(
                            text = "No categories found",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 14.sp,
                            fontFamily = AppTheme.fontFamily,
                            letterSpacing = 0.6.sp,
                            modifier =
                                    Modifier.focusRequester(contentItemFocusRequester).focusable()
                    )
                } else {
                    val searchDownIndex =
                            if (activeType != ContentType.SERIES && filteredCategories.size > 1) {
                                (categoryColumns - 1).coerceAtMost(filteredCategories.lastIndex)
                            } else {
                                -1
                            }
                    LazyVerticalGrid(
                            columns = GridCells.Fixed(categoryColumns),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            state = categoryGridState
                    ) {
                        items(filteredCategories.size) { index ->
                            val category = filteredCategories[index]
                            val requester =
                                    when {
                                        index == searchDownIndex -> searchDownCategoryFocusRequester
                                        index == 0 -> contentItemFocusRequester
                                        else -> null
                                    }
                            val isLeftEdge = index % categoryColumns == 0
                            val isTopRow = index < categoryColumns
                            val categoryThumbnailUrl by
                                    produceState<String?>(
                                            initialValue = null,
                                            key1 = category.id,
                                            key2 = category.type,
                                            key3 = authConfig
                                    ) {
                                        value =
                                                contentRepository.categoryThumbnail(
                                                        category.type,
                                                        category.id,
                                                        authConfig
                                                )
                                    }
                            CategoryCard(
                                    label = category.name,
                                    focusRequester = requester,
                                    isLeftEdge = isLeftEdge,
                                    isFavorite = isCategoryFavorite(category),
                                    thumbnail = categoryThumbnail(category.type),
                                    imageUrl = categoryThumbnailUrl,
                                    onActivate = {
                                        selectedSeries = null
                                        selectedCategory = category
                                        pendingCategoryEnterFocus = true
                                        lastCategoryContentIndex = 0
                                        lastCategoryContentId = null
                                    },
                                    onMoveLeft = onMoveLeft,
                                    onMoveUp =
                                            if (isTopRow) {
                                                { searchFocusRequester.requestFocus() }
                                            } else {
                                                null
                                            },
                                    onLongClick = { onToggleCategoryFavorite(category) },
                                    forceDarkText = activeType == ContentType.LIVE,
                                    useContrastText =
                                            activeType == ContentType.MOVIES ||
                                                    activeType == ContentType.SERIES
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ContinueWatchingDisplayEntry(
        val key: String,
        val displayItem: ContentItem,
        val resumeItem: ContentItem,
        val parentItem: ContentItem?,
        val sourceItems: List<ContentItem>,
        val positionMs: Long,
        val durationMs: Long,
        val timestampMs: Long
)

@Composable
fun ContinueWatchingScreen(
        title: String,
        contentRepository: ContentRepository,
        authConfig: AuthConfig,
        settings: SettingsState,
        navLayoutExpanded: Boolean,
        continueWatchingItems: List<ContinueWatchingEntry>,
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, Long, ContentItem?) -> Unit,
        onPlayWithPosition: (ContentItem, List<ContentItem>, Long?) -> Unit,
        onMovieInfo: (ContentItem, List<ContentItem>) -> Unit,
        onSeriesPlaybackStart: (ContentItem) -> Unit,
        onMoveLeft: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onRemoveEntry: (ContentItem) -> Unit,
        onClearAll: () -> Unit,
        isItemFavorite: (ContentItem) -> Boolean
) {

    val shape = RoundedCornerShape(18.dp)
    // Use 4 columns at 100% (poster sizing, since continue watching includes mixed content)
    val baseColumns = remember(settings.uiScale) {
        kotlin.math.ceil(4.0 / settings.uiScale).toInt().coerceIn(4, 8)
    }
    val columns = rememberReflowColumns(baseColumns, navLayoutExpanded)
    val posterFontScale = remember(columns) { 4f / columns.toFloat() }
    val displayEntries =
            remember(continueWatchingItems) {
                val grouped =
                        continueWatchingItems.groupBy { entry ->
                            if (entry.parentItem != null &&
                                            entry.item.contentType == ContentType.SERIES
                            ) {
                                "series:${entry.parentItem.id}"
                            } else {
                                "item:${entry.item.id}"
                            }
                        }
                grouped.values.mapNotNull { group ->
                    val latest = group.maxByOrNull { it.timestampMs } ?: return@mapNotNull null
                    val displayItem = latest.parentItem ?: latest.item
                    val resumeLabel =
                            if (latest.item.contentType == ContentType.SERIES) {
                                formatEpisodeLabel(latest.item, separator = " - ")?.let { "Resume $it" }
                            } else {
                                null
                            }
                    val displayItemWithSubtitle =
                            if (resumeLabel != null) {
                                displayItem.copy(subtitle = resumeLabel)
                            } else {
                                displayItem
                            }
                    ContinueWatchingDisplayEntry(
                            key = latest.key,
                            displayItem = displayItemWithSubtitle,
                            resumeItem = latest.item,
                            parentItem = latest.parentItem,
                            sourceItems = group.map { it.item }.distinctBy { it.id },
                            positionMs = latest.positionMs,
                            durationMs = latest.durationMs,
                            timestampMs = latest.timestampMs
                    )
                }.sortedByDescending { it.timestampMs }
            }
    val hasItems = displayEntries.isNotEmpty()
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedSeries by remember { mutableStateOf<ContentItem?>(null) }
    var pendingSeries by remember { mutableStateOf<ContentItem?>(null) }
    var pendingSeriesInfo by remember { mutableStateOf<SeriesInfo?>(null) }
    var pendingEpisodeFocus by remember { mutableStateOf(false) }
    val episodesFocusRequester = remember { FocusRequester() }
    val movieQueueItems =
            remember(displayEntries) {
                displayEntries.mapNotNull { entry ->
                    entry.displayItem.takeIf {
                        entry.resumeItem.contentType == ContentType.MOVIES
                    }
                }
            }
    val resolvedParents = remember { androidx.compose.runtime.mutableStateMapOf<String, ContentItem>() }

    LaunchedEffect(displayEntries, authConfig) {
        displayEntries
            .filter { entry ->
                entry.resumeItem.contentType == ContentType.SERIES && entry.parentItem == null &&
                    !resolvedParents.containsKey(entry.key)
            }
            .forEach { entry ->
                val rawTitle = entry.resumeItem.title
                val dashSeasonMatch = Regex("\\s-\\sS\\d", RegexOption.IGNORE_CASE).find(rawTitle)
                val compactSeasonMatch = Regex("S\\d+E\\d+", RegexOption.IGNORE_CASE).find(rawTitle)
                val seriesName =
                    when {
                        dashSeasonMatch != null && dashSeasonMatch.range.first > 0 ->
                            rawTitle.substring(0, dashSeasonMatch.range.first).trim()
                        compactSeasonMatch != null && compactSeasonMatch.range.first > 0 ->
                            rawTitle.substring(0, compactSeasonMatch.range.first).trim().trimEnd('-').trim()
                        else -> rawTitle
                    }
                if (seriesName.isBlank()) return@forEach
                val resolved = runCatching {
                    contentRepository.searchPage(
                        section = Section.SERIES,
                        query = seriesName,
                        page = 0,
                        limit = 20,
                        authConfig = authConfig
                    )
                }.getOrNull()?.items?.firstOrNull { it.title.startsWith(seriesName, ignoreCase = true) }
                    ?: runCatching {
                        contentRepository.searchPage(
                            section = Section.SERIES,
                            query = seriesName,
                            page = 0,
                            limit = 20,
                            authConfig = authConfig
                        )
                    }.getOrNull()?.items?.firstOrNull()
                if (resolved != null) {
                    resolvedParents[entry.key] = resolved
                }
            }
    }

    LaunchedEffect(pendingSeries) {
        val series = pendingSeries ?: return@LaunchedEffect
        val infoResult = runCatching { contentRepository.loadSeriesInfo(series, authConfig) }
        pendingSeriesInfo = infoResult.getOrNull()
        selectedSeries = series
        pendingSeries = null
        pendingEpisodeFocus = true
    }

    // Focus is managed by user navigation - no auto-focus on screen load

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 12.dp)
                                .clip(shape)
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(AppTheme.colors.background, AppTheme.colors.backgroundAlt)
                                        )
                                )
                                .border(1.dp, AppTheme.colors.border, shape)
                                .padding(20.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = title,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 20.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                )
                if (hasItems) {
                    FocusableButton(
                            onClick = { showClearDialog = true },
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = AppTheme.colors.surfaceAlt,
                                            contentColor = AppTheme.colors.textPrimary
                                    ),
                            modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                                text = "Clear all",
                                fontSize = 12.sp,
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (displayEntries.isEmpty()) {
                Text(
                        text = "No items in progress",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.focusRequester(contentItemFocusRequester).focusable()
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                                    .alpha(if (selectedSeries != null) 0f else 1f),
                            userScrollEnabled = selectedSeries == null
                    ) {
                        items(
                                count = displayEntries.size,
                                key = { index -> displayEntries[index].key }
                        ) { index ->
                        val entry = displayEntries[index]
                        val resolvedParent = resolvedParents[entry.key]
                        val displayItem = resolvedParent ?: entry.displayItem
                        val item = displayItem
                        val requester =
                                when {
                                    item.id == resumeFocusId -> resumeFocusRequester
                                    index == 0 -> contentItemFocusRequester
                                    else -> null
                                }
                            val isLeftEdge = index % columns == 0

                            val progressPercent =
                                    if (entry.durationMs > 0) {
                                        ((entry.positionMs * 100) / entry.durationMs).toInt()
                                    } else {
                                        0
                                    }

                            ContinueWatchingCard(
                                    entry = entry,
                                    focusRequester = requester,
                                    isLeftEdge = isLeftEdge,
                                    isFavorite = isItemFavorite(item),
                                    progressPercent = progressPercent,
                                    fontScaleFactor = posterFontScale,
                                    onActivate = {
                                        when (entry.resumeItem.contentType) {
                                            ContentType.MOVIES -> onMovieInfo(item, movieQueueItems)
                                            ContentType.SERIES -> {
                                                val seriesItem =
                                                        resolvedParent
                                                                ?: entry.parentItem
                                                                ?: item.takeIf {
                                                                    it.containerExtension.isNullOrBlank()
                                                                }
                                                if (seriesItem != null) {
                                                    pendingSeries = seriesItem
                                                } else {
                                                    onPlay(
                                                            entry.resumeItem,
                                                            entry.positionMs,
                                                            entry.parentItem
                                                    )
                                                }
                                            }
                                            else -> onPlay(entry.resumeItem, entry.positionMs, entry.parentItem)
                                        }
                                    },
                                    onFocused = { onItemFocused(item) },
                                    onMoveLeft = onMoveLeft,
                                    onLongClick = { onToggleFavorite(item) },
                                    onRemove = { entry.sourceItems.forEach(onRemoveEntry) }
                            )
                        }
                    }

                    if (selectedSeries != null) {
                        SeriesSeasonsScreen(
                                seriesItem = selectedSeries!!,
                                contentRepository = contentRepository,
                                authConfig = authConfig,
                                continueWatchingEntries = continueWatchingItems,
                                showClearContinueWatching = true,
                                contentItemFocusRequester = contentItemFocusRequester,
                                resumeFocusId = resumeFocusId,
                                resumeFocusRequester = resumeFocusRequester,
                                episodesFocusRequester = episodesFocusRequester,
                                pendingEpisodeFocus = pendingEpisodeFocus,
                                onEpisodeFocusHandled = { pendingEpisodeFocus = false },
                                onItemFocused = onItemFocused,
                                onPlay = { playItem, items ->
                                    onSeriesPlaybackStart(selectedSeries!!)
                                    onPlayWithPosition(playItem, items, null)
                                },
                                onPlayWithPosition = { playItem, items, position ->
                                    onSeriesPlaybackStart(selectedSeries!!)
                                    onPlayWithPosition(playItem, items, position)
                                },
                                onMoveLeft = onMoveLeft,
                                onBack = {
                                    selectedSeries = null
                                    pendingSeriesInfo = null
                                    pendingEpisodeFocus = false
                                    runCatching { contentItemFocusRequester.requestFocus() }
                                },
                                onToggleFavorite = onToggleFavorite,
                                onRemoveContinueWatching = onRemoveEntry,
                                isItemFavorite = isItemFavorite,
                                prefetchedInfo = pendingSeriesInfo
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AppDialog(onDismissRequest = { showClearDialog = false }) {
            val colors = AppTheme.colors
            Box(
                modifier =
                    Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Clear Continue Watching?",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "This will remove all items from your Continue Watching list.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FocusableButton(
                            onClick = {
                                onClearAll()
                                showClearDialog = false
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.textOnAccent
                                )
                        ) {
                            Text(
                                text = "Clear all",
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                        FocusableButton(
                            onClick = { showClearDialog = false },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = colors.surfaceAlt,
                                    contentColor = colors.textPrimary
                                )
                        ) {
                            Text(
                                text = "Cancel",
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesSeasonsScreen(
        seriesItem: ContentItem,
        contentRepository: ContentRepository,
        authConfig: AuthConfig,
        continueWatchingEntries: List<ContinueWatchingEntry> = emptyList(),
        showClearContinueWatching: Boolean = false,
        topInsetDp: Dp = 0.dp,
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        episodesFocusRequester: FocusRequester,
        pendingEpisodeFocus: Boolean,
        onEpisodeFocusHandled: () -> Unit,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onPlayWithPosition:
                (ContentItem, List<ContentItem>, Long?) -> Unit =
                { item, items, _ -> onPlay(item, items) },
        onMoveLeft: () -> Unit,
        onBack: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onRemoveContinueWatching: (ContentItem) -> Unit = {},
        isItemFavorite: (ContentItem) -> Boolean,
        forceDarkText: Boolean = false,
        onMoveUpFromTop: (() -> Unit)? = null,
        prefetchedInfo: SeriesInfo? = null
) {
    BackHandler(enabled = true) { onBack() }
    val colors = AppTheme.colors
    var seasonGroups by remember { mutableStateOf<List<SeasonGroup>>(emptyList()) }
    var selectedSeasonIndex by remember { mutableStateOf(0) }
    var isSeasonLoading by remember { mutableStateOf(true) }
    var seasonError by remember { mutableStateOf<String?>(null) }
    var seriesInfo by remember(seriesItem.streamId, prefetchedInfo) {
        mutableStateOf(prefetchedInfo)
    }
    var allEpisodes by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var allEpisodesError by remember { mutableStateOf<String?>(null) }
    var initialSeasonSet by remember { mutableStateOf(false) }
    var showSeasonMenu by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(SeriesDetailTab.EPISODES) }
    var episodesExpanded by remember { mutableStateOf(false) }
    var internalEpisodeFocusRequested by remember { mutableStateOf(false) }
    val closeFocusRequester = remember { FocusRequester() }
    val tabFocusRequesters = remember { listOf(FocusRequester(), FocusRequester()) }
    val playFocusRequester = remember { FocusRequester() }
    val seasonFocusRequester = remember { FocusRequester() }
    val favoriteFocusRequester = remember { FocusRequester() }
    val clearFocusRequester = remember { FocusRequester() }
    val readMoreFocusRequester = remember { FocusRequester() }
    val episodesTabRequester = contentItemFocusRequester
    val castTabRequester = tabFocusRequesters[1]
    val context = LocalContext.current

    LaunchedEffect(seriesItem.streamId) {
        withFrameNanos {}
        episodesExpanded = false
        contentItemFocusRequester.requestFocus()
    }

    LaunchedEffect(seriesItem.streamId, authConfig) {
        val result = runCatching { contentRepository.loadSeriesInfo(seriesItem, authConfig) }
        seriesInfo = result.getOrNull()
    }

    LaunchedEffect(seriesItem.streamId, authConfig) {
        isSeasonLoading = true
        seasonError = null
        selectedSeasonIndex = 0
        initialSeasonSet = false
        val result = runCatching {
            contentRepository.loadSeriesSeasons(seriesItem.streamId, authConfig)
        }
        result
            .onSuccess { summaries ->
                val grouped =
                    summaries
                        .map { summary ->
                            SeasonGroup(
                                label = summary.label,
                                displayLabel = buildSeasonLabel(summary.label),
                                seasonNumber = seasonNumberFromLabel(summary.label),
                                episodeCount = summary.episodeCount
                            )
                        }
                        .sortedWith(
                            compareBy<SeasonGroup> { it.seasonNumber }.thenBy { it.displayLabel }
                        )
                seasonGroups = grouped
                selectedSeasonIndex = 0
            }
            .onFailure { error -> seasonError = error.message ?: "Failed to load seasons" }
        isSeasonLoading = false
    }

    LaunchedEffect(seriesItem.streamId, authConfig) {
        allEpisodesError = null
        allEpisodes = emptyList()
        val result = runCatching {
            contentRepository.loadSeriesEpisodes(seriesItem.streamId, authConfig)
        }
        result
            .onSuccess { episodes -> allEpisodes = episodes }
            .onFailure { error -> allEpisodesError = error.message ?: "Failed to load episodes" }
    }

    val resumeEntry =
        remember(allEpisodes, continueWatchingEntries) {
            if (allEpisodes.isEmpty()) {
                null
            } else {
                val episodeIds = allEpisodes.map { it.streamId }.toHashSet()
                continueWatchingEntries.firstOrNull { entry ->
                    entry.item.contentType == ContentType.SERIES &&
                        episodeIds.contains(entry.item.streamId)
                }
            }
        }
    val continueWatchingEntriesForSeries =
        remember(allEpisodes, continueWatchingEntries) {
            if (allEpisodes.isEmpty()) {
                emptyList()
            } else {
                val episodeIds = allEpisodes.map { it.streamId }.toHashSet()
                continueWatchingEntries.filter { entry ->
                    entry.item.contentType == ContentType.SERIES &&
                        episodeIds.contains(entry.item.streamId)
                }
            }
        }
    val resumePositionsById =
        remember(continueWatchingEntries) {
            continueWatchingEntries
                .asSequence()
                .filter { it.item.contentType == ContentType.SERIES }
                .associate { it.item.id to it.positionMs }
        }
    val resumeSeasonLabel = resumeEntry?.item?.seasonLabel
        ?: extractSeasonLabel(resumeEntry?.item?.subtitle)
    val resumePositionMs = resumeEntry?.positionMs?.takeIf { it > 0 }
    val resumeSeasonNumber =
        resumeSeasonLabel?.let { seasonNumberFromLabel(it) }?.takeIf { it != Int.MAX_VALUE }

    LaunchedEffect(seasonGroups, resumeSeasonNumber, initialSeasonSet) {
        if (!initialSeasonSet && seasonGroups.isNotEmpty()) {
            val targetIndex =
                if (resumeSeasonNumber != null) {
                    seasonGroups.indexOfFirst { it.seasonNumber == resumeSeasonNumber }
                } else {
                    -1
                }
            selectedSeasonIndex = if (targetIndex >= 0) targetIndex else 0
            initialSeasonSet = true
        }
    }

    val selectedSeason = seasonGroups.getOrNull(selectedSeasonIndex)
    val selectedSeasonLabel = selectedSeason?.label
    val seasonEpisodes =
        remember(allEpisodes, selectedSeasonLabel) {
            if (selectedSeasonLabel.isNullOrBlank()) {
                emptyList()
            } else {
                val selectedSeasonNumber = seasonNumberFromLabel(selectedSeasonLabel)
                allEpisodes.filter { episode ->
                    val episodeSeason =
                        episode.seasonLabel
                            ?: extractSeasonLabel(episode.subtitle)
                    val episodeSeasonNumber =
                        episodeSeason?.let(::seasonNumberFromLabel) ?: Int.MAX_VALUE
                    if (selectedSeasonNumber != Int.MAX_VALUE && episodeSeasonNumber != Int.MAX_VALUE) {
                        episodeSeasonNumber == selectedSeasonNumber
                    } else {
                        episodeSeason == selectedSeasonLabel
                    }
                }
            }
        }
    val pagerFlow =
        remember(seriesItem.streamId, selectedSeasonLabel, authConfig) {
            val label = selectedSeasonLabel
            if (label.isNullOrBlank()) {
                flowOf(androidx.paging.PagingData.empty())
            } else {
                contentRepository.seriesSeasonPager(seriesItem.streamId, label, authConfig).flow
            }
        }
    val lazyItems = pagerFlow.collectAsLazyPagingItems()
    LaunchedEffect(seriesItem.streamId, selectedSeasonLabel, authConfig) {
        val label = selectedSeasonLabel
        if (!label.isNullOrBlank()) {
            contentRepository.prefetchSeriesSeasonFull(seriesItem.streamId, label, authConfig)
        }
    }

    val firstEpisode =
        remember(allEpisodes) {
            allEpisodes.minWithOrNull(
                compareBy<ContentItem>(
                    { it.seasonLabel?.let(::seasonNumberFromLabel) ?: Int.MAX_VALUE },
                    { it.episodeNumber?.toIntOrNull() ?: Int.MAX_VALUE },
                    { it.title }
                )
            )
        }
    val fallbackEpisode =
        lazyItems.itemSnapshotList.items.firstOrNull()
    val playTarget = resumeEntry?.item ?: firstEpisode ?: fallbackEpisode
    val playLabelSuffix =
        resumeEntry?.item?.let { formatEpisodeLabel(it, separator = " - ") }
            ?: playTarget?.let { formatEpisodeLabel(it, separator = ":") }
    val playLabel =
        if (resumeEntry != null && playLabelSuffix != null) {
            "Resume $playLabelSuffix"
        } else {
            "Play - ${playLabelSuffix ?: "S1:E1"}"
        }

    val episodesLabel =
        if (seasonEpisodes.isNotEmpty()) {
            seasonEpisodes.size
        } else {
            selectedSeason?.episodeCount ?: lazyItems.itemCount
        }
    val releaseLabel = formatReleaseYear(seriesInfo?.releaseDate, seriesInfo?.year)
    val ratingValue = ratingToStars(seriesInfo?.rating)
    val description =
        seriesInfo?.description?.takeIf { it.isNotBlank() } ?: "No description available."
    var plotOverflow by remember { mutableStateOf(false) }
    var showPlotDialog by remember { mutableStateOf(false) }
    LaunchedEffect(description) { plotOverflow = false }
    val showReadMore by remember(description, plotOverflow) {
        mutableStateOf(
            description != "No description available." &&
                (plotOverflow || description.length > 140)
        )
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val availableHeight = (screenHeight - topInsetDp).coerceAtLeast(320.dp)
    val collapsedEpisodesHeight = 150.dp
    val reservedBelowHeader = collapsedEpisodesHeight + 96.dp
    val headerMaxByRatio = availableHeight * 0.34f
    val headerMaxByReserve = availableHeight - reservedBelowHeader
    val headerExpandedHeight =
        minOf(headerMaxByRatio, headerMaxByReserve).coerceIn(170.dp, 250.dp)
    val headerCollapsedHeight = 0.dp
    val headerHeight by animateDpAsState(
        targetValue = if (episodesExpanded) headerCollapsedHeight else headerExpandedHeight,
        animationSpec = tween(durationMillis = 180),
        label = "seriesHeaderHeight"
    )
    val ratingAreaHeight = if (headerHeight > 0.dp) 24.dp else 0.dp
    val posterHeight = (headerHeight - ratingAreaHeight).coerceAtLeast(0.dp)
    val posterWidth = posterHeight * 0.68f
    val containerPadding = 20.dp
    val headerSpacer = 10.dp

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(colors.surface)
                .padding(containerPadding)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = seriesItem.title,
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TopBarButton(
                label = "CLOSE",
                onActivate = onBack,
                modifier =
                    Modifier.focusRequester(closeFocusRequester)
                        .onFocusChanged { if (it.isFocused) episodesExpanded = false },
                onMoveLeft = onMoveLeft,
                onMoveDown = {
                    if (!episodesExpanded && showReadMore) {
                        readMoreFocusRequester.requestFocus()
                    } else if (!episodesExpanded) {
                        favoriteFocusRequester.requestFocus()
                    } else {
                        episodesTabRequester.requestFocus()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(headerSpacer))

        if (headerHeight > 0.dp) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(headerHeight)
                        .clipToBounds(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            val context = LocalContext.current
            val imageRequest =
                remember(seriesItem.imageUrl) {
                    if (seriesItem.imageUrl.isNullOrBlank()) {
                        null
                    } else {
                        ImageRequest.Builder(context)
                            .data(seriesItem.imageUrl)
                            .size(600)
                            .build()
                    }
                }
            Column(
                modifier = Modifier.width(posterWidth),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (posterHeight > 0.dp) {
                    if (imageRequest != null) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.Low,
                            modifier =
                                Modifier.width(posterWidth)
                                    .height(posterHeight)
                                    .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier.width(posterWidth)
                                    .height(posterHeight)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceAlt)
                        )
                    }
                }
                if (ratingAreaHeight > 0.dp) {
                    Box(
                        modifier =
                            Modifier.width(posterWidth)
                                .height(ratingAreaHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (ratingValue != null) {
                            RatingStars(
                                rating = ratingValue,
                                starSize = 14.dp,
                                spacing = 2.dp
                            )
                        } else {
                            Text(
                                text = "N/A",
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DETAIL_ROW_SPACING)
                ) {
                MovieInfoRow(label = "Directed By:", value = seriesInfo?.director)
                MovieInfoRow(label = "Release Date:", value = releaseLabel)
                MovieInfoRow(label = "Genre:", value = seriesInfo?.genre)
                MovieInfoRow(label = "Cast:", value = seriesInfo?.cast)

                Text(
                    text = "Plot:",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = AppTheme.fontFamily
                )
                if (showReadMore) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val readMoreInteraction = remember { MutableInteractionSource() }
                        val isReadMoreFocused by readMoreInteraction.collectIsFocusedAsState()
                        Text(
                            text = description,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = AppTheme.fontFamily,
                            lineHeight = 18.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { plotOverflow = it.hasVisualOverflow },
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier =
                                Modifier.focusRequester(readMoreFocusRequester)
                                    .focusable(interactionSource = readMoreInteraction)
                                    .onKeyEvent {
                                        if (it.type != KeyEventType.KeyDown) {
                                            false
                                        } else if (it.key == Key.Enter ||
                                                it.key == Key.NumPadEnter ||
                                                it.key == Key.DirectionCenter
                                        ) {
                                            showPlotDialog = true
                                            true
                                        } else if (it.key == Key.DirectionUp) {
                                            closeFocusRequester.requestFocus()
                                            true
                                        } else if (it.key == Key.DirectionDown) {
                                            favoriteFocusRequester.requestFocus()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(
                                        if (isReadMoreFocused) {
                                            Modifier.border(1.dp, colors.focus, RoundedCornerShape(6.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clickable(
                                        interactionSource = readMoreInteraction,
                                        indication = null
                                    ) { showPlotDialog = true }
                        ) {
                            Text(
                                text = "Read more",
                                color = colors.accent,
                                fontSize = 12.sp,
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Text(
                        text = description,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily,
                        lineHeight = 18.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { plotOverflow = it.hasVisualOverflow }
                    )
                }
                }
            }
        }

        if (showPlotDialog) {
            PlotDialog(
                title = seriesItem.title,
                plot = description,
                onDismiss = { showPlotDialog = false }
            )
        }

        Spacer(modifier = Modifier.height(if (episodesExpanded) 6.dp else 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTypeTab(
                    label = "Episodes (${episodesLabel.coerceAtLeast(0)})",
                    selected = activeTab == SeriesDetailTab.EPISODES,
                    focusRequester = episodesTabRequester,
                    onFocused = { episodesExpanded = false },
                    onActivate = { activeTab = SeriesDetailTab.EPISODES },
                    onMoveDown = {
                        episodesExpanded = true
                        internalEpisodeFocusRequested = true
                    },
                    onMoveLeft = {
                        episodesExpanded = false
                        onMoveLeft()
                    },
                    onMoveRight = { castTabRequester.requestFocus() },
                    onMoveUp = {
                        episodesExpanded = false
                        if (!episodesExpanded && showReadMore) {
                            readMoreFocusRequester.requestFocus()
                        } else {
                            closeFocusRequester.requestFocus()
                        }
                    }
                )
                CategoryTypeTab(
                    label = "Cast",
                    selected = activeTab == SeriesDetailTab.CAST,
                    focusRequester = castTabRequester,
                    onFocused = { episodesExpanded = false },
                    onActivate = { activeTab = SeriesDetailTab.CAST },
                    onMoveLeft = { episodesTabRequester.requestFocus() },
                    onMoveRight = { playFocusRequester.requestFocus() },
                    onMoveUp = {
                        episodesExpanded = false
                        if (!episodesExpanded && showReadMore) {
                            readMoreFocusRequester.requestFocus()
                        } else {
                            closeFocusRequester.requestFocus()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusableButton(
                    onClick = {
                        val target = playTarget
                        if (target != null) {
                            val seasonLabel =
                                resumeSeasonLabel ?: target.seasonLabel ?: selectedSeasonLabel
                            val cachedSeason =
                                if (!seasonLabel.isNullOrBlank()) {
                                    contentRepository.peekSeriesSeasonFullCache(
                                        seriesItem.streamId,
                                        seasonLabel,
                                        authConfig
                                    )
                                } else {
                                    null
                                }
                            val fallbackEpisodes = lazyItems.itemSnapshotList.items
                            val queueItems =
                                when {
                                    seasonEpisodes.isNotEmpty() -> seasonEpisodes
                                    cachedSeason != null -> cachedSeason
                                    else -> fallbackEpisodes
                                }
                            val resumePosition =
                                if (resumeEntry?.item?.id == target.id) {
                                    resumePositionMs
                                } else {
                                    null
                                }
                            onPlayWithPosition(target, queueItems, resumePosition)
                        }
                    },
                    modifier =
                        Modifier.width(200.dp)
                            .focusRequester(playFocusRequester)
                            .onKeyEvent {
                                if (it.type != KeyEventType.KeyDown) {
                                    false
                                } else if (it.key == Key.DirectionUp) {
                                    if (!episodesExpanded && showReadMore) {
                                        readMoreFocusRequester.requestFocus()
                                    } else {
                                        closeFocusRequester.requestFocus()
                                    }
                                    true
                                } else if (it.key == Key.DirectionRight) {
                                    seasonFocusRequester.requestFocus()
                                    true
                                } else if (it.key == Key.DirectionLeft) {
                                    episodesExpanded = false
                                    castTabRequester.requestFocus()
                                    true
                                } else if (it.key == Key.DirectionDown) {
                                    activeTab = SeriesDetailTab.EPISODES
                                    episodesExpanded = true
                                    internalEpisodeFocusRequested = true
                                    if (seasonEpisodes.isNotEmpty() ||
                                        lazyItems.itemSnapshotList.items.isNotEmpty()
                                    ) {
                                        episodesFocusRequester.requestFocus()
                                    } else {
                                        episodesTabRequester.requestFocus()
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            .onFocusChanged { if (it.isFocused) episodesExpanded = false },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.textOnAccent
                        )
                ) {
                    Text(
                        text = playLabel,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box {
                    val seasonButtonLabel =
                        selectedSeason?.label?.takeIf { it.isNotBlank() }?.let { "Season - $it" }
                            ?: "Select season"
                    FocusableButton(
                        onClick = { showSeasonMenu = true },
                        enabled = seasonGroups.isNotEmpty(),
                        modifier =
                            Modifier.width(180.dp)
                                .focusRequester(seasonFocusRequester)
                                .onKeyEvent {
                                if (it.type != KeyEventType.KeyDown) {
                                    false
                                } else if (it.key == Key.DirectionLeft) {
                                    playFocusRequester.requestFocus()
                                    true
                                } else if (it.key == Key.DirectionRight) {
                                    favoriteFocusRequester.requestFocus()
                                    true
                                } else if (it.key == Key.DirectionUp) {
                                    if (!episodesExpanded && showReadMore) {
                                        readMoreFocusRequester.requestFocus()
                                    } else {
                                        closeFocusRequester.requestFocus()
                                    }
                                    true
                                } else if (it.key == Key.DirectionDown) {
                                    activeTab = SeriesDetailTab.EPISODES
                                    episodesExpanded = true
                                    internalEpisodeFocusRequested = true
                                    if (seasonEpisodes.isNotEmpty() ||
                                        lazyItems.itemSnapshotList.items.isNotEmpty()
                                    ) {
                                        episodesFocusRequester.requestFocus()
                                    } else {
                                        episodesTabRequester.requestFocus()
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                                .onFocusChanged { if (it.isFocused) episodesExpanded = false },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceAlt,
                                contentColor = colors.textPrimary
                            )
                    ) {
                        Text(
                            text = "$seasonButtonLabel \u25BE",
                            fontSize = 12.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (showSeasonMenu) {
                    SeasonSelectionDialog(
                        seasons = seasonGroups,
                        selectedIndex = selectedSeasonIndex,
                        onSelect = { index ->
                            selectedSeasonIndex = index
                            showSeasonMenu = false
                            seasonFocusRequester.requestFocus()
                        },
                        onDismiss = {
                            showSeasonMenu = false
                            seasonFocusRequester.requestFocus()
                        }
                    )
                }
                FocusableButton(
                    onClick = { onToggleFavorite(seriesItem) },
                    modifier =
                        Modifier.size(48.dp)
                            .focusRequester(favoriteFocusRequester)
                            .onKeyEvent {
                                if (it.type != KeyEventType.KeyDown) {
                                    false
                                } else if (it.key == Key.DirectionLeft) {
                                    seasonFocusRequester.requestFocus()
                                    true
                                } else if (it.key == Key.DirectionRight) {
                                    if (continueWatchingEntriesForSeries.isNotEmpty()) {
                                        clearFocusRequester.requestFocus()
                                    } else {
                                        closeFocusRequester.requestFocus()
                                    }
                                    true
                                } else if (it.key == Key.DirectionUp) {
                                    if (!episodesExpanded && showReadMore) {
                                        readMoreFocusRequester.requestFocus()
                                    } else {
                                        closeFocusRequester.requestFocus()
                                    }
                                    true
                                } else if (it.key == Key.DirectionDown) {
                                    activeTab = SeriesDetailTab.EPISODES
                                    episodesExpanded = true
                                    internalEpisodeFocusRequested = true
                                    if (seasonEpisodes.isNotEmpty() ||
                                        lazyItems.itemSnapshotList.items.isNotEmpty()
                                    ) {
                                        episodesFocusRequester.requestFocus()
                                    } else {
                                        episodesTabRequester.requestFocus()
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            .onFocusChanged { if (it.isFocused) episodesExpanded = false },
                    contentPadding = PaddingValues(0.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceAlt,
                            contentColor = colors.textPrimary
                        )
                ) {
                    Icon(
                        imageVector =
                            if (isItemFavorite(seriesItem)) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                        contentDescription = "Favorite",
                        tint = if (isItemFavorite(seriesItem)) Color(0xFFEF5350) else colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (showClearContinueWatching && continueWatchingEntriesForSeries.isNotEmpty()) {
                    FocusableButton(
                        onClick = {
                            val itemsToRemove =
                                continueWatchingEntriesForSeries
                                    .map { it.item }
                                    .distinctBy { it.id }
                            itemsToRemove.forEach { onRemoveContinueWatching(it) }
                            if (itemsToRemove.isNotEmpty()) {
                                Toast.makeText(
                                        context,
                                        "Removed from Continue Watching",
                                        Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier =
                            Modifier.height(48.dp)
                                .focusRequester(clearFocusRequester)
                                .onKeyEvent {
                                    if (it.type != KeyEventType.KeyDown) {
                                        false
                                    } else if (it.key == Key.DirectionLeft) {
                                        favoriteFocusRequester.requestFocus()
                                        true
                                    } else if (it.key == Key.DirectionRight) {
                                        closeFocusRequester.requestFocus()
                                        true
                                    } else if (it.key == Key.DirectionUp) {
                                        if (!episodesExpanded && showReadMore) {
                                            readMoreFocusRequester.requestFocus()
                                        } else {
                                            closeFocusRequester.requestFocus()
                                        }
                                        true
                                    } else if (it.key == Key.DirectionDown) {
                                        activeTab = SeriesDetailTab.EPISODES
                                        episodesExpanded = true
                                        internalEpisodeFocusRequested = true
                                        if (seasonEpisodes.isNotEmpty() ||
                                            lazyItems.itemSnapshotList.items.isNotEmpty()
                                        ) {
                                            episodesFocusRequester.requestFocus()
                                        } else {
                                            episodesTabRequester.requestFocus()
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .onFocusChanged { if (it.isFocused) episodesExpanded = false },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceAlt,
                                contentColor = colors.textPrimary
                            )
                    ) {
                        Text(
                            text = "Clear from list",
                            fontSize = 11.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (activeTab) {
            SeriesDetailTab.CAST -> {
                val castText = seriesInfo?.cast?.takeIf { it.isNotBlank() }
                if (castText.isNullOrBlank()) {
                    Text(
                        text = "No cast information available.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                } else {
                    val castNames =
                        castText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        castNames.forEach { name ->
                            Text(
                                text = name,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontFamily = AppTheme.fontFamily
                            )
                        }
                    }
                }
            }
            SeriesDetailTab.EPISODES -> {
                val fallbackEpisodes = lazyItems.itemSnapshotList.items
                val displayEpisodes =
                    if (seasonEpisodes.isNotEmpty()) seasonEpisodes else fallbackEpisodes
                val shouldRequestEpisodeFocus = pendingEpisodeFocus || internalEpisodeFocusRequested
                if (isSeasonLoading) {
                    Text(
                        text = "Loading seasons...",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                } else if (seasonError != null) {
                    Text(
                        text = seasonError ?: "Failed to load seasons",
                        color = colors.error,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                } else if (lazyItems.loadState.refresh is LoadState.Loading &&
                    lazyItems.itemCount == 0
                ) {
                    Text(
                        text = "Loading episodes...",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                } else if (lazyItems.loadState.refresh is LoadState.Error) {
                    Text(
                        text = "Episodes failed to load",
                        color = colors.error,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                } else if (displayEpisodes.isEmpty()) {
                    val emptyText =
                        if (allEpisodesError != null) {
                            allEpisodesError ?: "No episodes yet"
                        } else {
                            "No episodes yet"
                        }
                    Text(
                        text = emptyText,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                } else {
                    val listModifier =
                        if (episodesExpanded) {
                            Modifier.fillMaxWidth().weight(1f)
                        } else {
                            Modifier.fillMaxWidth().height(collapsedEpisodesHeight)
                        }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = listModifier
                    ) {
                        items(
                            count = displayEpisodes.size,
                            key = { index -> displayEpisodes[index].id }
                        ) { index ->
                            val item = displayEpisodes[index]
                            val requester =
                                when {
                                    item.id == resumeFocusId -> resumeFocusRequester
                                    index == 0 -> episodesFocusRequester
                                    else -> null
                                }
                            if (index == 0 && shouldRequestEpisodeFocus && requester != null) {
                                LaunchedEffect(shouldRequestEpisodeFocus, requester) {
                                    withFrameNanos {}
                                    requester.requestFocus()
                                    if (pendingEpisodeFocus) {
                                        onEpisodeFocusHandled()
                                    }
                                    internalEpisodeFocusRequested = false
                                }
                            }
                            SeriesEpisodeRow(
                                item = item,
                                focusRequester = requester,
                                forceDarkText = forceDarkText,
                                onActivate = {
                                    val label = selectedSeasonLabel
                                    val cachedSeason =
                                        if (!label.isNullOrBlank()) {
                                            contentRepository.peekSeriesSeasonFullCache(
                                                seriesItem.streamId,
                                                label,
                                                authConfig
                                            )
                                        } else {
                                            null
                                        }
                                    val queueItems =
                                        when {
                                            seasonEpisodes.isNotEmpty() -> seasonEpisodes
                                            cachedSeason != null -> cachedSeason
                                            else -> fallbackEpisodes
                                        }
                                    val resumePosition =
                                        resumePositionsById[item.id]?.takeIf { it > 0 }
                                    onPlayWithPosition(item, queueItems, resumePosition)
                                },
                                onFocused = {
                                    episodesExpanded = true
                                    onItemFocused(item)
                                },
                                onMoveLeft = onMoveLeft,
                                onMoveUp =
                                    if (index == 0) {
                                        {
                                            episodesExpanded = false
                                            contentItemFocusRequester.requestFocus()
                                        }
                                    } else {
                                        null
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class SeriesDetailTab {
    EPISODES,
    CAST
}

@Composable
private fun SeriesEpisodeRow(
        item: ContentItem,
        focusRequester: FocusRequester?,
        forceDarkText: Boolean,
        onActivate: () -> Unit,
        onFocused: (() -> Unit)? = null,
        onFocusLost: (() -> Unit)? = null,
        onMoveLeft: (() -> Unit)? = null,
        onMoveUp: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val colors = AppTheme.colors
    val borderColor = if (isFocused) colors.focus else colors.border
    val backgroundColor = if (isFocused) colors.surfaceAlt else colors.surfaceAlt
    val titleColor = if (forceDarkText) colors.textPrimary else colors.textPrimary
    val subtitleColor = if (forceDarkText) colors.textSecondary else colors.textSecondary
    val ratingValue = ratingToStars(item.rating)
    val durationLabel = formatDuration(item.duration)
    val description =
            item.description?.takeIf { it.isNotBlank() } ?: "No description available."
    val context = LocalContext.current
    val imageRequest =
            remember(item.imageUrl) {
                if (item.imageUrl.isNullOrBlank()) {
                    null
                } else {
                    ImageRequest.Builder(context).data(item.imageUrl).size(400).build()
                }
            }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused?.invoke()
        } else {
            onFocusLost?.invoke()
        }
    }

    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .then(
                                    if (focusRequester != null) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    }
                            )
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                if (it.type != KeyEventType.KeyDown) {
                                    false
                                } else if (it.key == Key.DirectionLeft && onMoveLeft != null) {
                                    onMoveLeft()
                                    true
                                } else if (it.key == Key.DirectionUp && onMoveUp != null) {
                                    onMoveUp()
                                    true
                                } else if (it.key == Key.Enter ||
                                        it.key == Key.NumPadEnter ||
                                        it.key == Key.DirectionCenter
                                ) {
                                    onActivate()
                                    true
                                } else {
                                    false
                                }
                            }
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onActivate
                            )
                            .clip(shape)
                            .background(backgroundColor)
                            .border(1.dp, borderColor, shape)
                            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val thumbModifier =
                    Modifier.width(150.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(10.dp))
            if (imageRequest != null) {
                AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Low,
                        modifier = thumbModifier
                )
            } else {
                Box(
                        modifier = thumbModifier.background(colors.surfaceAlt)
                )
            }
            Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                        text = item.title,
                        color = titleColor,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                )
                Text(
                        text = item.subtitle,
                        color = subtitleColor,
                        fontSize = 12.sp,
                        fontFamily = AppTheme.fontFamily
                )
                Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ratingValue != null) {
                        RatingStars(
                                rating = ratingValue,
                                starSize = 14.dp,
                                spacing = 2.dp
                        )
                    }
                    if (!durationLabel.isNullOrBlank()) {
                        EpisodeMetaChip(label = durationLabel)
                    }
                }
                Text(
                        text = description,
                        color = subtitleColor,
                        fontSize = 12.sp,
                        fontFamily = AppTheme.fontFamily,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EpisodeMetaChip(label: String) {
    val colors = AppTheme.colors
    Box(
            modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceAlt)
                            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SeasonSelectionDialog(
    seasons: List<SeasonGroup>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val coroutineScope = rememberCoroutineScope()
    val itemFocusRequesters =
        remember(seasons.size) { List(seasons.size) { FocusRequester() } }
    val initialIndex = selectedIndex.coerceIn(0, (seasons.size - 1).coerceAtLeast(0))

    LaunchedEffect(seasons.size, initialIndex) {
        if (seasons.isNotEmpty()) {
            itemFocusRequesters.getOrNull(initialIndex)?.requestFocus()
                ?: itemFocusRequesters.firstOrNull()?.requestFocus()
        }
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.38f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Season",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )
                if (seasons.isEmpty()) {
                    Text(
                        text = "No seasons available",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(seasons) { index, season ->
                            SeasonOption(
                                label = season.displayLabel,
                                isSelected = index == selectedIndex,
                                focusRequester = itemFocusRequesters[index],
                                onSelect = {
                                    onSelect(index)
                                    coroutineScope.launch {
                                        delay(80)
                                        onDismiss()
                                    }
                                },
                                onNavigateUp = {
                                    if (index > 0) {
                                        itemFocusRequesters[index - 1].requestFocus()
                                    }
                                },
                                onNavigateDown = {
                                    if (index < seasons.lastIndex) {
                                        itemFocusRequesters[index + 1].requestFocus()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonOption(
    label: String,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onSelect: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = AppTheme.colors

    val backgroundColor = when {
        isFocused -> colors.accent
        isSelected -> colors.accentMutedAlt
        else -> colors.surface
    }
    val borderColor = when {
        isFocused -> colors.focus
        isSelected -> colors.accent
        else -> colors.border
    }
    val textColor = if (isFocused) colors.textOnAccent else colors.textPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    false
                } else when (it.key) {
                    Key.DirectionUp -> {
                        onNavigateUp()
                        true
                    }
                    Key.DirectionDown -> {
                        onNavigateDown()
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        onSelect()
                        true
                    }
                    else -> false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            )
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 15.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PlotDialog(
    title: String,
    plot: String,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val scrollState = rememberScrollState()
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { closeFocusRequester.requestFocus() }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Plot",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 360.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .verticalScroll(scrollState)
                            .padding(14.dp)
                ) {
                    Text(
                        text = plot,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        lineHeight = 20.sp
                    )
                }
                FocusableButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(closeFocusRequester),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.textOnAccent
                        )
                ) {
                    Text(
                        text = "Close",
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdatePromptDialog(
    release: UpdateRelease,
    isDownloading: Boolean,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val updateFocusRequester = remember { FocusRequester() }
    val laterFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isDownloading) {
        if (!isDownloading) {
            updateFocusRequester.requestFocus()
        }
    }
    AppDialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .widthIn(min = 440.dp, max = 820.dp)
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.borderStrong, shape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Update available",
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Version ${release.versionName} is ready to install.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = AppTheme.fontFamily
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isDownloading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.backgroundAlt)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Downloading update...",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                    LinearProgressIndicator(
                        progress = { 0.35f },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                        trackColor = colors.surfaceAlt
                    )
                }
            } else {
                Text(
                    text = "Install now or choose Later.",
                    color = colors.textTertiary,
                    fontSize = 13.sp,
                    fontFamily = AppTheme.fontFamily
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FocusableButton(
                    onClick = onUpdate,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .focusRequester(updateFocusRequester),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.textOnAccent,
                        disabledContainerColor = colors.surfaceAlt,
                        disabledContentColor = colors.textTertiary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    focusBorderWidth = 1.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isDownloading) "Updating..." else "Update now",
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FocusableButton(
                    onClick = onLater,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .focusRequester(laterFocusRequester),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceAlt,
                        contentColor = colors.textPrimary,
                        disabledContainerColor = colors.surfaceAlt,
                        disabledContentColor = colors.textTertiary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    focusBorderWidth = 1.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Later",
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun extractSeasonLabel(subtitle: String?): String? {
    if (subtitle.isNullOrBlank()) return null
    val match = Regex("S(\\d+)").find(subtitle)
    return match?.groupValues?.getOrNull(1)
}

private fun extractEpisodeLabel(subtitle: String?): String? {
    if (subtitle.isNullOrBlank()) return null
    val match = Regex("E(\\d+)").find(subtitle)
    return match?.groupValues?.getOrNull(1)
}

private fun formatEpisodeLabel(item: ContentItem, separator: String): String? {
    val season = item.seasonLabel ?: extractSeasonLabel(item.subtitle)
    val episode = item.episodeNumber ?: extractEpisodeLabel(item.subtitle)
    return formatSeasonEpisodeLabel(season, episode, separator)
}

private fun formatSeasonEpisodeLabel(
        season: String?,
        episode: String?,
        separator: String
): String? {
    val seasonText = season?.trim()?.takeIf { it.isNotEmpty() }
    val episodeText = episode?.trim()?.takeIf { it.isNotEmpty() }
    if (seasonText == null || episodeText == null) return null
    return "S${seasonText}${separator}E${episodeText}"
}

private data class SeasonGroup(
        val label: String,
        val displayLabel: String,
        val seasonNumber: Int,
        val episodeCount: Int
)

private fun buildSeasonLabel(rawLabel: String): String {
    val label = rawLabel.ifBlank { "1" }
    return "Season $label"
}

private fun seasonNumberFromLabel(label: String): Int {
    val digits = label.filter { it.isDigit() }
    return digits.toIntOrNull() ?: Int.MAX_VALUE
}

@Composable
fun SeriesEpisodesScreen(
        seriesItem: ContentItem,
        contentRepository: ContentRepository,
        authConfig: AuthConfig,
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onMoveLeft: () -> Unit,
        onBack: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        isItemFavorite: (ContentItem) -> Boolean
) {
    BackHandler(enabled = true) { onBack() }
    val pagerFlow =
            remember(seriesItem.streamId, authConfig) {
                contentRepository.seriesPager(seriesItem.streamId, authConfig).flow
            }
    val lazyItems = pagerFlow.collectAsLazyPagingItems()
    val columns = 1
    val backFocusRequester = remember { FocusRequester() }

    // Focus is managed by user navigation - no auto-focus on content load

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TopBarButton(
                    label = "BACK",
                    onActivate = onBack,
                    modifier = Modifier.focusRequester(backFocusRequester),
                    onMoveLeft = onMoveLeft
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                    text = seriesItem.title,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (lazyItems.loadState.refresh is LoadState.Loading && lazyItems.itemCount == 0) {
            Text(
                    text = "Loading episodes...",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = AppTheme.fontFamily,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.focusRequester(contentItemFocusRequester).focusable()
            )
        } else if (lazyItems.loadState.refresh is LoadState.Error) {
            Text(
                    text = "Episodes failed to load",
                    color = AppTheme.colors.error,
                    fontSize = 14.sp,
                    fontFamily = AppTheme.fontFamily,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.focusRequester(contentItemFocusRequester).focusable()
            )
        } else if (lazyItems.itemCount == 0) {
            Text(
                    text = "No episodes yet",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = AppTheme.fontFamily,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.focusRequester(contentItemFocusRequester).focusable()
            )
        } else {
            LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(
                        count = lazyItems.itemCount,
                        key = { index -> lazyItems[index]?.id ?: "episode-$index" }
                ) { index ->
                    val item = lazyItems[index]
                    val requester =
                            when {
                                item?.id != null && item.id == resumeFocusId -> resumeFocusRequester
                                index == 0 -> contentItemFocusRequester
                                else -> null
                            }
                    val isLeftEdge = index % columns == 0
                    val isTopRow = index < columns
                    ContentCard(
                            item = item,
                            focusRequester = requester,
                            isLeftEdge = isLeftEdge,
                            isFavorite = item != null && isItemFavorite(item),
                            onActivate =
                                    if (item != null) {
                                        { onPlay(item, lazyItems.itemSnapshotList.items) }
                                    } else {
                                        null
                                    },
                            onFocused = onItemFocused,
                            onMoveLeft = onMoveLeft,
                            onMoveUp =
                                    if (isTopRow) {
                                        { backFocusRequester.requestFocus() }
                                    } else {
                                        null
                                    },
                            onLongClick =
                                    if (item != null) {
                                        { onToggleFavorite(item) }
                                    } else {
                                        null
                                    },
                            titleFontSize = 13.sp,
                            subtitleFontSize = 10.sp
                    )
                }
            }
        }
    }
}

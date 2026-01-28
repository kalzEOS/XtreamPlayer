package com.example.xtreamplayer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
import com.example.xtreamplayer.content.SearchNormalizer
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.player.Media3PlaybackEngine
import com.example.xtreamplayer.player.XtreamPlayerView
import com.example.xtreamplayer.settings.PlaybackSettingsController
import com.example.xtreamplayer.settings.SettingsState
import com.example.xtreamplayer.settings.SettingsViewModel
import com.example.xtreamplayer.ui.ApiKeyInputDialog
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
import com.example.xtreamplayer.ui.theme.AppTheme
import com.example.xtreamplayer.ui.theme.AppColors
import com.example.xtreamplayer.ui.theme.XtreamPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var playbackSettingsController: PlaybackSettingsController

    @Inject lateinit var playbackEngine: Media3PlaybackEngine

    @Inject lateinit var contentRepository: ContentRepository

    @Inject lateinit var favoritesRepository: FavoritesRepository

    @Inject lateinit var historyRepository: HistoryRepository

    @Inject lateinit var continueWatchingRepository: ContinueWatchingRepository

    @Inject lateinit var subtitleRepository: SubtitleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RootScreen(
                    playbackSettingsController = playbackSettingsController,
                    playbackEngine = playbackEngine,
                    contentRepository = contentRepository,
                    favoritesRepository = favoritesRepository,
                    historyRepository = historyRepository,
                    continueWatchingRepository = continueWatchingRepository,
                    subtitleRepository = subtitleRepository
            )
        }
    }

    override fun onDestroy() {
        // Release player only when activity is destroyed, not on recomposition
        playbackEngine.release()
        super.onDestroy()
    }
}

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
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = hiltViewModel()
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

    var selectedSection by remember { mutableStateOf(Section.ALL) }
    var navExpanded by remember { mutableStateOf(true) }
    var showManageLists by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showUiScaleDialog by remember { mutableStateOf(false) }
    var showFontScaleDialog by remember { mutableStateOf(false) }
    var showNextEpisodeThresholdDialog by remember { mutableStateOf(false) }
    var showLocalFilesGuest by remember { mutableStateOf(false) }
    var activePlaybackQueue by remember { mutableStateOf<PlaybackQueue?>(null) }
    var activePlaybackTitle by remember { mutableStateOf<String?>(null) }
    var activePlaybackItem by remember { mutableStateOf<ContentItem?>(null) }
    var activePlaybackItems by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var playbackFallbackAttempts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var resumePositionMs by remember { mutableStateOf<Long?>(null) }
    var resumeFocusId by remember { mutableStateOf<String?>(null) }
    val resumeFocusRequester = remember { FocusRequester() }

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

    var focusToContentTrigger by remember { mutableStateOf(0) }
    var moveFocusToNav by remember { mutableStateOf(false) }

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

    val storagePermissionLauncher =
            rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted) {
                    val scanned = scanMediaStoreMedia(context)
                    scanned.forEach { item ->
                        if (localFiles.none { it.uri == item.uri }) {
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
                        val videoCount = scanned.count { it.mediaType == LocalMediaType.VIDEO }
                        val audioCount = scanned.count { it.mediaType == LocalMediaType.AUDIO }
                        Toast.makeText(
                                        context,
                                        "Found $videoCount video(s), $audioCount audio file(s)",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                } else {
                    Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
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
            fun tryRequestFocus(requester: FocusRequester, attempt: Int): Boolean {
                return runCatching { requester.requestFocus() }.getOrElse { error ->
                    Timber.w(error, "FocusDebug: content focus request failed (attempt $attempt)")
                    false
                }
            }
            requesters.forEach { requester ->
                if (tryRequestFocus(requester, 1)) {
                    return@LaunchedEffect
                }
                repeat(4) { attempt ->
                    delay(16)
                    if (tryRequestFocus(requester, attempt + 2)) {
                        return@LaunchedEffect
                    }
                }
            }
        }
    }

    LaunchedEffect(moveFocusToNav) {
        if (moveFocusToNav) {
            when (selectedSection) {
                Section.ALL -> allNavItemFocusRequester.requestFocus()
                Section.CONTINUE_WATCHING -> continueWatchingNavItemFocusRequester.requestFocus()
                Section.FAVORITES -> favoritesNavItemFocusRequester.requestFocus()
                Section.MOVIES -> moviesNavItemFocusRequester.requestFocus()
                Section.SERIES -> seriesNavItemFocusRequester.requestFocus()
                Section.LIVE -> liveNavItemFocusRequester.requestFocus()
                Section.CATEGORIES -> categoriesNavItemFocusRequester.requestFocus()
                Section.LOCAL_FILES -> localFilesNavItemFocusRequester.requestFocus()
                Section.SETTINGS -> settingsNavItemFocusRequester.requestFocus()
            }
            moveFocusToNav = false
        }
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

    // Per-section sync state
    data class SectionSyncState(
        val progress: Float = 0f,
        val itemsIndexed: Int = 0,
        val isActive: Boolean = false
    )
    var sectionSyncStates by remember {
        mutableStateOf(mapOf<Section, SectionSyncState>())
    }
    var librarySyncJob by remember { mutableStateOf<Job?>(null) }
    var librarySyncToken by remember { mutableStateOf(0) }

    // Track which sections have been synced
    var syncedSections by remember { mutableStateOf(setOf<Section>()) }

    val isLibrarySyncing = sectionSyncStates.values.any { it.isActive }

    fun triggerLibrarySync(config: AuthConfig, reason: String, force: Boolean, sectionsToSync: List<Section>? = null) {
        if (isLibrarySyncing) return

        val sections = sectionsToSync ?: listOf(Section.SERIES, Section.MOVIES, Section.LIVE)
        // Mark sections as syncing
        sections.forEach { section ->
            sectionSyncStates = sectionSyncStates + (section to SectionSyncState(isActive = true))
        }

        Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
        val token = librarySyncToken
        val configKey = "${config.baseUrl}|${config.username}|${config.listName}"
        librarySyncJob = coroutineScope.launch {
            val result = runCatching {
                contentRepository.syncSearchIndex(config, force, sectionsToSync) { progress ->
                    // Update progress for specific section
                    sectionSyncStates = sectionSyncStates + (progress.section to SectionSyncState(
                        progress = progress.progress,
                        itemsIndexed = progress.itemsIndexed,
                        isActive = true
                    ))
                }
            }
            if (token != librarySyncToken || accountKey != configKey) {
                // Clear syncing state
                sections.forEach { section ->
                    sectionSyncStates = sectionSyncStates - section
                }
                return@launch
            }
            val message =
                    if (result.isSuccess) {
                        // Mark sections as synced and clear syncing state
                        sections.forEach { section ->
                            sectionSyncStates = sectionSyncStates - section
                        }
                        syncedSections = syncedSections + sections
                        hasSearchIndex = contentRepository.hasSearchIndex(config)
                        "Search library ready"
                    } else {
                        // Clear syncing state on error
                        sections.forEach { section ->
                            sectionSyncStates = sectionSyncStates - section
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
            sectionSyncStates = emptyMap()
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
        if (queue != null) {
            playbackEngine.setQueue(queue.items, queue.startIndex)
            val seekPosition = resumePositionMs
            if (seekPosition != null && seekPosition > 0) {
                playbackEngine.player.seekTo(seekPosition)
                resumePositionMs = null
            }
            playbackEngine.player.playWhenReady = true
        } else {
            playbackEngine.player.stop()
            playbackEngine.player.clearMediaItems()
            if (resumeFocusId != null) {
                resumeFocusRequester.requestFocus()
            }
        }
    }

    BackHandler(enabled = showManageLists) { showManageLists = false }
    BackHandler(enabled = showAppearance) { showAppearance = false }

    val handleItemFocused: (ContentItem) -> Unit = { item -> resumeFocusId = item.id }

    val handlePlayItem: (ContentItem, List<ContentItem>) -> Unit = { item, items ->
        val config = authState.activeConfig
        if (config != null) {
            resumeFocusId = item.id
            val playableItems = items.filter(::isPlayableContent)
            activePlaybackItems = playableItems
            activePlaybackItem = item
            val queue = buildPlaybackQueue(items, item, config)
            activePlaybackQueue = queue
            activePlaybackTitle = queue.items.getOrNull(queue.startIndex)?.title ?: item.title
            coroutineScope.launch { historyRepository.addToHistory(config, item) }
        }
    }

    val handlePlayLocalFile: (Int) -> Unit = { index ->
        if (index in localFiles.indices) {
            resumeFocusId = localFiles[index].uri.toString()
            activePlaybackItems = emptyList()
            activePlaybackItem = null
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
            resumePositionMs = if (positionMs > 0) positionMs else null
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
            val position = playbackEngine.player.currentPosition
            val duration = playbackEngine.player.duration
            if (duration > 0 && position > 0) {
                val progressPercent = (position * 100) / duration
                coroutineScope.launch {
                    if (progressPercent >= 90) {
                        continueWatchingRepository.removeEntry(config, item)
                    } else {
                        continueWatchingRepository.updateProgress(config, item, position, duration)
                    }
                }
            }
        }
    }

    // Periodic playback tracking
    LaunchedEffect(activePlaybackQueue, activePlaybackItem) {
        while (activePlaybackQueue != null && activePlaybackItem != null) {
            delay(10_000)
            savePlaybackProgress()
        }
    }

    DisposableEffect(playbackEngine) {
        val listener =
                object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val title = mediaItem?.mediaMetadata?.title?.toString()
                        if (!title.isNullOrBlank()) {
                            activePlaybackTitle = title
                        }
                        val currentIndex = playbackEngine.player.currentMediaItemIndex
                        if (currentIndex >= 0 && currentIndex < activePlaybackItems.size) {
                            activePlaybackItem = activePlaybackItems[currentIndex]
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY && !playbackEngine.player.isPlaying
                        ) {
                            savePlaybackProgress()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
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
                                            "This device can't decode HEVC (H.265). Try a different device or stream.",
                                            Toast.LENGTH_LONG
                            )
                                    .show()
                            Timber.e(error, "Playback failed: HEVC not supported on this device")
                            return
                        }
                        val mediaId = playbackEngine.player.currentMediaItem?.mediaId ?: return
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
                            val currentItem = playbackEngine.player.currentMediaItem ?: return
                            playbackEngine.player.setMediaItem(
                                    currentItem.buildUpon()
                                            .setUri(nextUri)
                                            .setMimeType(guessMimeTypeForUri(nextUri))
                                            .build()
                            )
                            playbackEngine.player.prepare()
                            playbackEngine.player.playWhenReady = true
                        } else {
                            Timber.e(error, "Playback failed for $mediaId; no more fallbacks")
                            Toast.makeText(
                                            context,
                                            "Playback failed. Please try again later.",
                                            Toast.LENGTH_LONG
                            )
                                    .show()
                        }
                    }
                }
        playbackEngine.player.addListener(listener)
        playbackSettingsController.bind(playbackEngine)
        onDispose {
            playbackEngine.player.removeListener(listener)
            playbackSettingsController.unbind(playbackEngine)
            // NOTE: Don't release player here - it's released in Activity.onDestroy()
        }
    }

    LaunchedEffect(settings) { playbackSettingsController.apply(settings) }

    XtreamPlayerTheme(appTheme = settings.appTheme, fontFamily = settings.appFont.fontFamily) {
        val baseDensity = LocalDensity.current
        val uiScale = settings.uiScale.coerceIn(0.7f, 1.3f)
        val fontScale = settings.fontScale.coerceIn(0.7f, 1.4f)
        val scaledDensity = Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale * uiScale * fontScale
        )

        CompositionLocalProvider(LocalDensity provides scaledDensity) {
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
                            }
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

                // Progressive sync status indicators
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(modifier = Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Fast Search Ready indicator
                        if (syncState.fastStartReady && syncState.phase != com.example.xtreamplayer.content.SyncPhase.COMPLETE) {
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
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
                                Text("Quick Search Ready", fontSize = 12.sp, color = Color.White, fontFamily = AppTheme.fontFamily)
                            }
                        }

                        // Background Syncing indicator
                        if (syncState.phase == com.example.xtreamplayer.content.SyncPhase.BACKGROUND_FULL ||
                                        syncState.phase == com.example.xtreamplayer.content.SyncPhase.PAUSED
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF424242), RoundedCornerShape(4.dp))
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
                                val progress = currentSection?.let { syncState.sectionProgress[it] }
                                val text = if (currentSection != null && progress != null) {
                                    "Syncing ${currentSection.name.lowercase()}... (${progress.itemsIndexed} items)"
                                } else {
                                    "Syncing library..."
                                }
                                Text(text, fontSize = 11.sp, color = Color.White, fontFamily = AppTheme.fontFamily)
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

                // Show sync banner for currently selected section if syncing
                // Or show for MOVIES on initial login (when selectedSection is ALL)
                val sectionToShow = if (selectedSection == Section.ALL || selectedSection == Section.CONTINUE_WATCHING || selectedSection == Section.FAVORITES) {
                    Section.MOVIES // Show MOVIES sync on home screen
                } else {
                    selectedSection
                }

                sectionSyncStates[sectionToShow]?.let { syncState ->
                    if (syncState.isActive) {
                        LibrarySyncBanner(
                                progress = syncState.progress,
                                itemsIndexed = syncState.itemsIndexed,
                                section = sectionToShow
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    SideNav(
                            selectedSection = selectedSection,
                            onSectionSelected = {
                                selectedSection = it
                                // Trigger lazy sync for SERIES/LIVE when first accessed
                                if (it == Section.SERIES || it == Section.LIVE) {
                                    activeConfig?.let { config ->
                                        triggerSectionSync(it, config)
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
                            expanded = navExpanded,
                            allNavItemFocusRequester = allNavItemFocusRequester,
                            continueWatchingNavItemFocusRequester =
                                    continueWatchingNavItemFocusRequester,
                            favoritesNavItemFocusRequester = favoritesNavItemFocusRequester,
                            moviesNavItemFocusRequester = moviesNavItemFocusRequester,
                            seriesNavItemFocusRequester = seriesNavItemFocusRequester,
                            liveNavItemFocusRequester = liveNavItemFocusRequester,
                            categoriesNavItemFocusRequester = categoriesNavItemFocusRequester,
                            localFilesNavItemFocusRequester = localFilesNavItemFocusRequester,
                            settingsNavItemFocusRequester = settingsNavItemFocusRequester
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
                                            authViewModel.enterEditMode()
                                        },
                                        onSignOut = {
                                            showManageLists = false
                                            coroutineScope.launch {
                                                contentRepository.clearCache()
                                                contentRepository.clearDiskCache()
                                            }
                                            authViewModel.signOut(keepSaved = true)
                                        },
                                        onForgetList = {
                                            showManageLists = false
                                            coroutineScope.launch {
                                                contentRepository.clearCache()
                                                contentRepository.clearDiskCache()
                                            }
                                            authViewModel.signOut(keepSaved = false)
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
                                        onOpenFontScale = { showFontScaleDialog = true }
                                )
                            } else {
                                val activeListName =
                                        authState.activeConfig?.listName
                                                ?: savedConfig?.listName ?: "Not set"
                                SettingsScreen(
                                        settings = settings,
                                        activeListName = activeListName,
                                        contentItemFocusRequester = contentItemFocusRequester,
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
                                        onClearCache = {
                                            coroutineScope.launch {
                                                val bytes = contentRepository.diskCacheSizeBytes()
                                                contentRepository.clearCache()
                                                contentRepository.clearDiskCache()
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
                                            authViewModel.signOut(keepSaved = true)
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
                                                continueWatchingItems =
                                                        filteredContinueWatchingItems,
                                                contentItemFocusRequester =
                                                        contentItemFocusRequester,
                                                resumeFocusId = resumeFocusId,
                                                resumeFocusRequester = resumeFocusRequester,
                                                onItemFocused = handleItemFocused,
                                                onPlay = handlePlayItemWithPosition,
                                                onMoveLeft = handleMoveLeft,
                                                onToggleFavorite = handleToggleFavorite,
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
                                                isItemFavorite = isContentFavorite
                                        )
                                    }
                                    Section.CATEGORIES -> {
                                        CategorySectionScreen(
                                                title = sectionTitle,
                                                contentRepository = contentRepository,
                                                authConfig = activeConfig,
                                                settings = settings,
                                                contentItemFocusRequester =
                                                        contentItemFocusRequester,
                                                resumeFocusId = resumeFocusId,
                                                resumeFocusRequester = resumeFocusRequester,
                                                onItemFocused = handleItemFocused,
                                                onPlay = handlePlayItem,
                                                onMoveLeft = handleMoveLeft,
                                                onToggleFavorite = handleToggleFavorite,
                                                onToggleCategoryFavorite =
                                                        handleToggleCategoryFavorite,
                                                isItemFavorite = isContentFavorite,
                                                isCategoryFavorite = isCategoryFavorite
                                        )
                                    }
                                    Section.FAVORITES -> {
                                        FavoritesScreen(
                                                title = sectionTitle,
                                                contentRepository = contentRepository,
                                                authConfig = activeConfig,
                                                settings = settings,
                                                favoriteContentItems = filteredFavoriteContentItems,
                                                favoriteCategoryItems =
                                                        filteredFavoriteCategoryItems,
                                                hasFavoriteContentKeys = hasFavoriteContentKeys,
                                                hasFavoriteCategoryKeys = hasFavoriteCategoryKeys,
                                                contentItemFocusRequester =
                                                        contentItemFocusRequester,
                                                resumeFocusId = resumeFocusId,
                                                resumeFocusRequester = resumeFocusRequester,
                                                onItemFocused = handleItemFocused,
                                                onPlay = handlePlayItem,
                                                onMoveLeft = handleMoveLeft,
                                                onToggleFavorite = handleToggleFavorite,
                                                onToggleCategoryFavorite =
                                                        handleToggleCategoryFavorite,
                                                isItemFavorite = isContentFavorite,
                                                isCategoryFavorite = isCategoryFavorite
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
                                                onPlayFile = handlePlayLocalFile,
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
                                                contentItemFocusRequester =
                                                        contentItemFocusRequester,
                                                resumeFocusId = resumeFocusId,
                                                resumeFocusRequester = resumeFocusRequester,
                                                onItemFocused = handleItemFocused,
                                                onPlay = handlePlayItem,
                                                onMoveLeft = handleMoveLeft,
                                                onToggleFavorite = handleToggleFavorite,
                                                isItemFavorite = isContentFavorite
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                                val scanned = scanMediaStoreMedia(context)
                                scanned.forEach { item ->
                                    if (localFiles.none { it.uri == item.uri }) {
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
                                            scanned.count { it.mediaType == LocalMediaType.VIDEO }
                                    val audioCount =
                                            scanned.count { it.mediaType == LocalMediaType.AUDIO }
                                    Toast.makeText(
                                                    context,
                                                    "Found $videoCount video(s), $audioCount audio file(s)",
                                                    Toast.LENGTH_SHORT
                                    )
                                            .show()
                                }
                            } else {
                                storagePermissionLauncher.launch(getRequiredMediaPermissions())
                            }
                        },
                        onRefresh = {
                            if (hasStoragePermission(context)) {
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
                                                it.mediaType == LocalMediaType.VIDEO
                                            }
                                    val audioCount =
                                            scanned.count {
                                                it.mediaType == LocalMediaType.AUDIO
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

        if (activePlaybackQueue != null) {
            // Calculate next episode info for auto-play
            val currentIndex = playbackEngine.player.currentMediaItemIndex
            val queueItems = activePlaybackQueue?.items ?: emptyList()
            val hasNextEpisode = currentIndex >= 0 && currentIndex < queueItems.size - 1
            val nextEpisodeTitle = queueItems.getOrNull(currentIndex + 1)?.title
            val currentType = queueItems.getOrNull(currentIndex)?.type

            PlayerOverlay(
                    title = activePlaybackTitle ?: "",
                    player = playbackEngine.player,
                    playbackEngine = playbackEngine,
                    subtitleRepository = subtitleRepository,
                    openSubtitlesApiKey = settings.openSubtitlesApiKey,
                    openSubtitlesUserAgent = settings.openSubtitlesUserAgent,
                    mediaId = activePlaybackItem?.streamId?.toString() ?: "",
                    onRequestOpenSubtitlesApiKey = { showApiKeyDialog = true },
                    onExit = {
                        savePlaybackProgress()
                        activePlaybackQueue = null
                        activePlaybackTitle = null
                        activePlaybackItem = null
                        resumePositionMs = null
                    },
                    autoPlayNextEnabled = settings.autoPlayNext,
                    nextEpisodeThresholdSeconds = settings.nextEpisodeThresholdSeconds,
                    currentContentType = currentType,
                    nextEpisodeTitle = nextEpisodeTitle,
                    hasNextEpisode = hasNextEpisode,
                    onPlayNextEpisode = { playbackEngine.player.seekToNextMediaItem() }
            )
        }
    }
    }

    if (showApiKeyDialog) {
        ApiKeyInputDialog(
                currentKey = settings.openSubtitlesApiKey,
                currentUserAgent = settings.openSubtitlesUserAgent,
                onSave = { apiKey, userAgent ->
                    settingsViewModel.setOpenSubtitlesApiKey(apiKey.trim())
                    settingsViewModel.setOpenSubtitlesUserAgent(userAgent.trim())
                    showApiKeyDialog = false
                    Toast.makeText(context, "OpenSubtitles settings saved", Toast.LENGTH_SHORT)
                            .show()
                },
                onDismiss = { showApiKeyDialog = false }
        )
    }
    if (showThemeDialog) {
        ThemeSelectionDialog(
                themes = com.example.xtreamplayer.settings.AppThemeOption.values().toList(),
                currentTheme = settings.appTheme,
                onThemeSelected = { theme ->
                    settingsViewModel.setAppTheme(theme)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
        )
    }
    if (showFontDialog) {
        FontSelectionDialog(
                fonts = com.example.xtreamplayer.ui.theme.AppFont.values().toList(),
                currentFont = settings.appFont,
                onFontSelected = { font ->
                    settingsViewModel.setAppFont(font)
                    showFontDialog = false
                },
                onDismiss = { showFontDialog = false }
        )
    }
    if (showUiScaleDialog) {
        UiScaleDialog(
                currentScale = settings.uiScale,
                onScaleChange = { scale -> settingsViewModel.setUiScale(scale) },
                onDismiss = { showUiScaleDialog = false }
        )
    }
    if (showFontScaleDialog) {
        FontScaleDialog(
                currentScale = settings.fontScale,
                onScaleChange = { scale -> settingsViewModel.setFontScale(scale) },
                onDismiss = { showFontScaleDialog = false }
        )
    }
    if (showNextEpisodeThresholdDialog) {
        NextEpisodeThresholdDialog(
                currentSeconds = settings.nextEpisodeThresholdSeconds,
                onSecondsChange = { seconds ->
                    settingsViewModel.setNextEpisodeThreshold(seconds)
                },
                onDismiss = { showNextEpisodeThresholdDialog = false }
        )
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

private data class ActiveSubtitle(val uri: Uri, val language: String, val label: String)

private enum class LocalMediaType {
    VIDEO,
    AUDIO
}

private data class LocalFileItem(
        val uri: Uri,
        val displayName: String,
        val volumeName: String,
        val mediaType: LocalMediaType = LocalMediaType.VIDEO
)

@OptIn(UnstableApi::class)
@Composable
private fun PlayerOverlay(
        title: String,
        player: Player,
        playbackEngine: Media3PlaybackEngine,
        subtitleRepository: SubtitleRepository,
        openSubtitlesApiKey: String,
        openSubtitlesUserAgent: String,
        mediaId: String,
        onRequestOpenSubtitlesApiKey: () -> Unit,
        onExit: () -> Unit,
        autoPlayNextEnabled: Boolean,
        nextEpisodeThresholdSeconds: Int,
        currentContentType: ContentType?,
        nextEpisodeTitle: String?,
        hasNextEpisode: Boolean,
        onPlayNextEpisode: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    var controlsVisible by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(PlayerResizeMode.FIT) }
    var playerView by remember { mutableStateOf<XtreamPlayerView?>(null) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleOptionsDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showAudioBoostDialog by remember { mutableStateOf(false) }
    var showPlaybackSettingsDialog by remember { mutableStateOf(false) }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var subtitleDialogState by remember {
        mutableStateOf<SubtitleDialogState>(SubtitleDialogState.Idle)
    }
    var audioBoostDb by remember { mutableStateOf(playbackEngine.getAudioBoostDb()) }
    val subtitleCoroutineScope = rememberCoroutineScope()
    var subtitlesEnabled by remember {
        mutableStateOf(isTextTrackEnabled(player.trackSelectionParameters))
    }
    var hasEmbeddedSubtitles by remember { mutableStateOf(false) }
    var activeSubtitle by remember { mutableStateOf<ActiveSubtitle?>(null) }

    // Next episode overlay state
    var showNextEpisodeOverlay by remember { mutableStateOf(false) }
    var countdownRemaining by remember { mutableIntStateOf(15) }
    var shouldAutoPlayNext by remember { mutableStateOf(false) }

    // Threshold from settings (converted to milliseconds)
    val episodeEndThresholdMs = nextEpisodeThresholdSeconds * 1000L

    // Detect when episode is about to end (for series with auto-play enabled)
    // Countdown is based on actual remaining video time
    LaunchedEffect(player, autoPlayNextEnabled, currentContentType, hasNextEpisode) {
        if (!autoPlayNextEnabled ||
            currentContentType != ContentType.SERIES ||
            !hasNextEpisode
        ) {
            showNextEpisodeOverlay = false
            shouldAutoPlayNext = false
            return@LaunchedEffect
        }

        while (true) {
            val duration = player.duration
            val position = player.currentPosition
            val isPlaying = player.isPlaying

            if (duration > 0 && position > 0) {
                val remainingMs = duration - position

                val overlayEnabled = episodeEndThresholdMs > 0
                if (overlayEnabled && remainingMs <= episodeEndThresholdMs && remainingMs > 0) {
                    showNextEpisodeOverlay = true
                    shouldAutoPlayNext = true
                    // Countdown based on actual remaining time (capped at 30 seconds)
                    countdownRemaining = (remainingMs / 1000).toInt().coerceIn(0, 30)
                } else {
                    showNextEpisodeOverlay = false
                    shouldAutoPlayNext = false
                }
            }

            delay(500) // Check every 500ms for responsiveness
        }
    }

    DisposableEffect(player) {
        val listener =
                object : Player.Listener {
                    override fun onTrackSelectionParametersChanged(
                            parameters: TrackSelectionParameters
                    ) {
                        subtitlesEnabled = isTextTrackEnabled(parameters)
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        hasEmbeddedSubtitles = hasEmbeddedTextTracks(player)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        // Auto-play next episode when current one ends
                        val canAutoPlay =
                                autoPlayNextEnabled &&
                                        currentContentType == ContentType.SERIES &&
                                        hasNextEpisode
                        val autoPlayAtEnd =
                                episodeEndThresholdMs == 0L && playbackState == Player.STATE_ENDED
                        if (playbackState == Player.STATE_ENDED &&
                                        canAutoPlay &&
                                        (shouldAutoPlayNext || autoPlayAtEnd)
                        ) {
                            shouldAutoPlayNext = false
                            showNextEpisodeOverlay = false
                            onPlayNextEpisode()
                        }
                    }
                }
        player.addListener(listener)
        hasEmbeddedSubtitles = hasEmbeddedTextTracks(player)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(mediaId) {
        activeSubtitle =
                if (mediaId.isBlank()) {
                    null
                } else {
                    subtitleRepository.getCachedSubtitlesForMedia(mediaId).firstOrNull()?.let {
                            cached ->
                        ActiveSubtitle(
                                uri = cached.uri,
                                language = cached.language,
                                label = "Cached ${cached.language.uppercase()}"
                        )
                    }
                }
    }

    val toggleSubtitles: () -> Unit = {
        if (subtitlesEnabled) {
            if (hasExternalSubtitles(player)) {
                playbackEngine.clearExternalSubtitles()
            } else {
                playbackEngine.setSubtitlesEnabled(false)
            }
        } else {
            if (hasEmbeddedTextTracks(player)) {
                playbackEngine.refreshMediaItem()
                playbackEngine.setSubtitlesEnabled(true)
            } else {
                val fallbackSubtitle =
                        if (mediaId.isBlank()) {
                            null
                        } else {
                            subtitleRepository
                                    .getCachedSubtitlesForMedia(mediaId)
                                    .firstOrNull()
                                    ?.let { cached ->
                                        ActiveSubtitle(
                                                uri = cached.uri,
                                                language = cached.language,
                                                label = "Cached ${cached.language.uppercase()}"
                                        )
                                    }
                        }
                val subtitleToApply = activeSubtitle ?: fallbackSubtitle
                if (subtitleToApply == null) {
                    Toast.makeText(
                                    context,
                                    "No cached subtitles for this title",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                } else {
                    activeSubtitle = subtitleToApply
                    playbackEngine.addSubtitle(
                            subtitleUri = subtitleToApply.uri,
                            language = subtitleToApply.language,
                            label = subtitleToApply.label
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        if (activity != null) {
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
            onDispose {
                controller.show(WindowInsetsCompat.Type.statusBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            playerView?.resetControllerFocus()
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(showAudioBoostDialog) {
        if (showAudioBoostDialog) {
            audioBoostDb = playbackEngine.getAudioBoostDb()
        }
    }

    BackHandler(enabled = true) {
        when {
            showPlaybackSettingsDialog -> showPlaybackSettingsDialog = false
            showPlaybackSpeedDialog -> showPlaybackSpeedDialog = false
            showResolutionDialog -> showResolutionDialog = false
            else -> {
                val dismissed = playerView?.dismissSettingsWindowIfShowing() == true
                if (!dismissed) {
                    onExit()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
                modifier =
                        Modifier.fillMaxSize()
                                .focusRequester(focusRequester)
                                .focusable(interactionSource = interactionSource)
                                .onKeyEvent { event ->
                                    val isSelectKey =
                                            event.key == Key.Enter ||
                                                    event.key == Key.NumPadEnter ||
                                                    event.key == Key.DirectionCenter
                                    if (event.type == KeyEventType.KeyDown &&
                                                    isSelectKey &&
                                                    !controlsVisible
                                    ) {
                                        playerView?.showController()
                                        true
                                    } else {
                                        false
                                    }
                                },
                factory = { context ->
                    XtreamPlayerView(context).apply {
                        this.player = player
                        useController = true
                        controllerAutoShow = false
                        setControllerShowTimeoutMs(3000)
                        setShutterBackgroundColor(AndroidColor.BLACK)
                        setBackgroundColor(AndroidColor.BLACK)
                        this.resizeMode = resizeMode.resizeMode
                        forcedAspectRatio = resizeMode.forcedAspectRatio
                        setResizeModeLabel(resizeMode.label)
                        onResizeModeClick = { resizeMode = nextResizeMode(resizeMode) }
                        onBackClick = onExit
                        onSubtitleDownloadClick = { showSubtitleDialog = true }
                        onSubtitleToggleClick = { showSubtitleOptionsDialog = true }
                        onAudioTrackClick = { showAudioTrackDialog = true }
                        onAudioBoostClick = { showAudioBoostDialog = true }
                        onSettingsClick = { showPlaybackSettingsDialog = true }
                        isFocusable = true
                        isFocusableInTouchMode = true
                        setControllerVisibilityListener(
                                PlayerView.ControllerVisibilityListener { visibility ->
                                    val visible = visibility == View.VISIBLE
                                    controlsVisible = visible
                                    if (visible) {
                                        focusPlayPause()
                                    } else {
                                        resetControllerFocus()
                                    }
                                }
                        )
                        setOnKeyListener { _, _, event ->
                            if (event.action != KeyEvent.ACTION_DOWN) {
                                return@setOnKeyListener false
                            }
                            if (event.keyCode == KeyEvent.KEYCODE_BACK ||
                                            event.keyCode == KeyEvent.KEYCODE_ESCAPE
                            ) {
                                val dismissed = dismissSettingsWindowIfShowing()
                                if (!dismissed) {
                                    onExit()
                                }
                                return@setOnKeyListener true
                            }
                            val isSelect =
                                    event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                            event.keyCode == KeyEvent.KEYCODE_ENTER ||
                                            event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                            if (isSelect && !isControllerFullyVisible()) {
                                showController()
                                return@setOnKeyListener true
                            }
                            false
                        }
                        playerView = this
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = resizeMode.resizeMode
                    view.forcedAspectRatio = resizeMode.forcedAspectRatio
                    view.setResizeModeLabel(resizeMode.label)
                    view.onResizeModeClick = { resizeMode = nextResizeMode(resizeMode) }
                    view.onBackClick = onExit
                    view.onSubtitleDownloadClick = { showSubtitleDialog = true }
                    view.onSubtitleToggleClick = { showSubtitleOptionsDialog = true }
                    view.onAudioTrackClick = { showAudioTrackDialog = true }
                    view.onAudioBoostClick = { showAudioBoostDialog = true }
                    view.onSettingsClick = { showPlaybackSettingsDialog = true }
                    if (playerView != view) {
                        playerView = view
                    }
                }
        )
        if (controlsVisible) {
            Row(
                    modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp
                )
            }
        }

        // Next Episode Overlay
        if (showNextEpisodeOverlay && nextEpisodeTitle != null) {
            NextEpisodeOverlay(
                nextEpisodeTitle = nextEpisodeTitle,
                countdownSeconds = (episodeEndThresholdMs / 1000).toInt(),
                remainingSeconds = countdownRemaining,
                controlsVisible = controlsVisible,
                onPlayNext = {
                    showNextEpisodeOverlay = false
                    onPlayNextEpisode()
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    if (showSubtitleDialog) {
        SubtitleSearchDialog(
                initialQuery = title,
                state = subtitleDialogState,
                subtitlesEnabled = subtitlesEnabled,
                embeddedSubtitlesAvailable = hasEmbeddedSubtitles,
                onSearch = { query ->
                    if (openSubtitlesApiKey.isBlank()) {
                        showSubtitleDialog = false
                        subtitleDialogState = SubtitleDialogState.Idle
                        onRequestOpenSubtitlesApiKey()
                        return@SubtitleSearchDialog
                    }
                    subtitleDialogState = SubtitleDialogState.Searching
                    subtitleCoroutineScope.launch {
                        val result =
                                subtitleRepository.searchSubtitles(
                                        apiKey = openSubtitlesApiKey,
                                        userAgent = openSubtitlesUserAgent,
                                        title = query
                                )
                        subtitleDialogState =
                                result.fold(
                                        onSuccess = { subtitles ->
                                            if (subtitles.isEmpty()) {
                                                SubtitleDialogState.Results(emptyList())
                                            } else {
                                                SubtitleDialogState.Results(subtitles)
                                            }
                                        },
                                        onFailure = { error ->
                                            SubtitleDialogState.Error(
                                                    error.message ?: "Search failed"
                                            )
                                        }
                                )
                    }
                },
                onSelect = { subtitle ->
                    subtitleDialogState = SubtitleDialogState.Downloading(subtitle)
                    subtitleCoroutineScope.launch {
                        val result =
                                subtitleRepository.downloadAndCacheSubtitle(
                                        apiKey = openSubtitlesApiKey,
                                        userAgent = openSubtitlesUserAgent,
                                        subtitle = subtitle,
                                        mediaId = mediaId
                                )
                        result.fold(
                                onSuccess = { cachedSubtitle ->
                                    activeSubtitle =
                                            ActiveSubtitle(
                                                    uri = cachedSubtitle.uri,
                                                    language = cachedSubtitle.language,
                                                    label = subtitle.release
                                            )
                                    playbackEngine.addSubtitle(
                                            subtitleUri = cachedSubtitle.uri,
                                            language = cachedSubtitle.language,
                                            label = subtitle.release
                                    )
                                    subtitlesEnabled = true
                                    showSubtitleDialog = false
                                    subtitleDialogState = SubtitleDialogState.Idle
                                },
                                onFailure = { error ->
                                    subtitleDialogState =
                                            SubtitleDialogState.Error(
                                                    error.message ?: "Download failed"
                                            )
                                }
                        )
                    }
                },
                onToggleSubtitles = { toggleSubtitles() },
                onDismiss = {
                    showSubtitleDialog = false
                    subtitleDialogState = SubtitleDialogState.Idle
                }
        )
    }

    if (showSubtitleOptionsDialog) {
        SubtitleOptionsDialog(
                subtitlesEnabled = subtitlesEnabled,
                onToggleSubtitles = {
                    toggleSubtitles()
                    showSubtitleOptionsDialog = false
                },
                onDownloadSubtitles = {
                    showSubtitleOptionsDialog = false
                    showSubtitleDialog = true
                },
                onDismiss = { showSubtitleOptionsDialog = false }
        )
    }

    if (showAudioTrackDialog) {
        AudioTrackDialog(
                availableTracks = playbackEngine.getAvailableAudioTracks(),
                onTrackSelected = { groupIndex, trackIndex ->
                    playbackEngine.selectAudioTrack(groupIndex, trackIndex)
                },
                onDismiss = { showAudioTrackDialog = false }
        )
    }

    if (showAudioBoostDialog) {
        AudioBoostDialog(
                boostDb = audioBoostDb,
                onBoostChange = { newBoost ->
                    audioBoostDb = newBoost
                    playbackEngine.setAudioBoostDb(newBoost)
                },
                onDismiss = { showAudioBoostDialog = false }
        )
    }

    if (showPlaybackSettingsDialog) {
        val audioTracks = playbackEngine.getAvailableAudioTracks()
        val videoTracks = playbackEngine.getAvailableVideoTracks()
        val selectedAudio =
                audioTracks.firstOrNull { it.isSelected }?.label
                        ?: if (audioTracks.isEmpty()) "None" else "Auto"
        val selectedResolution =
                videoTracks.firstOrNull { it.isSelected }?.label
                        ?: if (videoTracks.isEmpty()) "Unknown" else videoTracks.first().label
        val speedLabel = "${player.playbackParameters.speed}x"

        PlaybackSettingsDialog(
                audioLabel = selectedAudio,
                speedLabel = speedLabel,
                resolutionLabel = selectedResolution,
                onAudio = {
                    showPlaybackSettingsDialog = false
                    showAudioTrackDialog = true
                },
                onSpeed = {
                    showPlaybackSettingsDialog = false
                    showPlaybackSpeedDialog = true
                },
                onResolution = {
                    showPlaybackSettingsDialog = false
                    showResolutionDialog = true
                },
                onDismiss = { showPlaybackSettingsDialog = false }
        )
    }

    if (showPlaybackSpeedDialog) {
        PlaybackSpeedDialog(
                currentSpeed = player.playbackParameters.speed,
                onSpeedSelected = { speed ->
                    player.setPlaybackParameters(
                            androidx.media3.common.PlaybackParameters(speed)
                    )
                },
                onDismiss = { showPlaybackSpeedDialog = false }
        )
    }

    if (showResolutionDialog) {
        VideoResolutionDialog(
                availableTracks = playbackEngine.getAvailableVideoTracks(),
                onTrackSelected = { groupIndex, trackIndex ->
                    playbackEngine.selectVideoTrack(groupIndex, trackIndex)
                },
                onDismiss = { showResolutionDialog = false }
        )
    }
}

private fun isTextTrackEnabled(parameters: TrackSelectionParameters): Boolean {
    return !parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
}

private fun hasExternalSubtitles(player: Player): Boolean {
    return player.currentMediaItem?.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true
}

private fun hasEmbeddedTextTracks(player: Player): Boolean {
    val hasTextTracks = player.currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT }
    return hasTextTracks && !hasExternalSubtitles(player)
}

@OptIn(UnstableApi::class)
private enum class PlayerResizeMode(
        val label: String,
        val resizeMode: Int,
        val forcedAspectRatio: Float?
) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT, null),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL, null),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM, null),
    ONE_TO_ONE("1:1", AspectRatioFrameLayout.RESIZE_MODE_FIT, 1f)
}

private fun nextResizeMode(current: PlayerResizeMode): PlayerResizeMode {
    val values = PlayerResizeMode.values()
    return values[(current.ordinal + 1) % values.size]
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

private fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val colors = AppTheme.colors
    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    Brush.verticalGradient(
                                            colors = listOf(
                                                    colors.background,
                                                    colors.backgroundAlt
                                            )
                                    )
                            )
    ) {
        Box(
                modifier =
                        Modifier.fillMaxHeight()
                                .width(260.dp)
                                .background(
                                        Brush.horizontalGradient(
                                                colors =
                                                        listOf(
                                                                colors.accent.copy(alpha = 0.2f),
                                                                Color.Transparent
                                                        )
                                        )
                                )
        )
        content()
    }
}

@Composable
fun SideNav(
        selectedSection: Section,
        onSectionSelected: (Section) -> Unit,
        onMoveRight: () -> Unit,
        expanded: Boolean,
        allNavItemFocusRequester: FocusRequester,
        continueWatchingNavItemFocusRequester: FocusRequester,
        favoritesNavItemFocusRequester: FocusRequester,
        moviesNavItemFocusRequester: FocusRequester,
        seriesNavItemFocusRequester: FocusRequester,
        liveNavItemFocusRequester: FocusRequester,
        categoriesNavItemFocusRequester: FocusRequester,
        localFilesNavItemFocusRequester: FocusRequester,
        settingsNavItemFocusRequester: FocusRequester
) {
    val colors = AppTheme.colors
    val animatedNavWidth by
            animateDpAsState(
                    targetValue = if (expanded) 220.dp else 0.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "navWidth"
            )
    val scrollState = rememberScrollState()

    val items = remember(
            allNavItemFocusRequester,
            continueWatchingNavItemFocusRequester,
            favoritesNavItemFocusRequester,
            moviesNavItemFocusRequester,
            seriesNavItemFocusRequester,
            liveNavItemFocusRequester,
            categoriesNavItemFocusRequester,
            localFilesNavItemFocusRequester,
            settingsNavItemFocusRequester
    ) {
            listOf(
                    NavEntry("All", Section.ALL, allNavItemFocusRequester),
                    NavEntry(
                            "Continue Watching",
                            Section.CONTINUE_WATCHING,
                            continueWatchingNavItemFocusRequester
                    ),
                    NavEntry("Favorites", Section.FAVORITES, favoritesNavItemFocusRequester),
                    NavEntry("Movies", Section.MOVIES, moviesNavItemFocusRequester),
                    NavEntry("Series", Section.SERIES, seriesNavItemFocusRequester),
                    NavEntry("Live", Section.LIVE, liveNavItemFocusRequester),
                    NavEntry("Categories", Section.CATEGORIES, categoriesNavItemFocusRequester),
                    NavEntry(
                            "Play Local Files",
                            Section.LOCAL_FILES,
                            localFilesNavItemFocusRequester
                    ),
                    NavEntry("Settings", Section.SETTINGS, settingsNavItemFocusRequester)
            )
    }

    Box(modifier = Modifier.fillMaxHeight().width(animatedNavWidth).clipToBounds()) {
        Column(
                modifier =
                        Modifier.fillMaxHeight()
                                .width(220.dp)
                                .verticalScroll(scrollState)
                                .background(colors.panelBackground)
                                .border(1.dp, colors.panelBorder)
                                .padding(horizontal = 18.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items.forEach { item ->
                NavItem(
                        label = item.label,
                        isSelected = selectedSection == item.section,
                        onClick = { onSectionSelected(item.section) },
                        focusRequester = item.focusRequester,
                        onMoveRight = onMoveRight,
                        enabled = expanded
                )
            }
        }
    }
}

data class NavEntry(val label: String, val section: Section, val focusRequester: FocusRequester)

private fun bestContrastText(background: Color, primary: Color, onAccent: Color): Color {
    val bgLuminance = background.luminance()
    val primaryLuminance = primary.luminance()
    val onAccentLuminance = onAccent.luminance()
    val primaryContrast =
            (max(bgLuminance, primaryLuminance) + 0.05f) /
                    (min(bgLuminance, primaryLuminance) + 0.05f)
    val onAccentContrast =
            (max(bgLuminance, onAccentLuminance) + 0.05f) /
                    (min(bgLuminance, onAccentLuminance) + 0.05f)
    return if (onAccentContrast >= primaryContrast) onAccent else primary
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


@Composable
fun NavItem(
        label: String,
        isSelected: Boolean = false,
        onClick: () -> Unit = {},
        focusRequester: FocusRequester,
        onMoveRight: () -> Unit,
        enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var longPressTriggered by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val colors = AppTheme.colors
    val borderColor =
            when {
                isFocused -> colors.focus
                isSelected -> colors.accentSelected
                else -> colors.border
            }
    val selectedTextColor =
            bestContrastText(colors.accentSelected, colors.textPrimary, colors.textOnAccent)
    val textColor =
            when {
                isFocused -> colors.textOnAccent
                isSelected -> selectedTextColor
                else -> colors.navText
            }
    val backgroundBrush =
            when {
                isFocused ->
                        Brush.horizontalGradient(
                                colors = listOf(colors.accent, colors.accentAlt)
                        )
                isSelected ->
                        Brush.horizontalGradient(
                                colors = listOf(colors.accentSelected, colors.accentSelectedAlt)
                        )
                else ->
                        Brush.horizontalGradient(
                                colors = listOf(colors.accentMutedAlt, colors.surfaceAlt)
                        )
            }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .height(58.dp)
                            .focusRequester(focusRequester)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .then(
                                    if (enabled) {
                                        Modifier.focusable(interactionSource = interactionSource)
                                                .onKeyEvent {
                                                    if (it.type != KeyEventType.KeyDown) {
                                                        false
                                                    } else if (it.key == Key.DirectionRight) {
                                                        onMoveRight()
                                                        true
                                                    } else if (it.key == Key.Enter ||
                                                                    it.key == Key.NumPadEnter ||
                                                                    it.key == Key.DirectionCenter
                                                    ) {
                                                        onClick()
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                .clickable(
                                                        interactionSource = interactionSource,
                                                        indication = null,
                                                        onClick = onClick
                                                )
                                    } else {
                                        Modifier
                                    }
                            )
                            .background(brush = backgroundBrush, shape = shape)
                            .border(width = 1.dp, color = borderColor, shape = shape),
            contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                    modifier =
                            Modifier.align(Alignment.CenterStart)
                                    .padding(start = 8.dp)
                                    .width(4.dp)
                                    .height(28.dp)
                            .background(
                                    color =
                                            if (isFocused) colors.textPrimary
                                            else colors.accentAlt,
                                    shape = RoundedCornerShape(4.dp)
                            )
            )
        }
        Text(
                text = label,
                color = textColor,
                fontSize = 18.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.5.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MenuButton(expanded: Boolean, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val label = if (expanded) "CLOSE" else "MENU"
    val shape = remember { RoundedCornerShape(12.dp) }
    val colors = AppTheme.colors
    val borderColor = if (isFocused) colors.focus else colors.borderStrong
    val focusedBrush = remember(colors.accent, colors.accentAlt) {
        Brush.horizontalGradient(colors = listOf(colors.accent, colors.accentAlt))
    }
    val unfocusedBrush = remember(colors.accentMutedAlt, colors.surfaceAlt) {
        Brush.horizontalGradient(colors = listOf(colors.accentMutedAlt, colors.surfaceAlt))
    }
    val buttonBrush = if (isFocused) focusedBrush else unfocusedBrush

    Box(
            modifier =
                    Modifier.width(140.dp)
                            .height(46.dp)
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyDown &&
                                                (it.key == Key.Enter ||
                                                        it.key == Key.NumPadEnter ||
                                                        it.key == Key.DirectionCenter)
                                ) {
                                    onToggle()
                                    true
                                } else {
                                    false
                                }
                            }
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onToggle
                            )
                            .background(brush = buttonBrush, shape = shape)
                            .border(width = 1.dp, color = borderColor, shape = shape),
            contentAlignment = Alignment.Center
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                            modifier =
                                    Modifier.width(18.dp)
                                            .height(2.dp)
                                            .background(
                                                    color =
                                                            if (isFocused) colors.textOnAccent
                                                            else colors.textPrimary,
                                                    shape = RoundedCornerShape(2.dp)
                                            )
                    )
                }
            }
            Text(
                    text = label,
                    color = if (isFocused) colors.textOnAccent else colors.textPrimary,
                    fontSize = 16.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun FavoriteIndicator(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val colors = AppTheme.colors
    Box(
            modifier =
                    modifier.size(22.dp)
                            .background(colors.overlayStrong, shape)
                            .border(1.dp, colors.warning, shape)
    ) {
        Icon(
                painter = painterResource(R.drawable.ic_favorite),
                contentDescription = "Favorite",
                tint = AppTheme.colors.warning,
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

@Composable
private fun TopBar(title: String, showBack: Boolean, onBack: () -> Unit, onSettings: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showBack) {
            TopBarButton(label = "BACK", onActivate = onBack)
        }
        Text(
                text = title,
                color = AppTheme.colors.textPrimary,
                fontSize = 20.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f)
        )
        TopBarButton(label = "SETTINGS", onActivate = onSettings)
    }
}

@Composable
private fun TopBarButton(
        label: String,
        onActivate: () -> Unit,
        modifier: Modifier = Modifier,
        onMoveLeft: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isFocused) AppTheme.colors.focus else AppTheme.colors.borderStrong
    val buttonBrush =
            if (isFocused) {
                Brush.horizontalGradient(colors = listOf(AppTheme.colors.accent, AppTheme.colors.accentAlt))
            } else {
                Brush.horizontalGradient(colors = listOf(AppTheme.colors.accentMutedAlt, AppTheme.colors.surfaceAlt))
            }
    Box(
            modifier =
                    modifier.height(40.dp)
                            .width(140.dp)
                            .focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                if (it.type != KeyEventType.KeyDown) {
                                    false
                                } else if (it.key == Key.DirectionLeft && onMoveLeft != null) {
                                    onMoveLeft()
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
                            .background(brush = buttonBrush, shape = shape)
                            .border(1.dp, borderColor, shape),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = label,
                color = if (isFocused) AppTheme.colors.textOnAccent else AppTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
        )
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
        fontScaleFactor: Float = 1f
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
                            .focusable(interactionSource = interactionSource)
                            .onPreviewKeyEvent {
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
                                            isLeftEdge && it.key == Key.DirectionLeft -> {
                                                onMoveLeft()
                                                true
                                            }
                                            onMoveUp != null && it.key == Key.DirectionUp -> {
                                                onMoveUp()
                                                true
                                            }
                                            isSelectKey &&
                                                    (onActivate != null || onLongClick != null) -> {
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
                                                        (onActivate != null || onLongClick != null)
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
                            .then(
                                    if (onActivate != null) {
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
        entry: ContinueWatchingEntry,
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
    val item = entry.item
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var longPressTriggered by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val colors = AppTheme.colors
    val borderColor = if (isFocused) AppTheme.colors.focus else AppTheme.colors.borderStrong
    val backgroundColor = if (isFocused) AppTheme.colors.surfaceAlt else AppTheme.colors.surface
    val title = item.title
    val subtitle = item.subtitle
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
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onMoveLeft: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        isItemFavorite: (ContentItem) -> Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    // Live uses landscape cards (3 cols at 100%), Movies/Series use poster cards (4 cols at 100%)
    val columns = remember(settings.uiScale, section) {
        if (section == Section.LIVE) {
            kotlin.math.ceil(3.0 / settings.uiScale).toInt().coerceIn(3, 6)
        } else {
            kotlin.math.ceil(4.0 / settings.uiScale).toInt().coerceIn(4, 8)
        }
    }
    val posterFontScale = remember(columns, section) {
        if (section == Section.LIVE) 3f / columns.toFloat() else 4f / columns.toFloat()
    }
    val searchState = rememberDebouncedSearchState(key = section)
    var selectedSeries by remember { mutableStateOf<ContentItem?>(null) }
    var pendingSeriesReturnFocus by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val episodesFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var pendingEpisodeFocus by remember { mutableStateOf(false) }
    LaunchedEffect(section) {
        selectedSeries = null
        pendingSeriesReturnFocus = false
        pendingEpisodeFocus = false
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
                        contentItemFocusRequester = contentItemFocusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        episodesFocusRequester = episodesFocusRequester,
                        pendingEpisodeFocus = pendingEpisodeFocus,
                        onEpisodeFocusHandled = { pendingEpisodeFocus = false },
                        onItemFocused = onItemFocused,
                        onPlay = onPlay,
                        onMoveLeft = onMoveLeft,
                        onBack = {
                            onItemFocused(selectedSeries!!)
                            runCatching { contentItemFocusRequester.requestFocus() }
                            pendingSeriesReturnFocus = true
                            selectedSeries = null
                            pendingEpisodeFocus = false
                        },
                        onToggleFavorite = onToggleFavorite,
                        isItemFavorite = isItemFavorite
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
                                                        selectedSeries = item
                                                    } else {
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
                                    fontScaleFactor = if (posterHint) posterFontScale else 1f
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalFilesScreen(
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
        favoriteContentItems: List<ContentItem>,
        favoriteCategoryItems: List<CategoryItem>,
        hasFavoriteContentKeys: Boolean,
        hasFavoriteCategoryKeys: Boolean,
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onMoveLeft: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onToggleCategoryFavorite: (CategoryItem) -> Unit,
        isItemFavorite: (ContentItem) -> Boolean,
        isCategoryFavorite: (CategoryItem) -> Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    // Poster content scales with UI, categories stay fixed
    val posterColumns = remember(settings.uiScale) {
        kotlin.math.ceil(4.0 / settings.uiScale).toInt().coerceIn(4, 8)
    }
    val posterFontScale = remember(posterColumns) { 4f / posterColumns.toFloat() }
    val categoryColumns = 3
    var activeView by remember { mutableStateOf(FavoritesView.MENU) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var selectedSeries by remember { mutableStateOf<ContentItem?>(null) }
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
            pendingSeriesReturnFocus = false
            pendingCategoryEnterFocus = false
            pendingEpisodeFocus = false
        }
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
                        contentItemFocusRequester = contentItemFocusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        episodesFocusRequester = episodesFocusRequester,
                        pendingEpisodeFocus = pendingEpisodeFocus,
                        onEpisodeFocusHandled = { pendingEpisodeFocus = false },
                        onItemFocused = onItemFocused,
                        onPlay = onPlay,
                        onMoveLeft = onMoveLeft,
                        onBack = {
                            onItemFocused(selectedSeries!!)
                            runCatching { contentItemFocusRequester.requestFocus() }
                            pendingSeriesReturnFocus = true
                            selectedSeries = null
                            pendingEpisodeFocus = false
                        },
                        onToggleFavorite = onToggleFavorite,
                        isItemFavorite = isItemFavorite
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
                                            selectedSeries = item
                                        } else {
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
                        contentItemFocusRequester = contentItemFocusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        episodesFocusRequester = episodesFocusRequester,
                        pendingEpisodeFocus = pendingEpisodeFocus,
                        onEpisodeFocusHandled = { pendingEpisodeFocus = false },
                        onItemFocused = onItemFocused,
                        onPlay = onPlay,
                        onMoveLeft = onMoveLeft,
                        onBack = {
                            onItemFocused(selectedSeries!!)
                            runCatching { contentItemFocusRequester.requestFocus() }
                            pendingSeriesReturnFocus = true
                            selectedSeries = null
                            pendingEpisodeFocus = false
                        },
                        onToggleFavorite = onToggleFavorite,
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
                            LazyVerticalGrid(
                                    columns = GridCells.Fixed(posterColumns),
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
                                    val isLeftEdge = index % posterColumns == 0
                                    val isTopRow = index < posterColumns
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
                                                                selectedSeries = item
                                                            } else {
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
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onMoveLeft: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onToggleCategoryFavorite: (CategoryItem) -> Unit,
        isItemFavorite: (ContentItem) -> Boolean,
        isCategoryFavorite: (CategoryItem) -> Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    var activeType by remember { mutableStateOf(ContentType.LIVE) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var selectedSeries by remember { mutableStateOf<ContentItem?>(null) }
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
    val posterColumns = remember(settings.uiScale, activeType) {
        if (activeType == ContentType.LIVE) {
            kotlin.math.ceil(3.0 / settings.uiScale).toInt().coerceIn(3, 6)
        } else {
            kotlin.math.ceil(4.0 / settings.uiScale).toInt().coerceIn(4, 8)
        }
    }
    val posterFontScale = remember(posterColumns, activeType) {
        if (activeType == ContentType.LIVE) 3f / posterColumns.toFloat() else 4f / posterColumns.toFloat()
    }
    val categoryColumns = 3  // Fixed for category cards
    val categoryGridState = rememberLazyGridState()
    val contentGridState = rememberLazyGridState()
    val tabFocusRequesters = remember { ContentType.values().map { FocusRequester() } }
    val backTabFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val episodesFocusRequester = remember { FocusRequester() }
    var pendingEpisodeFocus by remember { mutableStateOf(false) }
    val useStackedHeader = settings.uiScale >= 1.2f

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
                val tabsContent: @Composable () -> Unit = {
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
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabsContent()
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tabsContent()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                                    tabFocusRequesters.firstOrNull()?.requestFocus()
                                }
                            },
                            onMoveUp = {
                                if (selectedCategory != null) {
                                    backTabFocusRequester.requestFocus()
                                } else {
                                    tabFocusRequesters.firstOrNull()?.requestFocus()
                                }
                            },
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
                                    val requester =
                                            if (selectedSeries == null) {
                                                when {
                                                    item?.id != null &&
                                                            (item.id == lastCategoryContentId ||
                                                                    item.id == resumeFocusId) ->
                                                            resumeFocusRequester
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
                                                                selectedSeries = item
                                                            } else {
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
                                contentItemFocusRequester = contentItemFocusRequester,
                                resumeFocusId = resumeFocusId,
                                resumeFocusRequester = resumeFocusRequester,
                                episodesFocusRequester = episodesFocusRequester,
                                pendingEpisodeFocus = pendingEpisodeFocus,
                                onEpisodeFocusHandled = { pendingEpisodeFocus = false },
                                onItemFocused = onItemFocused,
                                onPlay = onPlay,
                                onMoveLeft = onMoveLeft,
                                onBack = {
                                    onItemFocused(selectedSeries!!)
                                    runCatching { contentItemFocusRequester.requestFocus() }
                                    pendingSeriesReturnFocus = true
                                    selectedSeries = null
                                    pendingEpisodeFocus = false
                                },
                                onToggleFavorite = onToggleFavorite,
                                isItemFavorite = isItemFavorite,
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
                    LazyVerticalGrid(
                            columns = GridCells.Fixed(categoryColumns),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            state = categoryGridState
                    ) {
                        items(filteredCategories.size) { index ->
                            val category = filteredCategories[index]
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

@Composable
fun ContinueWatchingScreen(
        title: String,
        contentRepository: ContentRepository,
        authConfig: AuthConfig,
        settings: SettingsState,
        continueWatchingItems: List<ContinueWatchingEntry>,
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, Long) -> Unit,
        onMoveLeft: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        onRemoveEntry: (ContentItem) -> Unit,
        isItemFavorite: (ContentItem) -> Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    // Use 4 columns at 100% (poster sizing, since continue watching includes mixed content)
    val columns = remember(settings.uiScale) {
        kotlin.math.ceil(4.0 / settings.uiScale).toInt().coerceIn(4, 8)
    }
    val posterFontScale = remember(columns) { 4f / columns.toFloat() }
    val hasItems = continueWatchingItems.isNotEmpty()

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
            Text(
                    text = title,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (continueWatchingItems.isEmpty()) {
                Text(
                        text = "No items in progress",
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
                            count = continueWatchingItems.size,
                            key = { index -> continueWatchingItems[index].key }
                    ) { index ->
                        val entry = continueWatchingItems[index]
                        val item = entry.item
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
                                onActivate = { onPlay(item, entry.positionMs) },
                                onFocused = { onItemFocused(item) },
                                onMoveLeft = onMoveLeft,
                                onLongClick = { onToggleFavorite(item) },
                                onRemove = { onRemoveEntry(item) }
                        )
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
        contentItemFocusRequester: FocusRequester,
        resumeFocusId: String?,
        resumeFocusRequester: FocusRequester,
        episodesFocusRequester: FocusRequester,
        pendingEpisodeFocus: Boolean,
        onEpisodeFocusHandled: () -> Unit,
        onItemFocused: (ContentItem) -> Unit,
        onPlay: (ContentItem, List<ContentItem>) -> Unit,
        onMoveLeft: () -> Unit,
        onBack: () -> Unit,
        onToggleFavorite: (ContentItem) -> Unit,
        isItemFavorite: (ContentItem) -> Boolean,
        forceDarkText: Boolean = false,
        onMoveUpFromTop: (() -> Unit)? = null
) {
    BackHandler(enabled = true) { onBack() }
    var seasonGroups by remember { mutableStateOf<List<SeasonGroup>>(emptyList()) }
    var selectedSeasonIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var initialFocusSet by remember { mutableStateOf(false) }
    var placeholderFocused by remember { mutableStateOf(false) }
    var internalEpisodeFocus by remember { mutableStateOf(false) }
    val backFocusRequester = remember { FocusRequester() }

    LaunchedEffect(seriesItem.streamId, authConfig) {
        isLoading = true
        errorMessage = null
        initialFocusSet = false
        placeholderFocused = false
        selectedSeasonIndex = 0
        internalEpisodeFocus = false
        val result = runCatching {
            contentRepository.loadSeriesEpisodes(seriesItem.streamId, authConfig)
        }
        result
                .onSuccess { episodes ->
                    val grouped =
                            episodes
                                    .groupBy { seasonLabelFromSubtitle(it.subtitle) }
                                    .map { (label, items) ->
                                        SeasonGroup(
                                                label = label,
                                                displayLabel = buildSeasonLabel(label),
                                                seasonNumber = seasonNumberFromLabel(label),
                                                episodes = items
                                        )
                                    }
                                    .sortedWith(
                                            compareBy<SeasonGroup> { it.seasonNumber }.thenBy {
                                                it.displayLabel
                                            }
                                    )
                    seasonGroups = grouped
                    selectedSeasonIndex = 0
                }
                .onFailure { error -> errorMessage = error.message ?: "Failed to load seasons" }
        isLoading = false
    }

    val seasonFocusRequesters =
            remember(seasonGroups.size) { List(seasonGroups.size) { FocusRequester() } }
    val seasonPrimaryFocusRequester = remember { FocusRequester() }
    fun seasonRequesterFor(index: Int): FocusRequester? {
        return if (index == 0) {
            seasonPrimaryFocusRequester
        } else {
            seasonFocusRequesters.getOrNull(index)
        }
    }

    // Keep focus inside the series view when the details screen opens.

    val selectedSeason = seasonGroups.getOrNull(selectedSeasonIndex)
    val selectedEpisodes = selectedSeason?.episodes.orEmpty()
    val columns = 1
    val handleEpisodeFocused: (ContentItem) -> Unit = { item -> onItemFocused(item) }
    val shouldRequestEpisodeFocus = pendingEpisodeFocus || internalEpisodeFocus
    LaunchedEffect(
            isLoading,
            errorMessage,
            seasonGroups.size,
            initialFocusSet,
            placeholderFocused
    ) {
        if (!initialFocusSet && (isLoading || errorMessage != null || seasonGroups.isEmpty())) {
            if (!placeholderFocused) {
                withFrameNanos {}
                contentItemFocusRequester.requestFocus()
                placeholderFocused = true
            }
            if (!isLoading && (errorMessage != null || seasonGroups.isEmpty())) {
                initialFocusSet = true
            }
        }
        if (!initialFocusSet && seasonGroups.isNotEmpty()) {
            withFrameNanos {}
            contentItemFocusRequester.requestFocus()
            initialFocusSet = true
        }
    }

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
        if (isLoading) {
            Text(
                    text = "Loading seasons...",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = AppTheme.fontFamily,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.focusRequester(contentItemFocusRequester).focusable()
            )
        } else if (errorMessage != null) {
            Text(
                    text = errorMessage ?: "Failed to load seasons",
                    color = AppTheme.colors.error,
                    fontSize = 14.sp,
                    fontFamily = AppTheme.fontFamily,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.focusRequester(contentItemFocusRequester).focusable()
            )
        } else if (seasonGroups.isEmpty()) {
            Text(
                    text = "No seasons yet",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = AppTheme.fontFamily,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.focusRequester(contentItemFocusRequester).focusable()
            )
        } else {
            Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                        modifier =
                                Modifier.width(220.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AppTheme.colors.panelBackground)
                                        .border(1.dp, AppTheme.colors.panelBorder, RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                        .focusRequester(contentItemFocusRequester)
                                        .focusable()
                                        .onFocusChanged { state ->
                                            if (state.isFocused) {
                                                // Reset resume focus to series (not episode) when entering seasons
                                                onItemFocused(seriesItem)
                                                seasonRequesterFor(selectedSeasonIndex)
                                                        ?.requestFocus()
                                            }
                                        }
                ) {
                    Text(
                            text = "SEASONS",
                            color = AppTheme.colors.textTertiary,
                            fontSize = 11.sp,
                            fontFamily = AppTheme.fontFamily,
                            letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                                count = seasonGroups.size,
                                key = { index -> seasonGroups[index].displayLabel }
                        ) { index ->
                            val season = seasonGroups[index]
                            val label = "${season.displayLabel} (${season.episodes.size})"
                            CategoryTypeTab(
                                    label = label,
                                    selected = index == selectedSeasonIndex,
                                    focusRequester = seasonRequesterFor(index),
                                    onActivate = {
                                        selectedSeasonIndex = index
                                        // Don't auto-focus episodes - user must press Right to
                                        // navigate there
                                    },
                                    onMoveLeft = onMoveLeft,
                                    onMoveRight = { internalEpisodeFocus = true },
                                    onMoveUp =
                                            if (index == 0) {
                                                { backFocusRequester.requestFocus() }
                                            } else {
                                                null
                                            }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = selectedSeason?.displayLabel ?: "Select a season",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (selectedEpisodes.isEmpty()) {
                        Text(
                                text = "No episodes yet",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 14.sp,
                                fontFamily = AppTheme.fontFamily,
                                letterSpacing = 0.6.sp
                        )
                    } else {
                        LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                    count = selectedEpisodes.size,
                                    key = { index -> selectedEpisodes[index].id }
                            ) { index ->
                                val item = selectedEpisodes[index]
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
                                        internalEpisodeFocus = false
                                    }
                                }
                                val isLeftEdge = index % columns == 0
                                val isTopRow = index < columns
                                ContentCard(
                                        item = item,
                                        focusRequester = requester,
                                        isLeftEdge = isLeftEdge,
                                        isFavorite = isItemFavorite(item),
                                        onActivate = { onPlay(item, selectedEpisodes) },
                                        onFocused = handleEpisodeFocused,
                                        onMoveLeft = {
                                            seasonRequesterFor(selectedSeasonIndex)
                                                    ?.requestFocus()
                                                    ?: onMoveLeft()
                                        },
                                        onMoveUp =
                                                if (isTopRow) {
                                                    {
                                                        if (onMoveUpFromTop != null) {
                                                            onMoveUpFromTop()
                                                        } else {
                                                            contentItemFocusRequester.requestFocus()
                                                        }
                                                    }
                                                } else {
                                                    null
                                        },
                                        onLongClick = { onToggleFavorite(item) },
                                        titleFontSize = 13.sp,
                                        subtitleFontSize = 10.sp,
                                        forceDarkText = forceDarkText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SeasonGroup(
        val label: String,
        val displayLabel: String,
        val seasonNumber: Int,
        val episodes: List<ContentItem>
)

private fun seasonLabelFromSubtitle(subtitle: String): String {
    val prefix = subtitle.substringBefore("·").trim()
    if (prefix.startsWith("S", ignoreCase = true)) {
        val trimmed = prefix.drop(1).trim()
        if (trimmed.isNotBlank()) {
            return trimmed
        }
    }
    return prefix.ifBlank { "1" }
}

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

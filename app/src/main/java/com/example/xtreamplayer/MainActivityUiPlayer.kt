package com.example.xtreamplayer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MimeTypes
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.xtreamplayer.content.ContentType
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.player.Media3PlaybackEngine
import com.example.xtreamplayer.player.XtreamPlayerView
import com.example.xtreamplayer.ui.AudioBoostDialog
import com.example.xtreamplayer.ui.AudioTrackDialog
import com.example.xtreamplayer.ui.NextEpisodeOverlay
import com.example.xtreamplayer.ui.PlaybackSettingsDialog
import com.example.xtreamplayer.ui.PlaybackSpeedDialog
import com.example.xtreamplayer.ui.SubtitleDialogState
import com.example.xtreamplayer.ui.SubtitleOptionsDialog
import com.example.xtreamplayer.ui.SubtitleSearchDialog
import com.example.xtreamplayer.ui.VideoResolutionDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ActiveSubtitle(
    val uri: Uri,
    val language: String,
    val label: String,
    val fileName: String?
)

private fun sanitizeSubtitleQuery(rawTitle: String): String {
    if (rawTitle.isBlank()) return rawTitle
    var query = rawTitle
    query = query.replace(Regex("^\\s*[A-Z]{2,3}\\s*-\\s*"), "")
    query = query.replace(Regex("\\[[^\\]]*\\]"), "")
    query = query.replace(Regex("\\([^\\)]*\\)"), "")
    query = query.replace(Regex("\\s+-\\s*\\d{4}\\s*$"), "")
    query = query.replace(Regex("\\s+\\d{4}\\s*$"), "")
    query = query.replace(Regex("\\s+S\\d+\\s*E\\d+.*$", RegexOption.IGNORE_CASE), "")
    query = query.replace(Regex("\\s+"), " ").trim()
    return if (query.isBlank()) rawTitle.trim() else query
}

private fun subtitleMimeType(fileName: String?): String {
    val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return when (ext) {
        "vtt" -> MimeTypes.TEXT_VTT
        "ssa", "ass" -> MimeTypes.TEXT_SSA
        "ttml", "dfxp" -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

internal enum class LocalMediaType {
    VIDEO,
    AUDIO
}

internal data class LocalFileItem(
        val uri: Uri,
        val displayName: String,
        val volumeName: String,
        val mediaType: LocalMediaType = LocalMediaType.VIDEO
)

data class PendingResume(
        val mediaId: String,
        val positionMs: Long,
        val shouldPlay: Boolean
)

@OptIn(UnstableApi::class)
@Composable
internal fun PlayerOverlay(
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
        onPlayNextEpisode: () -> Unit,
        onLiveChannelSwitch: (Int) -> Boolean
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
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
    val hasModalOpen =
            showPlaybackSettingsDialog ||
                    showResolutionDialog ||
                    showAudioTrackDialog ||
                    showAudioBoostDialog ||
                    showPlaybackSpeedDialog ||
                    showSubtitleDialog ||
                    showSubtitleOptionsDialog
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
                                label = "Cached ${cached.language.uppercase()}",
                                fileName = cached.fileName
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
                                                label = "Cached ${cached.language.uppercase()}",
                                                fileName = cached.fileName
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
                            label = subtitleToApply.label,
                            mimeType = subtitleMimeType(subtitleToApply.fileName)
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
                        defaultControllerTimeoutMs = 3000
                        setShutterBackgroundColor(AndroidColor.BLACK)
                        setBackgroundColor(AndroidColor.BLACK)
                        this.resizeMode = resizeMode.resizeMode
                        forcedAspectRatio = resizeMode.forcedAspectRatio
                        setResizeModeLabel(resizeMode.label)
                        titleText = title
                        onResizeModeClick = { resizeMode = nextResizeMode(resizeMode) }
                        onBackClick = onExit
                        onSubtitleDownloadClick = { showSubtitleDialog = true }
                        onSubtitleToggleClick = { showSubtitleOptionsDialog = true }
                        onAudioTrackClick = { showAudioTrackDialog = true }
                        onAudioBoostClick = { showAudioBoostDialog = true }
                        onSettingsClick = { showPlaybackSettingsDialog = true }
                        isLiveContent = currentContentType == ContentType.LIVE
                        fastSeekEnabled = currentContentType != ContentType.LIVE
                        onToggleControls = {
                            if (hasModalOpen) {
                                false
                            } else {
                                if (isControllerFullyVisible()) {
                                    hideController()
                                } else {
                                    showController()
                                }
                                true
                            }
                        }
                        onChannelUp = {
                            if (!hasModalOpen && currentContentType == ContentType.LIVE) {
                                onLiveChannelSwitch(1)
                            } else {
                                false
                            }
                        }
                        onChannelDown = {
                            if (!hasModalOpen && currentContentType == ContentType.LIVE) {
                                onLiveChannelSwitch(-1)
                            } else {
                                false
                            }
                        }
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
                            val hasModalOpen =
                                    showPlaybackSettingsDialog ||
                                            showResolutionDialog ||
                                            showAudioTrackDialog ||
                                            showAudioBoostDialog ||
                                            showPlaybackSpeedDialog ||
                                            showSubtitleDialog ||
                                            showSubtitleOptionsDialog
                            // Live channel switching is handled inside XtreamPlayerView based on controller visibility.
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
                    view.titleText = title
                    view.onResizeModeClick = { resizeMode = nextResizeMode(resizeMode) }
                    view.onBackClick = onExit
                    view.onSubtitleDownloadClick = { showSubtitleDialog = true }
                    view.onSubtitleToggleClick = { showSubtitleOptionsDialog = true }
                    view.onAudioTrackClick = { showAudioTrackDialog = true }
                    view.onAudioBoostClick = { showAudioBoostDialog = true }
                    view.onSettingsClick = { showPlaybackSettingsDialog = true }
                    view.isLiveContent = currentContentType == ContentType.LIVE
                    view.fastSeekEnabled = currentContentType != ContentType.LIVE
                    view.defaultControllerTimeoutMs = 3000
                    view.onToggleControls = {
                        if (hasModalOpen) {
                            false
                        } else {
                            if (view.isControllerFullyVisible()) {
                                view.hideController()
                            } else {
                                view.showController()
                            }
                            true
                        }
                    }
                    view.onChannelUp = {
                        if (!hasModalOpen && currentContentType == ContentType.LIVE) {
                            onLiveChannelSwitch(1)
                        } else {
                            false
                        }
                    }
                    view.onChannelDown = {
                        if (!hasModalOpen && currentContentType == ContentType.LIVE) {
                            onLiveChannelSwitch(-1)
                        } else {
                            false
                        }
                    }
                    if (playerView != view) {
                        playerView = view
                    }
                }
        )
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
        val subtitleQuery = remember(title) { sanitizeSubtitleQuery(title) }
        SubtitleSearchDialog(
                initialQuery = subtitleQuery,
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
                                    android.util.Log.d("PlayerOverlay", "Subtitle downloaded successfully: uri=${cachedSubtitle.uri}, language=${cachedSubtitle.language}, fileName=${cachedSubtitle.fileName}")
                                    activeSubtitle =
                                            ActiveSubtitle(
                                                    uri = cachedSubtitle.uri,
                                                    language = cachedSubtitle.language,
                                                    label = subtitle.release,
                                                    fileName = cachedSubtitle.fileName
                                            )
                                    val mimeType = subtitleMimeType(cachedSubtitle.fileName)
                                    android.util.Log.d("PlayerOverlay", "Adding subtitle to player: mimeType=$mimeType")
                                    playbackEngine.addSubtitle(
                                            subtitleUri = cachedSubtitle.uri,
                                            language = cachedSubtitle.language,
                                            label = subtitle.release,
                                            mimeType = mimeType
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
        val resolutionMode =
                if (playbackEngine.isVideoOverrideActive()) {
                    "Manual"
                } else {
                    "Auto"
                }
        val resolutionLabel = "$resolutionMode \u2022 $selectedResolution"
        val showSpeedOption = currentContentType != ContentType.LIVE

        PlaybackSettingsDialog(
                audioLabel = selectedAudio,
                speedLabel = speedLabel,
                resolutionLabel = resolutionLabel,
                showSpeedOption = showSpeedOption,
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
        if (currentContentType == ContentType.LIVE) {
            showPlaybackSpeedDialog = false
        } else {
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

private fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

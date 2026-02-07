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
import androidx.compose.runtime.neverEqualPolicy
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
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.content.ContentType
import com.example.xtreamplayer.content.LiveNowNextEpg
import com.example.xtreamplayer.content.LiveProgramInfo
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.player.Media3PlaybackEngine
import com.example.xtreamplayer.player.XtreamPlayerView
import com.example.xtreamplayer.ui.AudioBoostDialog
import com.example.xtreamplayer.ui.AudioTrackDialog
import com.example.xtreamplayer.ui.NextEpisodeOverlay
import com.example.xtreamplayer.ui.PlaybackSettingsDialog
import com.example.xtreamplayer.ui.PlaybackSpeedDialog
import com.example.xtreamplayer.ui.SubtitleDialogState
import com.example.xtreamplayer.ui.SubtitleOffsetDialog
import com.example.xtreamplayer.ui.SubtitleOptionsDialog
import com.example.xtreamplayer.ui.SubtitleSearchDialog
import com.example.xtreamplayer.ui.VideoResolutionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LIVE_EPG_REFRESH_INTERVAL_MS = 60_000L
private const val LIVE_EPG_CHANNEL_CHANGE_DEBOUNCE_MS = 250L
private val LANGUAGE_PREFIX_REGEX = Regex("^\\s*[A-Z]{2,3}\\s*-\\s*")
private val BRACKET_TEXT_REGEX = Regex("\\[[^\\]]*\\]")
private val PAREN_TEXT_REGEX = Regex("\\([^\\)]*\\)")
private val TRAILING_YEAR_DASH_REGEX = Regex("\\s+-\\s*\\d{4}\\s*$")
private val TRAILING_YEAR_REGEX = Regex("\\s+\\d{4}\\s*$")
private val TRAILING_SEASON_EPISODE_REGEX = Regex("\\s+S\\d+\\s*E\\d+.*$", RegexOption.IGNORE_CASE)
private val MULTI_SPACE_REGEX = Regex("\\s+")
private val SRT_TIME_PATTERN = Regex("""(\d{2}):(\d{2}):(\d{2}),(\d{3})""")
private val VTT_TIME_PATTERN = Regex("""(?:(\d{2}):)?(\d{2}):(\d{2})[.](\d{3})""")
private val ASS_TIME_PATTERN = Regex("""(\d+):(\d{2}):(\d{2})[.](\d{2})""")
private val LIVE_PROGRAM_TIME_FORMATTER = ThreadLocal.withInitial {
    SimpleDateFormat("HH:mm", Locale.getDefault())
}

private data class ActiveSubtitle(
    val uri: Uri,
    val language: String,
    val label: String,
    val fileName: String?,
    val originalContent: ByteArray? = null  // Store original content for offset adjustment
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActiveSubtitle) return false
        return uri == other.uri && language == other.language && label == other.label && fileName == other.fileName
    }
    override fun hashCode(): Int = uri.hashCode()
}

private fun sanitizeSubtitleQuery(rawTitle: String): String {
    if (rawTitle.isBlank()) return rawTitle
    var query = rawTitle
    query = query.replace(LANGUAGE_PREFIX_REGEX, "")
    query = query.replace(BRACKET_TEXT_REGEX, "")
    query = query.replace(PAREN_TEXT_REGEX, "")
    query = query.replace(TRAILING_YEAR_DASH_REGEX, "")
    query = query.replace(TRAILING_YEAR_REGEX, "")
    query = query.replace(TRAILING_SEASON_EPISODE_REGEX, "")
    query = query.replace(MULTI_SPACE_REGEX, " ").trim()
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

private enum class SubtitleFormat {
    SRT,
    VTT,
    ASS,
    SSA,
    UNKNOWN
}

private fun subtitleFormat(fileName: String?): SubtitleFormat {
    return when (fileName?.substringAfterLast('.', "")?.lowercase()) {
        "srt" -> SubtitleFormat.SRT
        "vtt" -> SubtitleFormat.VTT
        "ass" -> SubtitleFormat.ASS
        "ssa" -> SubtitleFormat.SSA
        else -> SubtitleFormat.UNKNOWN
    }
}

private fun isOffsetSupported(fileName: String?): Boolean {
    return subtitleFormat(fileName) != SubtitleFormat.UNKNOWN
}

private data class DecodedSubtitle(val text: String, val charset: java.nio.charset.Charset)

private fun decodeSubtitle(content: ByteArray): DecodedSubtitle {
    val utf8 = content.toString(Charsets.UTF_8)
    return if (utf8.contains('\uFFFD')) {
        DecodedSubtitle(content.toString(Charsets.ISO_8859_1), Charsets.ISO_8859_1)
    } else {
        DecodedSubtitle(utf8, Charsets.UTF_8)
    }
}

private fun applySubtitleOffset(
    content: ByteArray,
    offsetMs: Long,
    format: SubtitleFormat
): ByteArray {
    if (offsetMs == 0L || format == SubtitleFormat.UNKNOWN) return content
    val decoded = decodeSubtitle(content)
    val adjustedText = when (format) {
        SubtitleFormat.SRT -> applySrtOffset(decoded.text, offsetMs)
        SubtitleFormat.VTT -> applyVttOffset(decoded.text, offsetMs)
        SubtitleFormat.ASS, SubtitleFormat.SSA -> applyAssOffset(decoded.text, offsetMs)
        SubtitleFormat.UNKNOWN -> decoded.text
    }
    return adjustedText.toByteArray(decoded.charset)
}

// Apply timing offset to SRT subtitle content
private fun applySrtOffset(text: String, offsetMs: Long): String {
    if (offsetMs == 0L) return text

    return SRT_TIME_PATTERN.replace(text) { match ->
        val hours = match.groupValues[1].toInt()
        val minutes = match.groupValues[2].toInt()
        val seconds = match.groupValues[3].toInt()
        val millis = match.groupValues[4].toInt()

        val totalMs = (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis + offsetMs
        val adjustedMs = maxOf(0L, totalMs)  // Don't go negative

        val newHours = (adjustedMs / 3600000).toInt()
        val newMinutes = ((adjustedMs % 3600000) / 60000).toInt()
        val newSeconds = ((adjustedMs % 60000) / 1000).toInt()
        val newMillis = (adjustedMs % 1000).toInt()

        formatSrtTimestamp(newHours, newMinutes, newSeconds, newMillis)
    }
}

private fun applyVttOffset(text: String, offsetMs: Long): String {
    return VTT_TIME_PATTERN.replace(text) { match ->
        val hasHours = match.groupValues[1].isNotEmpty()
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toInt()
        val seconds = match.groupValues[3].toInt()
        val millis = match.groupValues[4].toInt()
        val totalMs = (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis + offsetMs
        val adjustedMs = maxOf(0L, totalMs)
        val newHours = (adjustedMs / 3600000).toInt()
        val newMinutes = ((adjustedMs % 3600000) / 60000).toInt()
        val newSeconds = ((adjustedMs % 60000) / 1000).toInt()
        val newMillis = (adjustedMs % 1000).toInt()
        if (hasHours || newHours > 0) {
            formatVttTimestamp(newHours, newMinutes, newSeconds, newMillis)
        } else {
            val totalMinutes = (adjustedMs / 60000).toInt()
            formatVttTimestampNoHours(totalMinutes, newSeconds, newMillis)
        }
    }
}

private fun applyAssOffset(text: String, offsetMs: Long): String {
    return ASS_TIME_PATTERN.replace(text) { match ->
        val hours = match.groupValues[1].toInt()
        val minutes = match.groupValues[2].toInt()
        val seconds = match.groupValues[3].toInt()
        val centis = match.groupValues[4].toInt()
        val totalMs = (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + (centis * 10L) + offsetMs
        val adjustedMs = maxOf(0L, totalMs)
        val newHours = (adjustedMs / 3600000).toInt()
        val newMinutes = ((adjustedMs % 3600000) / 60000).toInt()
        val newSeconds = ((adjustedMs % 60000) / 1000).toInt()
        val newCentis = ((adjustedMs % 1000) / 10).toInt()
        formatAssTimestamp(newHours, newMinutes, newSeconds, newCentis)
    }
}

private fun StringBuilder.appendPadded(value: Int, width: Int) {
    val text = value.toString()
    repeat((width - text.length).coerceAtLeast(0)) { append('0') }
    append(text)
}

private fun formatSrtTimestamp(hours: Int, minutes: Int, seconds: Int, millis: Int): String {
    return buildString(12) {
        appendPadded(hours, 2)
        append(':')
        appendPadded(minutes, 2)
        append(':')
        appendPadded(seconds, 2)
        append(',')
        appendPadded(millis, 3)
    }
}

private fun formatVttTimestamp(hours: Int, minutes: Int, seconds: Int, millis: Int): String {
    return buildString(12) {
        appendPadded(hours, 2)
        append(':')
        appendPadded(minutes, 2)
        append(':')
        appendPadded(seconds, 2)
        append('.')
        appendPadded(millis, 3)
    }
}

private fun formatVttTimestampNoHours(totalMinutes: Int, seconds: Int, millis: Int): String {
    return buildString(9) {
        appendPadded(totalMinutes, 2)
        append(':')
        appendPadded(seconds, 2)
        append('.')
        appendPadded(millis, 3)
    }
}

private fun formatAssTimestamp(hours: Int, minutes: Int, seconds: Int, centis: Int): String {
    return buildString(11) {
        append(hours)
        append(':')
        appendPadded(minutes, 2)
        append(':')
        appendPadded(seconds, 2)
        append('.')
        appendPadded(centis, 2)
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
        activePlaybackItem: ContentItem?,
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
        onLiveChannelSwitch: (Int) -> Boolean,
        loadLiveNowNext: suspend (ContentItem) -> Result<LiveNowNextEpg?>
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
    var subtitleDialogState by remember {
        mutableStateOf<SubtitleDialogState>(SubtitleDialogState.Idle)
    }
    var audioBoostDb by remember { mutableStateOf(playbackEngine.getAudioBoostDb()) }
    val subtitleCoroutineScope = rememberCoroutineScope()
    var subtitlesEnabled by remember {
        mutableStateOf(isTextTrackEnabled(player.trackSelectionParameters))
    }
    var hasEmbeddedSubtitles by remember { mutableStateOf(false) }
    var activeSubtitle by remember { mutableStateOf<ActiveSubtitle?>(null, policy = neverEqualPolicy()) }
    var hasCachedSubtitle by remember { mutableStateOf(false) }
    var lastAppliedOffsetMs by remember { mutableStateOf(0L) }

    // Subtitle timing adjustment state
    var subtitleOffsetMs by remember { mutableStateOf(0L) }
    var showSubtitleTimingDialog by remember { mutableStateOf(false) }
    var offsetApplyJob by remember { mutableStateOf<Job?>(null) }
    val hasModalOpen =
            showPlaybackSettingsDialog ||
                    showResolutionDialog ||
                    showAudioTrackDialog ||
                    showAudioBoostDialog ||
                    showPlaybackSpeedDialog ||
                    showSubtitleDialog ||
                    showSubtitleOptionsDialog
    val isPlayerModalOpen = hasModalOpen || showSubtitleTimingDialog

    // Next episode overlay state
    var showNextEpisodeOverlay by remember { mutableStateOf(false) }
    var countdownRemaining by remember { mutableIntStateOf(15) }
    var shouldAutoPlayNext by remember { mutableStateOf(false) }
    var liveNowNextEpg by remember { mutableStateOf<LiveNowNextEpg?>(null) }
    val lastGoodLiveEpgByStream = remember { mutableStateMapOf<String, LiveNowNextEpg>() }
    var liveEpgGeneration by remember { mutableIntStateOf(0) }
    val nowPlayingInfoLabel = remember(title, activePlaybackItem, currentContentType, liveNowNextEpg) {
        buildNowPlayingInfoText(
            title = title,
            activeItem = activePlaybackItem,
            contentType = currentContentType,
            liveNowNextEpg = liveNowNextEpg
        )
    }

    LaunchedEffect(currentContentType, activePlaybackItem?.streamId) {
        val generation = liveEpgGeneration + 1
        liveEpgGeneration = generation
        if (currentContentType != ContentType.LIVE) {
            liveNowNextEpg = null
            return@LaunchedEffect
        }
        val liveItem = activePlaybackItem ?: run {
            // During live channel transitions, clear stale EPG until the next item resolves.
            liveNowNextEpg = null
            return@LaunchedEffect
        }
        val streamId = liveItem.streamId.takeIf { it.isNotBlank() } ?: run {
            liveNowNextEpg = null
            return@LaunchedEffect
        }
        delay(LIVE_EPG_CHANNEL_CHANGE_DEBOUNCE_MS)
        while (true) {
            val result =
                runCatching { loadLiveNowNext(liveItem) }
                    .getOrElse { error -> Result.failure(error) }
            if (generation != liveEpgGeneration) {
                return@LaunchedEffect
            }
            result
                .onSuccess { epg ->
                    val hasProgram =
                        epg?.now?.title?.isNotBlank() == true || epg?.next?.title?.isNotBlank() == true
                    if (hasProgram) {
                        liveNowNextEpg = epg
                        epg?.let { lastGoodLiveEpgByStream[streamId] = it }
                    } else {
                        liveNowNextEpg = null
                    }
                }
                .onFailure {
                    liveNowNextEpg = lastGoodLiveEpgByStream[streamId]
                }
            delay(LIVE_EPG_REFRESH_INTERVAL_MS)
        }
    }

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

    suspend fun loadSubtitleContent(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val path = uri.path ?: return@withContext null
        val file = java.io.File(path)
        if (!file.exists()) return@withContext null
        file.readBytes()
    }

    suspend fun applyOffsetToSubtitle(subtitle: ActiveSubtitle, offsetMs: Long) {
        val format = subtitleFormat(subtitle.fileName)
        if (format == SubtitleFormat.UNKNOWN) return
        val original = subtitle.originalContent ?: return
        val adjustedContent = applySubtitleOffset(original, offsetMs, format)
        val path = subtitle.uri.path ?: return
        withContext(Dispatchers.IO) {
            java.io.File(path).writeBytes(adjustedContent)
        }
        playbackEngine.addSubtitle(
            subtitleUri = subtitle.uri,
            language = subtitle.language,
            label = subtitle.label,
            mimeType = subtitleMimeType(subtitle.fileName)
        )
        lastAppliedOffsetMs = offsetMs
    }

    fun scheduleOffsetApply(newOffset: Long) {
        val subtitle = activeSubtitle ?: return
        if (subtitle.originalContent == null || !isOffsetSupported(subtitle.fileName)) return
        if (newOffset == lastAppliedOffsetMs) return
        offsetApplyJob?.cancel()
        offsetApplyJob = subtitleCoroutineScope.launch {
            delay(2000)
            applyOffsetToSubtitle(subtitle, newOffset)
        }
    }

    fun applyOffsetNow(newOffset: Long) {
        val subtitle = activeSubtitle ?: return
        if (subtitle.originalContent == null || !isOffsetSupported(subtitle.fileName)) return
        offsetApplyJob?.cancel()
        offsetApplyJob = subtitleCoroutineScope.launch {
            applyOffsetToSubtitle(subtitle, newOffset)
        }
    }

    LaunchedEffect(mediaId) {
        activeSubtitle =
                if (mediaId.isBlank()) {
                    null
                } else {
                    withContext(Dispatchers.IO) {
                        subtitleRepository.getCachedSubtitlesForMedia(mediaId)
                    }.firstOrNull()?.let {
                            cached ->
                        val originalContent = loadSubtitleContent(cached.uri)
                        ActiveSubtitle(
                                uri = cached.uri,
                                language = cached.language,
                                label = "Cached ${cached.language.uppercase()}",
                                fileName = cached.fileName,
                                originalContent = originalContent
                        )
                    }
                }
        subtitleOffsetMs = 0L
        lastAppliedOffsetMs = 0L
        hasCachedSubtitle =
                if (mediaId.isBlank()) {
                    false
                } else {
                    withContext(Dispatchers.IO) {
                        subtitleRepository.getCachedSubtitlesForMedia(mediaId).isNotEmpty()
                    }
                }
    }

    LaunchedEffect(showSubtitleOptionsDialog, mediaId) {
        if (showSubtitleOptionsDialog && activeSubtitle == null && mediaId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                subtitleRepository.getCachedSubtitlesForMedia(mediaId)
            }.firstOrNull()?.let { cached ->
                val originalContent = loadSubtitleContent(cached.uri)
                activeSubtitle =
                    ActiveSubtitle(
                        uri = cached.uri,
                        language = cached.language,
                        label = "Cached ${cached.language.uppercase()}",
                        fileName = cached.fileName,
                        originalContent = originalContent
                    )
            }
            hasCachedSubtitle =
                    withContext(Dispatchers.IO) {
                        subtitleRepository.getCachedSubtitlesForMedia(mediaId).isNotEmpty()
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
            showSubtitleTimingDialog -> showSubtitleTimingDialog = false
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

    val openSubtitleTimingDialog: () -> Unit = {
        subtitleCoroutineScope.launch {
            val subtitle =
                activeSubtitle ?: withContext(Dispatchers.IO) {
                    subtitleRepository.getCachedSubtitlesForMedia(mediaId)
                }.firstOrNull()?.let { cached ->
                    ActiveSubtitle(
                        uri = cached.uri,
                        language = cached.language,
                        label = "Cached ${cached.language.uppercase()}",
                        fileName = cached.fileName,
                        originalContent = null
                    )
                }

            if (subtitle == null) {
                Toast.makeText(
                    context,
                    "No subtitle available for timing adjustment",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            if (!isOffsetSupported(subtitle.fileName)) {
                Toast.makeText(
                    context,
                    "Subtitle format not supported for timing adjustment",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val withContent =
                if (subtitle.originalContent != null) {
                    subtitle
                } else {
                    val loaded = loadSubtitleContent(subtitle.uri)
                    if (loaded == null) {
                        Toast.makeText(
                            context,
                            "Subtitle file not available for timing adjustment",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    subtitle.copy(originalContent = loaded)
                }
            activeSubtitle = withContent
            showSubtitleDialog = false
            showSubtitleOptionsDialog = false
            showSubtitleTimingDialog = true
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
                        nowPlayingInfoText = nowPlayingInfoLabel
                        onResizeModeClick = { resizeMode = nextResizeMode(resizeMode) }
                        onBackClick = onExit
                        onSubtitleDownloadClick = { showSubtitleDialog = true }
                        onSubtitleToggleClick = { showSubtitleOptionsDialog = true }
                        onSubtitleTimingClick = openSubtitleTimingDialog
                        onAudioTrackClick = { showAudioTrackDialog = true }
                        onAudioBoostClick = { showAudioBoostDialog = true }
                        onSettingsClick = { showPlaybackSettingsDialog = true }
                        isLiveContent = currentContentType == ContentType.LIVE
                        fastSeekEnabled = currentContentType != ContentType.LIVE
                        onToggleControls = {
                            if (isPlayerModalOpen) {
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
                            if (!isPlayerModalOpen && currentContentType == ContentType.LIVE) {
                                onLiveChannelSwitch(1)
                            } else {
                                false
                            }
                        }
                        onChannelDown = {
                            if (!isPlayerModalOpen && currentContentType == ContentType.LIVE) {
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
                    view.nowPlayingInfoText = nowPlayingInfoLabel
                    view.onResizeModeClick = { resizeMode = nextResizeMode(resizeMode) }
                    view.onBackClick = onExit
                    view.onSubtitleDownloadClick = { showSubtitleDialog = true }
                    view.onSubtitleToggleClick = { showSubtitleOptionsDialog = true }
                    view.onSubtitleTimingClick = openSubtitleTimingDialog
                    view.onAudioTrackClick = { showAudioTrackDialog = true }
                    view.onAudioBoostClick = { showAudioBoostDialog = true }
                    view.onSettingsClick = { showPlaybackSettingsDialog = true }
                    view.isLiveContent = currentContentType == ContentType.LIVE
                    view.fastSeekEnabled = currentContentType != ContentType.LIVE
                    view.defaultControllerTimeoutMs = 3000
                    view.onToggleControls = {
                        if (isPlayerModalOpen) {
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
                        if (!isPlayerModalOpen && currentContentType == ContentType.LIVE) {
                            onLiveChannelSwitch(1)
                        } else {
                            false
                        }
                    }
                    view.onChannelDown = {
                        if (!isPlayerModalOpen && currentContentType == ContentType.LIVE) {
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

        // Subtitle offset adjustment overlay with modal backdrop
        if (showSubtitleTimingDialog && activeSubtitle != null && isOffsetSupported(activeSubtitle?.fileName)) {
            SubtitleOffsetDialog(
                offsetMs = subtitleOffsetMs,
                onOffsetChange = { newOffset ->
                    if (newOffset == subtitleOffsetMs) return@SubtitleOffsetDialog
                    subtitleOffsetMs = newOffset
                    scheduleOffsetApply(newOffset)
                },
                onDismiss = {
                    if (subtitleOffsetMs != lastAppliedOffsetMs) {
                        applyOffsetNow(subtitleOffsetMs)
                    }
                    showSubtitleTimingDialog = false
                }
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
                        if (result.isSuccess) {
                            val cachedSubtitle = result.getOrThrow()
                            // Read original content for offset adjustment
                            val originalContent =
                                    withContext(Dispatchers.IO) {
                                        try {
                                            java.io.File(cachedSubtitle.uri.path ?: "").readBytes()
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                            activeSubtitle =
                                    ActiveSubtitle(
                                            uri = cachedSubtitle.uri,
                                            language = cachedSubtitle.language,
                                            label = subtitle.release,
                                            fileName = cachedSubtitle.fileName,
                                            originalContent = originalContent
                                    )
                            subtitleOffsetMs = 0L  // Reset offset for new subtitle
                            val mimeType = subtitleMimeType(cachedSubtitle.fileName)
                            playbackEngine.addSubtitle(
                                    subtitleUri = cachedSubtitle.uri,
                                    language = cachedSubtitle.language,
                                    label = subtitle.release,
                                    mimeType = mimeType
                            )
                            subtitlesEnabled = true
                            showSubtitleDialog = false
                            subtitleDialogState = SubtitleDialogState.Idle
                        } else {
                            val error = result.exceptionOrNull()
                            subtitleDialogState =
                                    SubtitleDialogState.Error(
                                            error?.message ?: "Download failed"
                                    )
                        }
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

private fun buildNowPlayingInfoText(
    title: String,
    activeItem: ContentItem?,
    contentType: ContentType?,
    liveNowNextEpg: LiveNowNextEpg? = null
): String {
    val resolvedTitle = activeItem?.title?.takeIf { it.isNotBlank() } ?: title
    return when (contentType) {
        ContentType.LIVE -> {
            val liveEpgLabel = buildLiveNowNextLabel(resolvedTitle, liveNowNextEpg)
            if (!liveEpgLabel.isNullOrBlank()) {
                liveEpgLabel
            } else {
                val program = activeItem?.subtitle?.takeIf { it.isNotBlank() }
                    ?: activeItem?.description?.takeIf { it.isNotBlank() }
                when {
                    !program.isNullOrBlank() && resolvedTitle.isNotBlank() -> "$resolvedTitle • $program"
                    !program.isNullOrBlank() -> program
                    resolvedTitle.isNotBlank() -> "Live: $resolvedTitle"
                    else -> "Live"
                }
            }
        }
        ContentType.MOVIES -> {
            val parts = mutableListOf<String>()
            if (resolvedTitle.isNotBlank()) {
                parts += resolvedTitle
            }
            activeItem?.duration?.takeIf { it.isNotBlank() }?.let { parts += it }
            activeItem?.rating?.takeIf { it.isNotBlank() }?.let { parts += "Rating $it" }
            activeItem?.subtitle
                ?.takeIf { it.isNotBlank() && !it.equals(resolvedTitle, ignoreCase = true) }
                ?.let { parts += it }
            parts.joinToString(" • ")
        }
        ContentType.SERIES -> {
            val parts = mutableListOf<String>()
            if (resolvedTitle.isNotBlank()) {
                parts += resolvedTitle
            }
            val season = activeItem?.seasonLabel?.takeIf { it.isNotBlank() }
            val episode = activeItem?.episodeNumber?.takeIf { it.isNotBlank() }
            val seasonEpisode = when {
                !season.isNullOrBlank() && !episode.isNullOrBlank() -> "$season • Episode $episode"
                !season.isNullOrBlank() -> season
                !episode.isNullOrBlank() -> "Episode $episode"
                else -> null
            }
            seasonEpisode?.let { parts += it }
            activeItem?.subtitle
                ?.takeIf { it.isNotBlank() && !it.equals(resolvedTitle, ignoreCase = true) }
                ?.let { parts += it }
            parts.joinToString(" • ")
        }
        else -> resolvedTitle
    }.trim()
}

private fun buildLiveNowNextLabel(
    channelName: String,
    liveNowNextEpg: LiveNowNextEpg?
): String? {
    val now = liveNowNextEpg?.now?.takeIf { it.title.isNotBlank() }
    val next = liveNowNextEpg?.next?.takeIf { it.title.isNotBlank() }
    if (now == null && next == null) return null
    val parts = mutableListOf<String>()
    if (channelName.isNotBlank()) {
        parts += channelName
    }
    now?.let {
        parts += "Now: ${formatProgramLabel(it)}"
    }
    next?.let {
        parts += "Next: ${formatProgramLabel(it)}"
    }
    return parts.joinToString(" • ").takeIf { it.isNotBlank() }
}

private fun formatProgramLabel(program: LiveProgramInfo): String {
    val range = formatProgramTimeRange(program.startTimeMs, program.endTimeMs)
    return if (range == null) {
        program.title
    } else {
        "${program.title} ($range)"
    }
}

private fun formatProgramTimeRange(startMs: Long?, endMs: Long?): String? {
    if (startMs == null || endMs == null) return null
    val formatter = LIVE_PROGRAM_TIME_FORMATTER.get() ?: SimpleDateFormat("HH:mm", Locale.getDefault())
    val start = formatter.format(Date(startMs))
    val end = formatter.format(Date(endMs))
    return "$start-$end"
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

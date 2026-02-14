package com.example.xtreamplayer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.xtreamplayer.content.CategoryItem
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
import com.example.xtreamplayer.ui.SubtitleTrackDialog
import com.example.xtreamplayer.ui.VideoResolutionDialog
import com.example.xtreamplayer.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val LIVE_EPG_REFRESH_INTERVAL_MS = 60_000L
private const val LIVE_EPG_CHANNEL_CHANGE_DEBOUNCE_MS = 250L
private const val LIVE_GUIDE_ROW_LOGO_SIZE_DP = 34
private const val LIVE_GUIDE_CATEGORY_BADGE_SIZE_DP = 42
private const val LIVE_GUIDE_MIN_ROWS_VISIBLE = 6
private const val LIVE_GUIDE_MAX_ROWS_VISIBLE = 10
private const val NERD_STATS_REFRESH_INTERVAL_MS = 500L
private val LANGUAGE_PREFIX_REGEX = Regex("^\\s*[A-Z]{2,3}\\s*-\\s*")
private val BRACKET_TEXT_REGEX = Regex("\\[[^\\]]*\\]")
private val PAREN_TEXT_REGEX = Regex("\\([^\\)]*\\)")
private val TRAILING_YEAR_DASH_REGEX = Regex("\\s+-\\s*\\d{4}\\s*$")
private val TRAILING_YEAR_REGEX = Regex("\\s+\\d{4}\\s*$")
private val TRAILING_SEASON_EPISODE_REGEX = Regex("\\s+S\\d+\\s*E\\d+.*$", RegexOption.IGNORE_CASE)
private val MULTI_SPACE_REGEX = Regex("\\s+")
private val LIVE_GUIDE_NON_ALNUM_REGEX = Regex("[^a-z0-9]+")
private val SRT_TIME_PATTERN = Regex("""(\d{2}):(\d{2}):(\d{2}),(\d{3})""")
private val VTT_TIME_PATTERN = Regex("""(?:(\d{2}):)?(\d{2}):(\d{2})[.](\d{3})""")
private val ASS_TIME_PATTERN = Regex("""(\d+):(\d{2}):(\d{2})[.](\d{2})""")
private val LIVE_PROGRAM_TIME_FORMATTER = ThreadLocal.withInitial {
    SimpleDateFormat("HH:mm", Locale.getDefault())
}

private enum class LiveGuideLevel {
    CATEGORIES,
    CHANNELS
}

private data class LiveGuideCategoryVisual(
        val badgeCode: String,
        val badgeFlag: String?,
        val groupCode: String?,
        val detailCode: String?
)

private fun liveGuideCategoryUiKey(category: CategoryItem): String {
    return "${category.id}::${category.name.trim().uppercase(Locale.ROOT)}"
}

private fun parseLiveGuideCategoryVisual(categoryName: String): LiveGuideCategoryVisual {
    val tokens = categoryName.split('|').map { it.trim() }.filter { it.isNotBlank() }
    val groupCode = tokens.firstOrNull()?.uppercase(Locale.ROOT)
    val badgeSource = tokens.getOrNull(1) ?: tokens.firstOrNull().orEmpty()
    val badgeCode = badgeSourceToCode(badgeSource)
    val flagCode = when (badgeCode) {
        "UK" -> "GB"
        else -> badgeCode
    }
    val detailCode = tokens.getOrNull(2)?.uppercase(Locale.ROOT)
    return LiveGuideCategoryVisual(
            badgeCode = badgeCode.ifBlank { "TV" },
            badgeFlag = countryCodeToFlagEmoji(flagCode),
            groupCode = groupCode,
            detailCode = detailCode
    )
}

private fun badgeSourceToCode(source: String): String {
    if (source.isBlank()) return ""
    val words = source.uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z\\s]"), " ")
            .split(MULTI_SPACE_REGEX)
            .filter { it.isNotBlank() }
    if (words.isEmpty()) return ""
    if (words.size >= 2) {
        return "${words[0].first()}${words[1].first()}"
    }
    return words[0].take(2)
}

private fun countryCodeToFlagEmoji(code: String): String? {
    val normalized = code.uppercase(Locale.ROOT)
    if (normalized.length != 2 || normalized.any { it !in 'A'..'Z' }) return null
    val first = normalized[0].code - 'A'.code + 0x1F1E6
    val second = normalized[1].code - 'A'.code + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

private data class ActiveSubtitle(
    val uri: Uri,
    val language: String,
    val label: String,
    val fileName: String?,
    val originalContent: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActiveSubtitle) return false
        return uri == other.uri && language == other.language && label == other.label && fileName == other.fileName
    }
    override fun hashCode(): Int = uri.hashCode()
}

private data class PlayerNerdStats(
    val matchFrameRateEnabled: Boolean,
    val sourceFrameRate: Float?,
    val displayRefreshRate: Float?,
    val frameRateMatched: Boolean?,
    val videoCodec: String?,
    val videoMimeType: String?,
    val videoBitrateKbps: Float?,
    val sourceResolution: String?,
    val outputResolution: String?,
    val audioCodec: String?,
    val audioMimeType: String?,
    val audioBitrateKbps: Float?,
    val audioLanguage: String?,
    val audioChannels: Int?,
    val audioSampleRateHz: Int?,
    val droppedFrames: Int,
    val bufferedPercent: Int,
    val playbackSpeed: Float
)

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

private fun normalizeCategoryId(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }

private fun normalizeLiveGuideText(raw: String): String =
        raw.lowercase(Locale.US)
                .replace(LIVE_GUIDE_NON_ALNUM_REGEX, " ")
                .trim()
                .replace(MULTI_SPACE_REGEX, " ")

private fun tokenizeLiveGuideText(normalized: String): Set<String> =
        normalized.split(' ')
                .asSequence()
                .map { it.trim() }
                .filter { it.length >= 2 }
                .toSet()

private fun inferCategoryIdFromChannelTitles(
        categories: List<CategoryItem>,
        activeChannel: ContentItem?,
        channels: List<ContentItem>
): String? {
    val titles = buildList {
        activeChannel?.title?.takeIf { it.isNotBlank() }?.let(::add)
        channels.asSequence()
                .mapNotNull { it.title.takeIf(String::isNotBlank) }
                .take(12)
                .forEach(::add)
    }
    val normalizedTitles = titles.map(::normalizeLiveGuideText).filter { it.isNotBlank() }
    if (normalizedTitles.isEmpty()) return null
    val titleTokens = normalizedTitles.map(::tokenizeLiveGuideText)
    return categories
            .mapNotNull { category ->
                val categoryId = normalizeCategoryId(category.id) ?: return@mapNotNull null
                val normalizedName = normalizeLiveGuideText(category.name)
                val categoryTokens = tokenizeLiveGuideText(normalizedName)
                if (categoryTokens.isEmpty()) return@mapNotNull null
                val maxTokenOverlap =
                        titleTokens.maxOfOrNull { tokens ->
                            categoryTokens.count { it in tokens }
                        } ?: 0
                val totalTokenOverlap =
                        titleTokens.sumOf { tokens ->
                            categoryTokens.count { it in tokens }
                        }
                val phraseMatches = normalizedTitles.count { title -> title.contains(normalizedName) }
                if (maxTokenOverlap < 2 && phraseMatches == 0) {
                    null
                } else {
                    Quintuple(
                            categoryId = categoryId,
                            phraseMatches = phraseMatches,
                            maxTokenOverlap = maxTokenOverlap,
                            totalTokenOverlap = totalTokenOverlap,
                            tokenCount = categoryTokens.size
                    )
                }
            }
            .maxWithOrNull(
                    compareBy<Quintuple> { it.maxTokenOverlap }
                            .thenBy { it.totalTokenOverlap }
                            .thenBy { it.tokenCount }
                            .thenBy { it.phraseMatches }
            )
            ?.categoryId
}

private data class Quintuple(
        val categoryId: String,
        val phraseMatches: Int,
        val maxTokenOverlap: Int,
        val totalTokenOverlap: Int,
        val tokenCount: Int
)

@OptIn(UnstableApi::class)
@Composable
internal fun PlayerOverlay(
        title: String,
        activePlaybackItem: ContentItem?,
        activePlaybackItems: List<ContentItem>,
        persistedSubtitleState: PlaybackSubtitleState?,
        player: Player,
        playbackEngine: Media3PlaybackEngine,
        subtitleRepository: SubtitleRepository,
        openSubtitlesApiKey: String,
        openSubtitlesUserAgent: String,
        mediaId: String,
        onRequestOpenSubtitlesApiKey: () -> Unit,
        onExit: () -> Unit,
        autoPlayNextEnabled: Boolean,
        matchFrameRateEnabled: Boolean,
        nextEpisodeThresholdSeconds: Int,
        currentContentType: ContentType?,
        nextEpisodeTitle: String?,
        hasNextEpisode: Boolean,
        onPlayNextEpisode: () -> Unit,
        onMatchFrameRateChange: (Boolean) -> Unit,
        onLiveChannelSwitch: (Int) -> Boolean,
        onLiveGuideChannelSelect: (ContentItem, List<ContentItem>) -> Unit,
        onSubtitleStateChanged: (PlaybackSubtitleState?) -> Unit,
        loadLiveNowNext: suspend (ContentItem) -> Result<LiveNowNextEpg?>,
        loadLiveCategories: suspend () -> Result<List<CategoryItem>>,
        loadLiveCategoryChannels: suspend (CategoryItem) -> Result<List<ContentItem>>,
        loadLiveCategoryThumbnail: suspend (CategoryItem) -> Result<String?>
) {
    val context = LocalContext.current
    var controlsVisible by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(PlayerResizeMode.FIT) }
    var playerView by remember { mutableStateOf<XtreamPlayerView?>(null) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleOptionsDialog by remember { mutableStateOf(false) }
    var showSubtitleTrackDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showAudioBoostDialog by remember { mutableStateOf(false) }
    var showPlaybackSettingsDialog by remember { mutableStateOf(false) }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showNerdStats by remember { mutableStateOf(false) }
    var nerdStats by remember { mutableStateOf<PlayerNerdStats?>(null) }
    var droppedVideoFrames by remember { mutableIntStateOf(0) }
    var subtitleDialogState by remember {
        mutableStateOf<SubtitleDialogState>(SubtitleDialogState.Idle)
    }
    var audioBoostDb by remember { mutableFloatStateOf(playbackEngine.getAudioBoostDb()) }
    val subtitleCoroutineScope = rememberCoroutineScope()
    var subtitlesEnabled by remember {
        mutableStateOf(isTextTrackEnabled(player.trackSelectionParameters))
    }
    var hasEmbeddedSubtitles by remember { mutableStateOf(false) }
    var activeSubtitle by remember { mutableStateOf<ActiveSubtitle?>(null, policy = neverEqualPolicy()) }
    var hasCachedSubtitle by remember { mutableStateOf(false) }
    var lastAppliedOffsetMs by remember { mutableLongStateOf(0L) }

    // Subtitle timing adjustment state
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }
    var showSubtitleTimingDialog by remember { mutableStateOf(false) }
    var offsetApplyJob by remember { mutableStateOf<Job?>(null) }
    val hasModalOpen =
            showPlaybackSettingsDialog ||
                    showResolutionDialog ||
                    showAudioTrackDialog ||
                    showAudioBoostDialog ||
                    showPlaybackSpeedDialog ||
                    showSubtitleDialog ||
                    showSubtitleOptionsDialog ||
                    showSubtitleTrackDialog
    val isPlayerModalOpen = hasModalOpen || showSubtitleTimingDialog
    var showLiveGuide by remember { mutableStateOf(false) }
    var liveGuideLevel by remember { mutableStateOf(LiveGuideLevel.CHANNELS) }
    var liveGuideSelectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var liveGuideCategories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var liveGuideChannels by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var liveGuideLoading by remember { mutableStateOf(false) }
    var liveGuideErrorMessage by remember { mutableStateOf<String?>(null) }
    var liveGuideCategoryIndex by remember { mutableIntStateOf(0) }
    var liveGuideChannelIndex by remember { mutableIntStateOf(0) }
    var liveGuideReturnCategoryAtTop by remember { mutableStateOf(false) }
    var liveGuidePreferredCategoryId by remember { mutableStateOf<String?>(null) }
    var liveGuideParentCategoryId by remember { mutableStateOf<String?>(null) }
    val liveGuideCategoryThumbnails = remember { mutableStateMapOf<String, String?>() }
    val liveGuideCategoryThumbnailRequested = remember { mutableStateMapOf<String, Boolean>() }

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
    val liveQueueItems = remember(activePlaybackItems) {
        activePlaybackItems.filter { it.contentType == ContentType.LIVE }
    }
    fun toPlaybackSubtitleState(subtitle: ActiveSubtitle?, offsetMs: Long): PlaybackSubtitleState? {
        val safeSubtitle = subtitle ?: return null
        val fileName = safeSubtitle.fileName?.takeUnless { it.isBlank() } ?: return null
        return PlaybackSubtitleState(
            fileName = fileName,
            language = safeSubtitle.language.takeUnless { it.isBlank() },
            label = safeSubtitle.label.takeUnless { it.isBlank() },
            offsetMs = offsetMs
        )
    }
    fun publishSubtitleState(subtitle: ActiveSubtitle?, offsetMs: Long) {
        onSubtitleStateChanged(toPlaybackSubtitleState(subtitle, offsetMs))
    }

    val closeLiveGuide: () -> Unit = {
        showLiveGuide = false
        liveGuideErrorMessage = null
        playerView?.let { view ->
            view.post {
                view.useController = true
                view.showController()
                view.focusPlayPause()
            }
        }
    }
    fun wrapIndex(index: Int, size: Int): Int {
        if (size <= 0) return 0
        return ((index % size) + size) % size
    }

    fun resolveCategoryIndexById(
            categories: List<CategoryItem>,
            categoryId: String?
    ): Int? {
        val normalizedId = normalizeCategoryId(categoryId) ?: return null
        return categories.indexOfFirst { normalizeCategoryId(it.id) == normalizedId }
                .takeIf { it >= 0 }
    }

    fun ensureLiveGuideCategoryThumbnail(category: CategoryItem) {
        val key = liveGuideCategoryUiKey(category)
        if (liveGuideCategoryThumbnailRequested[key] == true) return
        liveGuideCategoryThumbnailRequested[key] = true
        subtitleCoroutineScope.launch {
            val result =
                    runCatching { loadLiveCategoryThumbnail(category) }
                            .getOrElse { Result.failure(it) }
            if (result.isSuccess) {
                liveGuideCategoryThumbnails[key] = result.getOrNull()?.takeIf { it.isNotBlank() }
            } else {
                // Allow retry later on transient failures.
                liveGuideCategoryThumbnailRequested[key] = false
            }
        }
    }

    fun refreshLiveCategories() {
        subtitleCoroutineScope.launch {
            liveGuideLoading = true
            liveGuideErrorMessage = null
            val result = runCatching { loadLiveCategories() }.getOrElse { Result.failure(it) }
            result
                    .onSuccess { categories ->
                        liveGuideCategories = categories
                        val validKeys = categories.map(::liveGuideCategoryUiKey).toSet()
                        liveGuideCategoryThumbnails.keys.retainAll(validKeys)
                        liveGuideCategoryThumbnailRequested.keys.retainAll(validKeys)
                        categories.take(12).forEach(::ensureLiveGuideCategoryThumbnail)
                        val preferredCategoryId =
                                normalizeCategoryId(liveGuidePreferredCategoryId)
                                        ?: normalizeCategoryId(liveGuideParentCategoryId)
                                        ?: inferCategoryIdFromChannelTitles(
                                                categories = categories,
                                                activeChannel = activePlaybackItem,
                                                channels = liveGuideChannels
                                        )
                        val preferredIndex = resolveCategoryIndexById(categories, preferredCategoryId)
                        if (preferredIndex != null) {
                            liveGuideCategoryIndex = preferredIndex
                            liveGuideSelectedCategory = categories.getOrNull(preferredIndex)
                            liveGuidePreferredCategoryId =
                                    normalizeCategoryId(categories.getOrNull(preferredIndex)?.id)
                            liveGuideParentCategoryId = liveGuidePreferredCategoryId
                        } else {
                            liveGuideCategoryIndex =
                                    resolveCategoryIndexById(categories, liveGuideSelectedCategory?.id)
                                            ?: wrapIndex(liveGuideCategoryIndex, categories.size)
                        }
                    }
                    .onFailure { error ->
                        liveGuideErrorMessage = error.message ?: "Failed to load categories"
                    }
            liveGuideLoading = false
        }
    }

    fun openCategoryChannels(category: CategoryItem) {
        subtitleCoroutineScope.launch {
            liveGuideLoading = true
            liveGuideErrorMessage = null
            val result = runCatching { loadLiveCategoryChannels(category) }.getOrElse { Result.failure(it) }
            result
                    .onSuccess { channels ->
                        val liveChannels = channels.filter { it.contentType == ContentType.LIVE }
                        val liveChannelsInCategory =
                                liveChannels.filter {
                                    normalizeCategoryId(it.categoryId) == normalizeCategoryId(category.id)
                                }
                        val resolvedChannels =
                                if (liveChannelsInCategory.isNotEmpty()) {
                                    liveChannelsInCategory
                                } else {
                                    liveChannels
                                }
                        val normalizedCategoryId = normalizeCategoryId(category.id)
                        liveGuidePreferredCategoryId = normalizedCategoryId
                        liveGuideParentCategoryId = normalizedCategoryId
                        liveGuideSelectedCategory = category
                        liveGuideCategoryIndex =
                                resolveCategoryIndexById(liveGuideCategories, normalizedCategoryId)
                                        ?: liveGuideCategoryIndex
                        liveGuideChannels = resolvedChannels
                        // Always start at the top of a category channel list.
                        liveGuideChannelIndex = 0
                        liveGuideLevel = LiveGuideLevel.CHANNELS
                    }
                    .onFailure { error ->
                        liveGuideErrorMessage = error.message ?: "Failed to load channels"
                    }
            liveGuideLoading = false
        }
    }

    fun showLiveGuidePanel() {
        if (currentContentType != ContentType.LIVE || isPlayerModalOpen) {
            return
        }
        val queueCategoryId =
                normalizeCategoryId(activePlaybackItem?.categoryId)
                        ?: liveQueueItems
                                .mapNotNull { normalizeCategoryId(it.categoryId) }
                                .groupingBy { it }
                                .eachCount()
                                .maxByOrNull { it.value }
                                ?.key
                        ?: inferCategoryIdFromChannelTitles(
                                categories = liveGuideCategories,
                                activeChannel = activePlaybackItem,
                                channels = liveQueueItems
                        )
                        ?: normalizeCategoryId(liveGuideParentCategoryId)
                        ?: normalizeCategoryId(liveGuidePreferredCategoryId)
        queueCategoryId?.let { categoryId ->
            liveGuidePreferredCategoryId = categoryId
            liveGuideParentCategoryId = categoryId
            val preferredIndex = resolveCategoryIndexById(liveGuideCategories, categoryId)
            if (preferredIndex != null) {
                liveGuideCategoryIndex = preferredIndex
                liveGuideSelectedCategory = liveGuideCategories[preferredIndex]
            }
        }
        if (liveQueueItems.size > 1) {
            liveGuideChannels = liveQueueItems
            liveGuideLevel = LiveGuideLevel.CHANNELS
            liveGuideChannelIndex =
                    liveQueueItems.indexOfFirst {
                        it.id == activePlaybackItem?.id
                    }.takeIf { it >= 0 } ?: 0
        } else {
            liveGuideLevel = LiveGuideLevel.CATEGORIES
        }
        showLiveGuide = true
        playerView?.hideController()
        playerView?.resetControllerFocus()
        playerView?.requestFocus()
        if (liveGuideCategories.isEmpty()) {
            refreshLiveCategories()
        } else {
            liveGuideCategories.take(12).forEach(::ensureLiveGuideCategoryThumbnail)
        }
    }

    fun navigateLiveGuideBackOneLevel() {
        if (liveGuideLevel == LiveGuideLevel.CHANNELS) {
            playerView?.hideController()
            liveGuideLevel = LiveGuideLevel.CATEGORIES
            liveGuideReturnCategoryAtTop = true
            val preferredCategoryId =
                    normalizeCategoryId(liveGuideParentCategoryId)
                            ?: normalizeCategoryId(liveGuideSelectedCategory?.id)
                            ?: normalizeCategoryId(liveGuidePreferredCategoryId)
                            ?: normalizeCategoryId(activePlaybackItem?.categoryId)
                            ?: inferCategoryIdFromChannelTitles(
                                    categories = liveGuideCategories,
                                    activeChannel = activePlaybackItem,
                                    channels = liveGuideChannels
                            )
            liveGuideCategoryIndex =
                    resolveCategoryIndexById(liveGuideCategories, preferredCategoryId)
                            ?: wrapIndex(liveGuideCategoryIndex, liveGuideCategories.size)
            liveGuideSelectedCategory = liveGuideCategories.getOrNull(liveGuideCategoryIndex)
            if (liveGuideCategories.isEmpty() && !liveGuideLoading) {
                refreshLiveCategories()
            }
        }
    }

    fun moveLiveGuideSelection(delta: Int) {
        if (liveGuideLoading || !liveGuideErrorMessage.isNullOrBlank()) return
        playerView?.hideController()
        if (liveGuideLevel == LiveGuideLevel.CATEGORIES) {
            if (liveGuideCategories.isEmpty()) return
            liveGuideCategoryIndex = wrapIndex(liveGuideCategoryIndex + delta, liveGuideCategories.size)
        } else {
            if (liveGuideChannels.isEmpty()) return
            liveGuideChannelIndex = wrapIndex(liveGuideChannelIndex + delta, liveGuideChannels.size)
        }
    }

    fun retryLiveGuideLoad() {
        val selectedCategory = liveGuideSelectedCategory
        if (liveGuideLevel == LiveGuideLevel.CHANNELS && selectedCategory != null) {
            openCategoryChannels(selectedCategory)
        } else {
            refreshLiveCategories()
        }
    }

    fun activateLiveGuideSelection() {
        if (liveGuideLoading) return
        playerView?.hideController()
        if (!liveGuideErrorMessage.isNullOrBlank()) {
            retryLiveGuideLoad()
            return
        }
        if (liveGuideLevel == LiveGuideLevel.CATEGORIES) {
            val category = liveGuideCategories.getOrNull(liveGuideCategoryIndex) ?: return
            openCategoryChannels(category)
            return
        }
        val channel = liveGuideChannels.getOrNull(liveGuideChannelIndex) ?: return
        if (channel.id == activePlaybackItem?.id) {
            closeLiveGuide()
            return
        }
        onLiveGuideChannelSelect(channel, liveGuideChannels)
    }

    LaunchedEffect(currentContentType) {
        if (currentContentType != ContentType.LIVE) {
            showLiveGuide = false
        }
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
        val deltaMs = offsetMs - lastAppliedOffsetMs
        if (deltaMs == 0L) {
            lastAppliedOffsetMs = offsetMs
            publishSubtitleState(subtitle, offsetMs)
            return
        }
        val adjustedContent = applySubtitleOffset(original, deltaMs, format)
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
        val updatedSubtitle =
            subtitle.copy(
                originalContent = adjustedContent
            )
        activeSubtitle = updatedSubtitle
        lastAppliedOffsetMs = offsetMs
        publishSubtitleState(updatedSubtitle, offsetMs)
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

    LaunchedEffect(mediaId, persistedSubtitleState?.fileName) {
        if (mediaId.isBlank()) {
            activeSubtitle = null
            subtitleOffsetMs = 0L
            lastAppliedOffsetMs = 0L
            hasCachedSubtitle = false
            onSubtitleStateChanged(null)
            return@LaunchedEffect
        }

        val cachedSubtitles = withContext(Dispatchers.IO) {
            subtitleRepository.getCachedSubtitlesForMedia(mediaId)
        }
        hasCachedSubtitle = cachedSubtitles.isNotEmpty()
        val preferredSubtitle =
            persistedSubtitleState?.fileName?.let { preferredFileName ->
                cachedSubtitles.firstOrNull { it.fileName == preferredFileName }
            }
        val selectedSubtitle = preferredSubtitle ?: cachedSubtitles.firstOrNull()
        val restoredOffsetMs = if (preferredSubtitle != null) {
            persistedSubtitleState?.offsetMs ?: 0L
        } else {
            0L
        }

        activeSubtitle = selectedSubtitle?.let { cached ->
            val originalContent = loadSubtitleContent(cached.uri)
            ActiveSubtitle(
                uri = cached.uri,
                language = if (preferredSubtitle != null) {
                    persistedSubtitleState?.language?.takeUnless { it.isBlank() } ?: cached.language
                } else {
                    cached.language
                },
                label = if (preferredSubtitle != null) {
                    persistedSubtitleState?.label?.takeUnless { it.isBlank() }
                        ?: "Cached ${cached.language.uppercase()}"
                } else {
                    "Cached ${cached.language.uppercase()}"
                },
                fileName = cached.fileName,
                originalContent = originalContent
            )
        }
        subtitleOffsetMs = restoredOffsetMs
        lastAppliedOffsetMs = restoredOffsetMs

        val subtitleToRestore = activeSubtitle
        if (preferredSubtitle != null && subtitleToRestore != null) {
            playbackEngine.addSubtitle(
                subtitleUri = subtitleToRestore.uri,
                language = subtitleToRestore.language,
                label = subtitleToRestore.label,
                mimeType = subtitleMimeType(subtitleToRestore.fileName)
            )
            playbackEngine.setSubtitlesEnabled(true)
            publishSubtitleState(subtitleToRestore, restoredOffsetMs)
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

    val latestMediaId by rememberUpdatedState(mediaId)
    val toggleSubtitles: () -> Unit = {
        subtitleCoroutineScope.launch {
            if (subtitlesEnabled) {
                // Keep loaded subtitle tracks in the MediaItem; only disable rendering.
                playbackEngine.setSubtitlesEnabled(false)
                return@launch
            }

            if (hasExternalSubtitles(player)) {
                playbackEngine.setSubtitlesEnabled(true)
                return@launch
            }

            if (hasEmbeddedTextTracks(player)) {
                playbackEngine.setSubtitlesEnabled(true)
                return@launch
            }

            val requestedMediaId = mediaId
            val requestedPlayerMediaId = player.currentMediaItem?.mediaId
            val fallbackSubtitle =
                    if (requestedMediaId.isBlank()) {
                        null
                    } else {
                        withContext(Dispatchers.IO) {
                            subtitleRepository
                                    .getCachedSubtitlesForMedia(requestedMediaId)
                                    .firstOrNull()
                                    ?.let { cached ->
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
                    }

            if (
                requestedMediaId != latestMediaId ||
                        requestedPlayerMediaId != player.currentMediaItem?.mediaId
            ) {
                return@launch
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
                publishSubtitleState(subtitleToApply, subtitleOffsetMs)
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

    LaunchedEffect(title) {
        if (currentContentType == ContentType.LIVE) {
            playerView?.showController()
        }
    }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            playerView?.resetControllerFocus()
            playerView?.requestFocus()
        }
    }

    LaunchedEffect(showAudioBoostDialog) {
        if (showAudioBoostDialog) {
            audioBoostDb = playbackEngine.getAudioBoostDb()
        }
    }

    DisposableEffect(player, showNerdStats) {
        val exoPlayer = player as? ExoPlayer
        if (!showNerdStats || exoPlayer == null) {
            onDispose {}
        } else {
            droppedVideoFrames = 0
            val listener =
                object : AnalyticsListener {
                    override fun onDroppedVideoFrames(
                        eventTime: AnalyticsListener.EventTime,
                        droppedFrameCount: Int,
                        elapsedMs: Long
                    ) {
                        droppedVideoFrames += droppedFrameCount
                    }
                }
            exoPlayer.addAnalyticsListener(listener)
            onDispose {
                exoPlayer.removeAnalyticsListener(listener)
            }
        }
    }

    LaunchedEffect(showNerdStats, matchFrameRateEnabled, droppedVideoFrames, player, context) {
        if (!showNerdStats) {
            nerdStats = null
            return@LaunchedEffect
        }
        while (showNerdStats) {
            nerdStats = collectPlayerNerdStats(context, player, matchFrameRateEnabled, droppedVideoFrames)
            delay(NERD_STATS_REFRESH_INTERVAL_MS)
        }
    }

    BackHandler(enabled = true) {
        when {
            showLiveGuide -> closeLiveGuide()
            showSubtitleTimingDialog -> showSubtitleTimingDialog = false
            showSubtitleTrackDialog -> showSubtitleTrackDialog = false
            showPlaybackSettingsDialog -> showPlaybackSettingsDialog = false
            showPlaybackSpeedDialog -> showPlaybackSpeedDialog = false
            showResolutionDialog -> showResolutionDialog = false
            else -> {
                if (playerView?.isControllerFullyVisible == true) {
                    playerView?.hideController()
                    return@BackHandler
                }
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

    val bindMutablePlayerCallbacks: (XtreamPlayerView) -> Unit = { view ->
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
            if (showLiveGuide || isPlayerModalOpen) {
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
        view.onOpenLiveGuide = {
            if (!isPlayerModalOpen && currentContentType == ContentType.LIVE) {
                showLiveGuidePanel()
                true
            } else {
                false
            }
        }
        view.isLiveGuideOpen = showLiveGuide
        view.onLiveGuideMove = {
            moveLiveGuideSelection(it)
            true
        }
        view.onLiveGuideSelect = {
            activateLiveGuideSelection()
            true
        }
        view.onLiveGuideBack = {
            navigateLiveGuideBackOneLevel()
            true
        }
        view.onLiveGuideDismiss = {
            closeLiveGuide()
            true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    XtreamPlayerView(context).apply {
                        this.player = player
                        useController = !showLiveGuide
                        controllerAutoShow = false
                        setControllerShowTimeoutMs(3000)
                        defaultControllerTimeoutMs = 3000
                        setShutterBackgroundColor(AndroidColor.BLACK)
                        setBackgroundColor(AndroidColor.BLACK)
                        bindMutablePlayerCallbacks(this)
                        isFocusable = true
                        isFocusableInTouchMode = true
                        requestFocus()
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
                                if (showLiveGuide) {
                                    closeLiveGuide()
                                    return@setOnKeyListener true
                                }
                                if (isControllerFullyVisible()) {
                                    hideController()
                                    return@setOnKeyListener true
                                }
                                val dismissed = dismissSettingsWindowIfShowing()
                                if (!dismissed) {
                                    onExit()
                                }
                                return@setOnKeyListener true
                            }
                            if (showLiveGuide) {
                                return@setOnKeyListener true
                            }
                            false
                        }
                        playerView = this
                    }
                },
                update = { view ->
                    view.player = player
                    view.useController = !showLiveGuide
                    if (showLiveGuide) {
                        view.hideController()
                    }
                    bindMutablePlayerCallbacks(view)
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

        if (showNerdStats) {
            PlayerNerdStatsPanel(
                stats = nerdStats,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 64.dp)
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
                    showSubtitleTimingDialog = false
                    if (subtitleOffsetMs != lastAppliedOffsetMs) {
                        applyOffsetNow(subtitleOffsetMs)
                    }
                }
            )
        }

        if (showLiveGuide && currentContentType == ContentType.LIVE) {
            LiveGuidePanel(
                    level = liveGuideLevel,
                    categories = liveGuideCategories,
                    channels = liveGuideChannels,
                    selectedCategoryIndex = liveGuideCategoryIndex,
                    selectedChannelIndex = liveGuideChannelIndex,
                    activeChannelId = activePlaybackItem?.id,
                    isLoading = liveGuideLoading,
                    errorMessage = liveGuideErrorMessage,
                    selectedCategoryName = liveGuideSelectedCategory?.name,
                    alignCategorySelectionToTop = liveGuideReturnCategoryAtTop,
                    onCategorySelectionAligned = { liveGuideReturnCategoryAtTop = false },
                    categoryThumbnailProvider = { category ->
                        liveGuideCategoryThumbnails[liveGuideCategoryUiKey(category)]
                    },
                    onCategoryThumbnailNeeded = { category ->
                        ensureLiveGuideCategoryThumbnail(category)
                    }
            )
        }

    }


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
                            lastAppliedOffsetMs = 0L
                            val mimeType = subtitleMimeType(cachedSubtitle.fileName)
                            playbackEngine.addSubtitle(
                                    subtitleUri = cachedSubtitle.uri,
                                    language = cachedSubtitle.language,
                                    label = subtitle.release,
                                    mimeType = mimeType
                            )
                            publishSubtitleState(activeSubtitle, 0L)
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
                onSelectSubtitleTrack = {
                    showSubtitleDialog = false
                    showSubtitleTrackDialog = true
                },
                onDismiss = {
                    showSubtitleDialog = false
                    subtitleDialogState = SubtitleDialogState.Idle
                }
        )
    }

    if (showSubtitleOptionsDialog) {
        val availableSubtitleTracks = playbackEngine.getAvailableSubtitleTracks()
        val canSelectSubtitleTrack = availableSubtitleTracks.count { it.isSupported } > 1
        SubtitleOptionsDialog(
                subtitlesEnabled = subtitlesEnabled,
                showSubtitleTrackOption = canSelectSubtitleTrack,
                onToggleSubtitles = {
                    toggleSubtitles()
                    showSubtitleOptionsDialog = false
                },
                onSelectSubtitleTrack = {
                    showSubtitleOptionsDialog = false
                    showSubtitleTrackDialog = true
                },
                onDownloadSubtitles = {
                    showSubtitleOptionsDialog = false
                    showSubtitleDialog = true
                },
                onDismiss = { showSubtitleOptionsDialog = false }
        )
    }

    if (showSubtitleTrackDialog) {
        SubtitleTrackDialog(
                availableTracks = playbackEngine.getAvailableSubtitleTracks(),
                onTrackSelected = { groupIndex, trackIndex ->
                    playbackEngine.selectSubtitleTrack(groupIndex, trackIndex)
                },
                onDismiss = { showSubtitleTrackDialog = false }
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
        val matchFrameRateLabel = if (matchFrameRateEnabled) "On" else "Off"
        val nerdStatsLabel = if (showNerdStats) "On" else "Off"

        PlaybackSettingsDialog(
                audioLabel = selectedAudio,
                speedLabel = speedLabel,
                resolutionLabel = resolutionLabel,
                matchFrameRateLabel = matchFrameRateLabel,
                nerdStatsLabel = nerdStatsLabel,
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
                onToggleMatchFrameRate = { onMatchFrameRateChange(!matchFrameRateEnabled) },
                onToggleNerdStats = { showNerdStats = !showNerdStats },
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

@Composable
private fun LiveGuidePanel(
        level: LiveGuideLevel,
        categories: List<CategoryItem>,
        channels: List<ContentItem>,
        selectedCategoryIndex: Int,
        selectedChannelIndex: Int,
        activeChannelId: String?,
        isLoading: Boolean,
        errorMessage: String?,
        selectedCategoryName: String?,
        alignCategorySelectionToTop: Boolean,
        onCategorySelectionAligned: () -> Unit,
        categoryThumbnailProvider: (CategoryItem) -> String?,
        onCategoryThumbnailNeeded: (CategoryItem) -> Unit
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val hasRows = if (level == LiveGuideLevel.CATEGORIES) categories.isNotEmpty() else channels.isNotEmpty()
    val selectedIndex = if (level == LiveGuideLevel.CATEGORIES) selectedCategoryIndex else selectedChannelIndex
    val selectedBoundedIndex = if (hasRows) selectedIndex.coerceAtLeast(0) else 0
    val visibleWindow by remember(listState, selectedBoundedIndex) {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val viewportStart = listState.layoutInfo.viewportStartOffset
            val viewportEnd = listState.layoutInfo.viewportEndOffset
            val fullyVisibleItems =
                    visibleItems.filter { visible ->
                        visible.offset >= viewportStart && visible.offset + visible.size <= viewportEnd
                    }
            val effectiveVisibleItems =
                    if (fullyVisibleItems.isNotEmpty()) fullyVisibleItems else visibleItems
            val firstVisibleIndex = effectiveVisibleItems.firstOrNull()?.index ?: selectedBoundedIndex
            val lastVisibleIndex = effectiveVisibleItems.lastOrNull()?.index ?: selectedBoundedIndex
            val selectedDisplayIndex = selectedBoundedIndex.coerceIn(firstVisibleIndex, lastVisibleIndex)
            Triple(firstVisibleIndex, lastVisibleIndex, selectedDisplayIndex)
        }
    }
    val firstVisibleIndex = visibleWindow.first
    val lastVisibleIndex = visibleWindow.second
    val selectedDisplayIndex = visibleWindow.third
    val rowHeight = 62.dp
    val rowSpacing = 4.dp
    val headerHeight = 26.dp

    LaunchedEffect(
            level,
            selectedBoundedIndex,
            categories.size,
            channels.size,
            alignCategorySelectionToTop
    ) {
        val rowCount = if (level == LiveGuideLevel.CATEGORIES) categories.size else channels.size
        if (rowCount > 0) {
            val targetIndex = selectedBoundedIndex.coerceAtMost(rowCount - 1)
            if (level == LiveGuideLevel.CATEGORIES && alignCategorySelectionToTop) {
                listState.scrollToItem(targetIndex)
                onCategorySelectionAligned()
                return@LaunchedEffect
            }
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                listState.scrollToItem(targetIndex)
                return@LaunchedEffect
            }
            val viewportStart = listState.layoutInfo.viewportStartOffset
            val viewportEnd = listState.layoutInfo.viewportEndOffset
            val fullyVisibleItems =
                    visibleItems.filter { visible ->
                        visible.offset >= viewportStart && visible.offset + visible.size <= viewportEnd
                    }
            val effectiveVisibleItems = if (fullyVisibleItems.isNotEmpty()) fullyVisibleItems else visibleItems
            val firstVisible = effectiveVisibleItems.first().index
            val lastVisible = effectiveVisibleItems.last().index
            when {
                targetIndex < firstVisible -> {
                    listState.scrollToItem(targetIndex)
                }
                targetIndex > lastVisible -> {
                    val visibleCount = effectiveVisibleItems.size.coerceAtLeast(1)
                    val newFirst = (targetIndex - visibleCount + 1).coerceAtLeast(0)
                    listState.scrollToItem(newFirst)
                }
            }
        }
    }

    BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(vertical = 20.dp, horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd
    ) {
        val layoutMetrics =
                remember(maxHeight, rowHeight, rowSpacing, headerHeight, density.density, density.fontScale) {
                    with(density) {
                        val rowPx = rowHeight.roundToPx()
                        val spacingPx = rowSpacing.roundToPx()
                        val chromePx = (headerHeight + 24.dp).roundToPx()
                        val availablePx = (maxHeight.roundToPx() - chromePx).coerceAtLeast(rowPx)
                        val rowsVisible =
                                ((availablePx + spacingPx) / (rowPx + spacingPx))
                                        .coerceIn(LIVE_GUIDE_MIN_ROWS_VISIBLE, LIVE_GUIDE_MAX_ROWS_VISIBLE)
                        val listPx = rowPx * rowsVisible + spacingPx * (rowsVisible - 1)
                        Pair(listPx.toDp(), (listPx + chromePx).toDp())
                    }
                }
        val listHeight = layoutMetrics.first
        val panelHeight = layoutMetrics.second
        Column(
                modifier =
                        Modifier.width(360.dp)
                                .height(panelHeight)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppTheme.colors.overlayStrong.copy(alpha = 0.82f))
                                .border(
                                        width = 1.dp,
                                        color = AppTheme.colors.borderStrong.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth().height(headerHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text =
                                when (level) {
                                    LiveGuideLevel.CATEGORIES -> "Categories"
                                    LiveGuideLevel.CHANNELS ->
                                            selectedCategoryName ?: "Current channel list"
                                },
                        color = AppTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppTheme.fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                )
                Text(
                        text = "LIVE GUIDE",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontFamily = AppTheme.fontFamily,
                        modifier = Modifier.padding(start = 8.dp)
                )
            }
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(listHeight), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.focus)
                    }
                }
                !errorMessage.isNullOrBlank() -> {
                    Column(
                            modifier = Modifier.fillMaxWidth().height(listHeight),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                                text = errorMessage,
                                color = AppTheme.colors.error,
                                fontSize = 13.sp,
                                fontFamily = AppTheme.fontFamily
                        )
                        Text(
                                text = "Press OK to retry",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                fontFamily = AppTheme.fontFamily
                        )
                    }
                }
                !hasRows -> {
                    Box(modifier = Modifier.fillMaxWidth().height(listHeight), contentAlignment = Alignment.Center) {
                        Text(
                                text = "No channels available",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 13.sp,
                                fontFamily = AppTheme.fontFamily
                        )
                    }
                }
                else -> {
                    LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth().height(listHeight),
                            verticalArrangement = Arrangement.spacedBy(rowSpacing)
                    ) {
                        if (level == LiveGuideLevel.CATEGORIES) {
                            itemsIndexed(
                                    items = categories,
                                    key = { _, category -> liveGuideCategoryUiKey(category) }
                            ) { index, category ->
                                LiveGuideCategoryRow(
                                        category = category,
                                        rowIndex = index,
                                        selected = index == selectedDisplayIndex,
                                        thumbnailUrl = categoryThumbnailProvider(category),
                                        onThumbnailNeeded = onCategoryThumbnailNeeded
                                )
                            }
                        } else {
                            itemsIndexed(
                                    items = channels,
                                    key = { _, channel -> channel.id }
                            ) { index, channel ->
                                LiveGuideChannelRow(
                                        channel = channel,
                                        selected = index == selectedDisplayIndex,
                                        active = channel.id == activeChannelId
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
private fun LiveGuideCategoryRow(
        category: CategoryItem,
        rowIndex: Int,
        selected: Boolean,
        thumbnailUrl: String?,
        onThumbnailNeeded: (CategoryItem) -> Unit
) {
    val localContext = LocalContext.current
    val visual = remember(category.id, category.name) { parseLiveGuideCategoryVisual(category.name) }
    val categoryKey = remember(category.id, category.name) { liveGuideCategoryUiKey(category) }
    LaunchedEffect(categoryKey, thumbnailUrl) {
        if (thumbnailUrl.isNullOrBlank()) {
            onThumbnailNeeded(category)
        }
    }
    val shape = RoundedCornerShape(9.dp)
    val borderColor = if (selected) AppTheme.colors.focus else AppTheme.colors.borderStrong.copy(alpha = 0.22f)
    val rowTintAlpha = if (rowIndex % 2 == 0) 0.07f else 0.13f
    val background =
            if (selected) AppTheme.colors.focus.copy(alpha = 0.2f)
            else AppTheme.colors.surfaceAlt.copy(alpha = rowTintAlpha)
    Box(
            modifier = Modifier.fillMaxWidth()
                    .height(62.dp)
                    .clip(shape)
                    .background(background)
                    .border(1.dp, borderColor, shape)
                    .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        if (selected) {
            Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .width(3.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AppTheme.colors.focus.copy(alpha = 0.95f))
            )
        }
        Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val badgeShape = RoundedCornerShape(6.dp)
            val badgeBackground =
                    if (selected) AppTheme.colors.focus.copy(alpha = 0.24f)
                    else AppTheme.colors.surfaceAlt.copy(alpha = 0.72f)
            val badgeBorder =
                    if (selected) AppTheme.colors.focus.copy(alpha = 0.7f)
                    else AppTheme.colors.borderStrong.copy(alpha = 0.6f)
            Box(
                    modifier = Modifier.width(LIVE_GUIDE_CATEGORY_BADGE_SIZE_DP.dp)
                            .height(LIVE_GUIDE_CATEGORY_BADGE_SIZE_DP.dp)
                            .clip(badgeShape)
                            .background(badgeBackground)
                            .border(1.dp, badgeBorder, badgeShape),
                    contentAlignment = Alignment.Center
            ) {
                if (thumbnailUrl.isNullOrBlank()) {
                    val badgeText = "TV"
                    Text(
                            text = badgeText,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppTheme.fontFamily,
                            maxLines = 1
                    )
                } else {
                    val imageRequest =
                            remember(thumbnailUrl) {
                                ImageRequest.Builder(localContext)
                                        .data(thumbnailUrl)
                                        .size(120)
                                        .crossfade(true)
                                        .build()
                            }
                    AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                    )
                }
            }
            Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                        text = category.name,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppTheme.fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val typeLabel =
                            when (category.type) {
                                ContentType.LIVE -> "Live"
                                ContentType.MOVIES -> "Movies"
                                ContentType.SERIES -> "Series"
                            }
                    LiveGuideMetaChip(text = typeLabel, selected = selected)
                    visual.groupCode?.takeIf { it.isNotBlank() }?.let {
                        LiveGuideMetaChip(text = it, selected = selected)
                    }
                    visual.detailCode?.takeIf { it.isNotBlank() }?.let {
                        LiveGuideMetaChip(text = it, selected = selected)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveGuideMetaChip(
        text: String,
        selected: Boolean
) {
    val chipShape = RoundedCornerShape(5.dp)
    val chipBackground =
            if (selected) AppTheme.colors.focus.copy(alpha = 0.18f)
            else AppTheme.colors.surfaceAlt.copy(alpha = 0.55f)
    val chipBorder =
            if (selected) AppTheme.colors.focus.copy(alpha = 0.5f)
            else AppTheme.colors.borderStrong.copy(alpha = 0.4f)
    Box(
            modifier = Modifier.clip(chipShape)
                    .background(chipBackground)
                    .border(1.dp, chipBorder, chipShape)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
                text = text,
                color = AppTheme.colors.textSecondary,
                fontSize = 9.sp,
                fontFamily = AppTheme.fontFamily,
                maxLines = 1
        )
    }
}

@Composable
private fun LiveGuideChannelRow(
        channel: ContentItem,
        selected: Boolean,
        active: Boolean
) {
    val localContext = LocalContext.current
    val shape = RoundedCornerShape(9.dp)
    val borderColor =
            when {
                selected -> AppTheme.colors.focus
                active -> AppTheme.colors.borderStrong
                else -> Color.Transparent
            }
    val background =
            when {
                selected -> AppTheme.colors.focus.copy(alpha = 0.22f)
                active -> AppTheme.colors.surfaceAlt.copy(alpha = 0.78f)
                else -> Color.Transparent
            }
    Row(
            modifier = Modifier.fillMaxWidth()
                    .height(62.dp)
                    .clip(shape)
                    .background(background)
                    .border(1.dp, borderColor, shape)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val logoModifier =
                Modifier.width(LIVE_GUIDE_ROW_LOGO_SIZE_DP.dp)
                        .height(LIVE_GUIDE_ROW_LOGO_SIZE_DP.dp)
                        .clip(RoundedCornerShape(6.dp))
        if (channel.imageUrl.isNullOrBlank()) {
            Box(
                    modifier = logoModifier.background(AppTheme.colors.surfaceAlt.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
            ) {
                Text(
                        text = "#",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = AppTheme.fontFamily
                )
            }
        } else {
            val imageRequest =
                    remember(channel.imageUrl) {
                        ImageRequest.Builder(localContext)
                                .data(channel.imageUrl)
                                .size(120)
                                .crossfade(true)
                                .build()
                    }
            AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = logoModifier.background(AppTheme.colors.surfaceAlt.copy(alpha = 0.35f))
            )
        }
        Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            val channelLabel = channel.streamId.ifBlank { channel.id }
            Text(
                    text = channelLabel,
                    color = AppTheme.colors.textSecondary,
                    fontSize = 10.sp,
                    fontFamily = AppTheme.fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Text(
                    text = channel.title,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 13.sp,
                    fontFamily = AppTheme.fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }
        if (active) {
            Box(
                    modifier =
                            Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(AppTheme.colors.overlay.copy(alpha = 0.48f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                        text = "Playing",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 10.sp,
                        fontFamily = AppTheme.fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }
        }
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

@Composable
private fun PlayerNerdStatsPanel(
    stats: PlayerNerdStats?,
    modifier: Modifier = Modifier
) {
    val lines =
        if (stats == null) {
            listOf("Collecting player stats..." to Color.White)
        } else {
            val fpsText =
                stats.sourceFrameRate?.let { String.format(Locale.US, "%.2f fps", it) } ?: "Unknown"
            val hzText =
                stats.displayRefreshRate?.let { String.format(Locale.US, "%.2f Hz", it) } ?: "Unknown"
            val (compatibilityLabel, compatibilityReason) =
                formatRefreshCompatibility(
                    sourceFrameRate = stats.sourceFrameRate,
                    displayRefreshRate = stats.displayRefreshRate,
                    matchFrameRateEnabled = stats.matchFrameRateEnabled,
                    frameRateMatched = stats.frameRateMatched
                )
            val compatibilityColor =
                when (compatibilityLabel) {
                    "Good" -> Color(0xFF22C55E)
                    "Poor" -> Color(0xFFEF4444)
                    "Disabled" -> Color(0xFF9CA3AF)
                    else -> Color(0xFFF59E0B)
                }
            val videoCodecText = listOfNotNull(stats.videoCodec, stats.videoMimeType).joinToString(" / ")
                .ifBlank { "Unknown" }
            val audioCodecText = listOfNotNull(stats.audioCodec, stats.audioMimeType).joinToString(" / ")
                .ifBlank { "Unknown" }
            val sourceResolutionText = stats.sourceResolution ?: "Unknown"
            val outputResolutionText = stats.outputResolution ?: "Unknown"
            val videoBitrateText = stats.videoBitrateKbps?.let { String.format(Locale.US, "%.1f kbps", it) } ?: "Unknown"
            val audioBitrateText = stats.audioBitrateKbps?.let { String.format(Locale.US, "%.1f kbps", it) } ?: "Unknown"
            val audioDetails = buildString {
                append(stats.audioLanguage ?: "Unknown")
                stats.audioChannels?.let { append(" / ").append(it).append("ch") }
                stats.audioSampleRateHz?.let { append(" / ").append(it).append("Hz") }
            }
            listOf(
                "Match Frame Rate Request: ${if (stats.matchFrameRateEnabled) "On" else "Off"}" to Color.White,
                "Source FPS: $fpsText" to Color.White,
                "Display Refresh: $hzText" to Color.White,
                "Refresh Compatibility: $compatibilityLabel" to compatibilityColor,
                "Compatibility Detail: $compatibilityReason" to Color.White,
                "Source Resolution: $sourceResolutionText" to Color.White,
                "Output Resolution: $outputResolutionText" to Color.White,
                "Video Codec: $videoCodecText" to Color.White,
                "Video Bitrate: $videoBitrateText" to Color.White,
                "Audio Codec: $audioCodecText" to Color.White,
                "Audio Bitrate: $audioBitrateText" to Color.White,
                "Audio: $audioDetails" to Color.White,
                "Dropped Frames: ${stats.droppedFrames}" to Color.White,
                "Buffered: ${stats.bufferedPercent}%" to Color.White,
                "Speed: ${String.format(Locale.US, "%.2fx", stats.playbackSpeed)}" to Color.White
            )
        }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppTheme.colors.overlayStrong.copy(alpha = 0.88f))
            .border(1.dp, AppTheme.colors.borderStrong, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .width(360.dp)
    ) {
        Text(
            text = "Stats for nerds",
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        lines.forEach { (line, lineColor) ->
            Text(
                text = line,
                color = lineColor,
                fontSize = 12.sp,
                fontFamily = AppTheme.fontFamily
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun collectPlayerNerdStats(
    context: Context,
    player: Player,
    matchFrameRateEnabled: Boolean,
    droppedFrames: Int
): PlayerNerdStats {
    val videoFormat = selectedTrackFormat(player, C.TRACK_TYPE_VIDEO)
    val audioFormat = selectedTrackFormat(player, C.TRACK_TYPE_AUDIO)
    val sourceFrameRate = videoFormat?.frameRate?.takeIf { it.isFinite() && it > 0f }
    val displayRefreshRate = currentDisplayRefreshRateHz(context)
    val frameRateMatched = computeFrameRateMatch(sourceFrameRate, displayRefreshRate, matchFrameRateEnabled)
    val sourceResolution = formatResolution(videoFormat?.width ?: 0, videoFormat?.height ?: 0)
    val outputResolution = formatResolution(player.videoSize.width, player.videoSize.height)

    return PlayerNerdStats(
        matchFrameRateEnabled = matchFrameRateEnabled,
        sourceFrameRate = sourceFrameRate,
        displayRefreshRate = displayRefreshRate,
        frameRateMatched = frameRateMatched,
        videoCodec = videoFormat?.codecs,
        videoMimeType = videoFormat?.sampleMimeType,
        videoBitrateKbps = videoFormat?.bitrate?.takeIf { it > 0 }?.div(1000f),
        sourceResolution = sourceResolution,
        outputResolution = outputResolution,
        audioCodec = audioFormat?.codecs,
        audioMimeType = audioFormat?.sampleMimeType,
        audioBitrateKbps = audioFormat?.bitrate?.takeIf { it > 0 }?.div(1000f),
        audioLanguage = audioFormat?.language,
        audioChannels = audioFormat?.channelCount?.takeIf { it > 0 },
        audioSampleRateHz = audioFormat?.sampleRate?.takeIf { it > 0 },
        droppedFrames = droppedFrames,
        bufferedPercent = player.bufferedPercentage.coerceAtLeast(0),
        playbackSpeed = player.playbackParameters.speed
    )
}

@OptIn(UnstableApi::class)
private fun selectedTrackFormat(player: Player, trackType: Int): Format? {
    val tracks = player.currentTracks.groups
    tracks.forEach { group ->
        if (group.type != trackType) return@forEach
        for (trackIndex in 0 until group.length) {
            if (group.isTrackSelected(trackIndex)) {
                return group.getTrackFormat(trackIndex)
            }
        }
    }
    tracks.forEach { group ->
        if (group.type != trackType) return@forEach
        for (trackIndex in 0 until group.length) {
            if (group.isTrackSupported(trackIndex)) {
                return group.getTrackFormat(trackIndex)
            }
        }
    }
    return null
}

private fun currentDisplayRefreshRateHz(context: Context): Float? {
    val activityDisplayRate =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.findActivity()?.display?.refreshRate
        } else {
            null
        }
    if (activityDisplayRate != null && activityDisplayRate > 0f) {
        return activityDisplayRate
    }
    val manager = context.getSystemService(DisplayManager::class.java) ?: return null
    val display = manager.getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: return null
    return display.refreshRate.takeIf { it > 0f }
}

private fun computeFrameRateMatch(
    sourceFrameRate: Float?,
    displayRefreshRate: Float?,
    matchFrameRateEnabled: Boolean
): Boolean? {
    if (sourceFrameRate == null || displayRefreshRate == null) return null
    if (!matchFrameRateEnabled) return false
    val toleranceHz = 0.5f
    if (abs(displayRefreshRate - sourceFrameRate) <= toleranceHz) return true
    val multiple = (displayRefreshRate / sourceFrameRate).roundToInt().coerceAtLeast(1)
    return abs(displayRefreshRate - (sourceFrameRate * multiple)) <= toleranceHz
}

private fun formatRefreshCompatibility(
    sourceFrameRate: Float?,
    displayRefreshRate: Float?,
    matchFrameRateEnabled: Boolean,
    frameRateMatched: Boolean?
): Pair<String, String> {
    if (!matchFrameRateEnabled) {
        return "Disabled" to "Match frame rate is off."
    }
    if (sourceFrameRate == null || displayRefreshRate == null) {
        return "Unknown" to "Missing source FPS or display refresh data."
    }

    val ratio = displayRefreshRate / sourceFrameRate
    val sourceText = String.format(Locale.US, "%.2f", sourceFrameRate)
    val displayText = String.format(Locale.US, "%.2f", displayRefreshRate)
    val ratioText = String.format(Locale.US, "%.2f", ratio)
    if (frameRateMatched == true) {
        return "Good" to "$sourceText fps on $displayText Hz (${ratioText}x) is a clean cadence."
    }

    val ratioTimesTwo = ratio * 2f
    val nearHalfStep = abs(ratioTimesTwo - ratioTimesTwo.roundToInt()) <= 0.03f
    val oddHalfStep = ratioTimesTwo.roundToInt() % 2 != 0
    if (nearHalfStep && oddHalfStep) {
        return "Poor" to "$sourceText fps on $displayText Hz (${ratioText}x) likely uses uneven cadence (e.g. 3:2)."
    }

    return "Poor" to "$sourceText fps on $displayText Hz (${ratioText}x) is not a clean multiple; judder is possible."
}

private fun formatResolution(width: Int, height: Int): String? {
    if (width <= 0 || height <= 0) return null
    return "${width}x${height}"
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
    val values = PlayerResizeMode.entries
    return values[(current.ordinal + 1) % values.size]
}

private fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

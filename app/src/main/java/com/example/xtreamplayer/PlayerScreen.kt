package com.example.xtreamplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import com.example.xtreamplayer.content.CategoryItem
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.content.ContentType
import com.example.xtreamplayer.content.LiveNowNextEpg
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.player.Media3PlaybackEngine
import com.example.xtreamplayer.settings.SettingsState

@Composable
internal fun PlayerScreen(
    activePlaybackQueue: PlaybackQueue?,
    activePlaybackTitle: String?,
    activePlaybackItem: ContentItem?,
    activePlaybackItems: List<ContentItem>,
    continueWatchingSubtitleState: PlaybackSubtitleState?,
    playbackEngine: Media3PlaybackEngine,
    subtitleRepository: SubtitleRepository,
    settings: SettingsState,
    onRequestOpenSubtitlesApiKey: () -> Unit,
    onExitPlayback: () -> Unit,
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
    val queue = activePlaybackQueue ?: return
    val player = playbackEngine.player
    var currentIndex by remember(queue, player) { mutableIntStateOf(player.currentMediaItemIndex) }
    DisposableEffect(queue, player) {
        currentIndex = player.currentMediaItemIndex
        val listener =
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    currentIndex = player.currentMediaItemIndex
                }
            }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    val queueItems = queue.items
    val hasNextEpisode = currentIndex >= 0 && currentIndex < queueItems.size - 1
    val nextEpisodeTitle = queueItems.getOrNull(currentIndex + 1)?.title
    val currentType = activePlaybackItem?.contentType ?: queueItems.getOrNull(currentIndex)?.type

    PlayerOverlay(
        title = activePlaybackTitle ?: "",
        activePlaybackItem = activePlaybackItem,
        activePlaybackItems = activePlaybackItems,
        persistedSubtitleState = continueWatchingSubtitleState,
        player = playbackEngine.player,
        playbackEngine = playbackEngine,
        subtitleRepository = subtitleRepository,
        subtitleAppearanceSettings = settings.subtitleAppearance,
        openSubtitlesApiKey = settings.openSubtitlesApiKey,
        openSubtitlesUserAgent = settings.openSubtitlesUserAgent,
        mediaId = activePlaybackItem?.streamId?.toString() ?: "",
        onRequestOpenSubtitlesApiKey = onRequestOpenSubtitlesApiKey,
        onExit = onExitPlayback,
        autoPlayNextEnabled = settings.autoPlayNext,
        matchFrameRateEnabled = settings.matchFrameRateEnabled,
        nextEpisodeThresholdSeconds = settings.nextEpisodeThresholdSeconds,
        currentContentType = currentType,
        nextEpisodeTitle = nextEpisodeTitle,
        hasNextEpisode = hasNextEpisode,
        onPlayNextEpisode = onPlayNextEpisode,
        onMatchFrameRateChange = onMatchFrameRateChange,
        onLiveChannelSwitch = onLiveChannelSwitch,
        onLiveGuideChannelSelect = onLiveGuideChannelSelect,
        onSubtitleStateChanged = onSubtitleStateChanged,
        loadLiveNowNext = loadLiveNowNext,
        loadLiveCategories = loadLiveCategories,
        loadLiveCategoryChannels = loadLiveCategoryChannels,
        loadLiveCategoryThumbnail = loadLiveCategoryThumbnail
    )
}

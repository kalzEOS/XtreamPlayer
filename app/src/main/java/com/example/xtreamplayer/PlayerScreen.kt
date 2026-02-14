package com.example.xtreamplayer

import androidx.compose.runtime.Composable
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
    val currentIndex = playbackEngine.player.currentMediaItemIndex
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

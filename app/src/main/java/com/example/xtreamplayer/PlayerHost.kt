package com.example.xtreamplayer

import androidx.compose.runtime.Composable
import com.example.xtreamplayer.content.CategoryItem
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.content.LiveNowNextEpg
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.player.Media3PlaybackEngine
import com.example.xtreamplayer.settings.SettingsState
import com.example.xtreamplayer.settings.SubtitleAppearanceSettings

@Composable
internal fun PlayerHost(
    activePlaybackQueue: PlaybackQueue?,
    activePlaybackTitle: String?,
    activePlaybackItem: ContentItem?,
    activePlaybackItems: List<ContentItem>,
    continueWatchingSubtitleState: PlaybackSubtitleState?,
    playbackEngine: Media3PlaybackEngine,
    subtitleRepository: SubtitleRepository,
    settings: SettingsState,
    subtitleAppearancePreview: SubtitleAppearanceSettings?,
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
    val effectivePlaybackSettings =
        subtitleAppearancePreview?.let { preview ->
            settings.copy(subtitleAppearance = preview)
        } ?: settings
    PlayerScreen(
        activePlaybackQueue = activePlaybackQueue,
        activePlaybackTitle = activePlaybackTitle,
        activePlaybackItem = activePlaybackItem,
        activePlaybackItems = activePlaybackItems,
        continueWatchingSubtitleState = continueWatchingSubtitleState,
        playbackEngine = playbackEngine,
        subtitleRepository = subtitleRepository,
        settings = effectivePlaybackSettings,
        onRequestOpenSubtitlesApiKey = onRequestOpenSubtitlesApiKey,
        onExitPlayback = onExitPlayback,
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

package com.example.xtreamplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.xtreamplayer.content.ContentRepository
import com.example.xtreamplayer.content.ContinueWatchingRepository
import com.example.xtreamplayer.content.FavoritesRepository
import com.example.xtreamplayer.content.HistoryRepository
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.player.Media3PlaybackEngine
import com.example.xtreamplayer.settings.PlaybackSettingsController
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var playbackSettingsController: PlaybackSettingsController

    @Inject lateinit var playbackEngine: Media3PlaybackEngine

    @Inject lateinit var contentRepository: ContentRepository

    @Inject lateinit var favoritesRepository: FavoritesRepository

    @Inject lateinit var historyRepository: HistoryRepository

    @Inject lateinit var continueWatchingRepository: ContinueWatchingRepository

    @Inject lateinit var subtitleRepository: SubtitleRepository

    @Inject lateinit var okHttpClient: OkHttpClient

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
                    subtitleRepository = subtitleRepository,
                    updateHttpClient = okHttpClient
            )
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            playbackEngine.release()
        }
        super.onDestroy()
    }
}

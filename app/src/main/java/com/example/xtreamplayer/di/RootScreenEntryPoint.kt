package com.example.xtreamplayer.di

import com.example.xtreamplayer.content.ContentRepository
import com.example.xtreamplayer.content.ContinueWatchingRepository
import com.example.xtreamplayer.content.FavoritesRepository
import com.example.xtreamplayer.content.HistoryRepository
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.player.Media3PlaybackEngine
import com.example.xtreamplayer.settings.PlaybackSettingsController
import com.example.xtreamplayer.settings.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RootScreenEntryPoint {
    fun playbackSettingsController(): PlaybackSettingsController
    fun playbackEngine(): Media3PlaybackEngine
    fun contentRepository(): ContentRepository
    fun favoritesRepository(): FavoritesRepository
    fun historyRepository(): HistoryRepository
    fun continueWatchingRepository(): ContinueWatchingRepository
    fun subtitleRepository(): SubtitleRepository
    fun settingsRepository(): SettingsRepository
    fun okHttpClient(): OkHttpClient
}

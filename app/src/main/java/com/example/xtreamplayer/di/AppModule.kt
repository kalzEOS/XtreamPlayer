package com.example.xtreamplayer.di

import android.content.Context
import com.example.xtreamplayer.api.OpenSubtitlesApi
import com.example.xtreamplayer.api.XtreamApi
import com.example.xtreamplayer.content.ContentCache
import com.example.xtreamplayer.content.ContentRepository
import com.example.xtreamplayer.content.ContinueWatchingRepository
import com.example.xtreamplayer.content.FavoritesRepository
import com.example.xtreamplayer.content.HistoryRepository
import com.example.xtreamplayer.content.SubtitleRepository
import com.example.xtreamplayer.player.Media3PlaybackEngine
import com.example.xtreamplayer.settings.PlaybackSettingsController
import com.example.xtreamplayer.settings.SettingsRepository
import com.example.xtreamplayer.auth.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideXtreamApi(client: OkHttpClient): XtreamApi = XtreamApi(client)

    @Provides
    @Singleton
    fun provideContentCache(
        @ApplicationContext context: Context
    ): ContentCache = ContentCache(context)

    @Provides
    @Singleton
    fun provideContentRepository(
        api: XtreamApi,
        cache: ContentCache
    ): ContentRepository = ContentRepository(api, cache)

    @Provides
    @Singleton
    fun provideFavoritesRepository(
        @ApplicationContext context: Context
    ): FavoritesRepository = FavoritesRepository(context)

    @Provides
    @Singleton
    fun provideHistoryRepository(
        @ApplicationContext context: Context
    ): HistoryRepository = HistoryRepository(context)

    @Provides
    @Singleton
    fun provideContinueWatchingRepository(
        @ApplicationContext context: Context
    ): ContinueWatchingRepository = ContinueWatchingRepository(context)

    @Provides
    @Singleton
    fun provideMedia3PlaybackEngine(
        @ApplicationContext context: Context
    ): Media3PlaybackEngine = Media3PlaybackEngine(context)

    @Provides
    @Singleton
    fun providePlaybackSettingsController(): PlaybackSettingsController =
        PlaybackSettingsController()

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context
    ): AuthRepository = AuthRepository(context)

    @Provides
    @Singleton
    fun provideOpenSubtitlesApi(client: OkHttpClient): OpenSubtitlesApi = OpenSubtitlesApi(client)

    @Provides
    @Singleton
    fun provideSubtitleRepository(
        @ApplicationContext context: Context,
        api: OpenSubtitlesApi
    ): SubtitleRepository = SubtitleRepository(context, api)
}

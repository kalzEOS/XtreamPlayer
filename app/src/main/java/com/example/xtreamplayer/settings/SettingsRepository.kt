package com.example.xtreamplayer.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        val autoPlay = prefs[Keys.AUTO_PLAY_NEXT] ?: true
        val subtitles = prefs[Keys.SUBTITLES_ENABLED] ?: false
        val matchFrameRate = prefs[Keys.MATCH_FRAME_RATE] ?: true
        val quality = parsePlaybackQuality(prefs[Keys.PLAYBACK_QUALITY])
        val audio = parseAudioLanguage(prefs[Keys.AUDIO_LANGUAGE])
        val rememberLogin = prefs[Keys.REMEMBER_LOGIN] ?: true
        val autoSignIn = prefs[Keys.AUTO_SIGN_IN] ?: true
        val parentalPin = prefs[Keys.PARENTAL_PIN_ENABLED] ?: false
        val parentalRating = parseParentalRating(prefs[Keys.PARENTAL_RATING])
        val openSubtitlesApiKey = prefs[Keys.OPENSUBTITLES_API_KEY] ?: ""
        val openSubtitlesUserAgent = prefs[Keys.OPENSUBTITLES_USER_AGENT] ?: "XtreamPlayer"

        SettingsState(
            autoPlayNext = autoPlay,
            subtitlesEnabled = subtitles,
            matchFrameRate = matchFrameRate,
            playbackQuality = quality,
            audioLanguage = audio,
            rememberLogin = rememberLogin,
            autoSignIn = autoSignIn,
            parentalPinEnabled = parentalPin,
            parentalRating = parentalRating,
            openSubtitlesApiKey = openSubtitlesApiKey,
            openSubtitlesUserAgent = openSubtitlesUserAgent
        )
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_PLAY_NEXT] = enabled
        }
    }

    suspend fun setSubtitlesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SUBTITLES_ENABLED] = enabled
        }
    }

    suspend fun setMatchFrameRate(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MATCH_FRAME_RATE] = enabled
        }
    }

    suspend fun setPlaybackQuality(quality: PlaybackQuality) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PLAYBACK_QUALITY] = quality.name
        }
    }

    suspend fun setAudioLanguage(language: AudioLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUDIO_LANGUAGE] = language.name
        }
    }

    suspend fun setRememberLogin(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMEMBER_LOGIN] = enabled
        }
    }

    suspend fun setAutoSignIn(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SIGN_IN] = enabled
        }
    }

    suspend fun setParentalPinEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PARENTAL_PIN_ENABLED] = enabled
        }
    }

    suspend fun setParentalRating(rating: ParentalRating) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PARENTAL_RATING] = rating.name
        }
    }

    suspend fun setOpenSubtitlesApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OPENSUBTITLES_API_KEY] = apiKey
        }
    }

    suspend fun setOpenSubtitlesUserAgent(userAgent: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OPENSUBTITLES_USER_AGENT] = userAgent
        }
    }

    private fun parsePlaybackQuality(value: String?): PlaybackQuality {
        return PlaybackQuality.values().firstOrNull { it.name == value } ?: PlaybackQuality.AUTO
    }

    private fun parseAudioLanguage(value: String?): AudioLanguage {
        return AudioLanguage.values().firstOrNull { it.name == value } ?: AudioLanguage.ENGLISH
    }

    private fun parseParentalRating(value: String?): ParentalRating {
        return ParentalRating.values().firstOrNull { it.name == value } ?: ParentalRating.EVERYONE
    }

    private object Keys {
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val MATCH_FRAME_RATE = booleanPreferencesKey("match_frame_rate")
        val PLAYBACK_QUALITY = stringPreferencesKey("playback_quality")
        val AUDIO_LANGUAGE = stringPreferencesKey("audio_language")
        val REMEMBER_LOGIN = booleanPreferencesKey("remember_login")
        val AUTO_SIGN_IN = booleanPreferencesKey("auto_sign_in")
        val PARENTAL_PIN_ENABLED = booleanPreferencesKey("parental_pin_enabled")
        val PARENTAL_RATING = stringPreferencesKey("parental_rating")
        val OPENSUBTITLES_API_KEY = stringPreferencesKey("opensubtitles_api_key")
        val OPENSUBTITLES_USER_AGENT = stringPreferencesKey("opensubtitles_user_agent")
    }
}

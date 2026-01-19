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
        val audio = parseAudioLanguage(prefs[Keys.AUDIO_LANGUAGE])
        val rememberLogin = prefs[Keys.REMEMBER_LOGIN] ?: true
        val autoSignIn = prefs[Keys.AUTO_SIGN_IN] ?: true
        val openSubtitlesApiKey = prefs[Keys.OPENSUBTITLES_API_KEY] ?: ""
        val openSubtitlesUserAgent = prefs[Keys.OPENSUBTITLES_USER_AGENT] ?: "XtreamPlayer"

        SettingsState(
            autoPlayNext = autoPlay,
            subtitlesEnabled = subtitles,
            audioLanguage = audio,
            rememberLogin = rememberLogin,
            autoSignIn = autoSignIn,
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

    private fun parseAudioLanguage(value: String?): AudioLanguage {
        return AudioLanguage.values().firstOrNull { it.name == value } ?: AudioLanguage.ENGLISH
    }

    private object Keys {
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val AUDIO_LANGUAGE = stringPreferencesKey("audio_language")
        val REMEMBER_LOGIN = booleanPreferencesKey("remember_login")
        val AUTO_SIGN_IN = booleanPreferencesKey("auto_sign_in")
        val OPENSUBTITLES_API_KEY = stringPreferencesKey("opensubtitles_api_key")
        val OPENSUBTITLES_USER_AGENT = stringPreferencesKey("opensubtitles_user_agent")
    }
}

package com.example.xtreamplayer.settings

import android.content.Context
import com.example.xtreamplayer.ui.theme.AppFont
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        val autoPlay = prefs[Keys.AUTO_PLAY_NEXT] ?: true
        val nextEpisodeThreshold = prefs[Keys.NEXT_EPISODE_THRESHOLD] ?: 60
        val subtitles = prefs[Keys.SUBTITLES_ENABLED] ?: false
        val rememberLogin = prefs[Keys.REMEMBER_LOGIN] ?: true
        val autoSignIn = prefs[Keys.AUTO_SIGN_IN] ?: true
        val appTheme = parseAppTheme(prefs[Keys.APP_THEME])
        val appFont = parseAppFont(prefs[Keys.APP_FONT])
        val openSubtitlesApiKey = prefs[Keys.OPENSUBTITLES_API_KEY] ?: ""
        val openSubtitlesUserAgent = prefs[Keys.OPENSUBTITLES_USER_AGENT] ?: ""

        SettingsState(
            autoPlayNext = autoPlay,
            nextEpisodeThresholdSeconds = nextEpisodeThreshold,
            subtitlesEnabled = subtitles,
            rememberLogin = rememberLogin,
            autoSignIn = autoSignIn,
            appTheme = appTheme,
            appFont = appFont,
            openSubtitlesApiKey = openSubtitlesApiKey,
            openSubtitlesUserAgent = openSubtitlesUserAgent
        )
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_PLAY_NEXT] = enabled
        }
    }

    suspend fun setNextEpisodeThreshold(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NEXT_EPISODE_THRESHOLD] = seconds.coerceIn(0, 300)
        }
    }

    suspend fun setSubtitlesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SUBTITLES_ENABLED] = enabled
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

    suspend fun setAppTheme(theme: AppThemeOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_THEME] = theme.name
        }
    }

    suspend fun setAppFont(font: AppFont) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_FONT] = font.name
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

    private fun parseAppTheme(value: String?): AppThemeOption {
        return AppThemeOption.values().firstOrNull { it.name == value } ?: AppThemeOption.DEFAULT
    }

    private fun parseAppFont(value: String?): AppFont {
        if (value == null) return AppFont.DEFAULT
        return AppFont.values().firstOrNull { it.name == value } ?: AppFont.DEFAULT
    }

    private object Keys {
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val NEXT_EPISODE_THRESHOLD = intPreferencesKey("next_episode_threshold")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val REMEMBER_LOGIN = booleanPreferencesKey("remember_login")
        val AUTO_SIGN_IN = booleanPreferencesKey("auto_sign_in")
        val APP_THEME = stringPreferencesKey("app_theme")
        val APP_FONT = stringPreferencesKey("app_font")
        val OPENSUBTITLES_API_KEY = stringPreferencesKey("opensubtitles_api_key")
        val OPENSUBTITLES_USER_AGENT = stringPreferencesKey("opensubtitles_user_agent")
    }
}

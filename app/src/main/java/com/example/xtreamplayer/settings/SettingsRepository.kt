package com.example.xtreamplayer.settings

import android.content.Context
import com.example.xtreamplayer.ui.theme.AppFont
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val bootPrefs = context.getSharedPreferences("boot_settings", Context.MODE_PRIVATE)
    val settings: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        val autoPlay = prefs[Keys.AUTO_PLAY_NEXT] ?: true
        val nextEpisodeThreshold = prefs[Keys.NEXT_EPISODE_THRESHOLD] ?: 45
        val subtitles = prefs[Keys.SUBTITLES_ENABLED] ?: true
        val checkUpdatesOnStartup = prefs[Keys.CHECK_UPDATES_ON_STARTUP] ?: false
        val rememberLogin = prefs[Keys.REMEMBER_LOGIN] ?: true
        val autoSignIn = prefs[Keys.AUTO_SIGN_IN] ?: true
        val appTheme = parseAppTheme(prefs[Keys.APP_THEME])
        val appFont = parseAppFont(prefs[Keys.APP_FONT])
        val uiScale = resolveUiScale(prefs)
        val fontScale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.7f, 1.4f)
        val clockFormat = parseClockFormat(prefs[Keys.CLOCK_FORMAT])
        val openSubtitlesApiKey = prefs[Keys.OPENSUBTITLES_API_KEY] ?: ""
        val openSubtitlesUserAgent = prefs[Keys.OPENSUBTITLES_USER_AGENT] ?: ""

        cacheBootSettings(appTheme, appFont, uiScale, fontScale)

        SettingsState(
            autoPlayNext = autoPlay,
            nextEpisodeThresholdSeconds = nextEpisodeThreshold,
            subtitlesEnabled = subtitles,
            checkUpdatesOnStartup = checkUpdatesOnStartup,
            rememberLogin = rememberLogin,
            autoSignIn = autoSignIn,
            appTheme = appTheme,
            appFont = appFont,
            uiScale = uiScale,
            fontScale = fontScale,
            clockFormat = clockFormat,
            openSubtitlesApiKey = openSubtitlesApiKey,
            openSubtitlesUserAgent = openSubtitlesUserAgent
        )
    }

    fun cachedSettings(): SettingsState {
        val base = SettingsState()
        val theme = parseAppTheme(bootPrefs.getString(Keys.BOOT_APP_THEME, null))
        val font = parseAppFont(bootPrefs.getString(Keys.BOOT_APP_FONT, null))
        val uiScale =
            bootPrefs.getFloat(Keys.BOOT_UI_SCALE, base.uiScale).coerceIn(UI_SCALE_MIN, UI_SCALE_MAX)
        val fontScale =
            bootPrefs.getFloat(Keys.BOOT_FONT_SCALE, base.fontScale).coerceIn(0.7f, 1.4f)
        return base.copy(appTheme = theme, appFont = font, uiScale = uiScale, fontScale = fontScale)
    }

    /**
     * One-time migration for legacy synchronous boot prefs into DataStore-backed settings.
     * Keeps startup snappy while converging runtime reads to DataStore values.
     */
    suspend fun migrateBootSettingsToDataStore() {
        val bootUiScale =
            if (bootPrefs.contains(Keys.BOOT_UI_SCALE)) {
                bootPrefs.getFloat(Keys.BOOT_UI_SCALE, UI_SCALE_BASE).coerceIn(UI_SCALE_MIN, UI_SCALE_MAX)
            } else {
                null
            }
        val bootFontScale =
            if (bootPrefs.contains(Keys.BOOT_FONT_SCALE)) {
                bootPrefs.getFloat(Keys.BOOT_FONT_SCALE, 1.0f).coerceIn(0.7f, 1.4f)
            } else {
                null
            }
        val bootTheme = bootPrefs.getString(Keys.BOOT_APP_THEME, null)
        val bootFont = bootPrefs.getString(Keys.BOOT_APP_FONT, null)

        if (bootUiScale == null && bootFontScale == null && bootTheme == null && bootFont == null) {
            return
        }

        context.dataStore.edit { prefs ->
            if (prefs[Keys.UI_SCALE] == null && bootUiScale != null) {
                prefs[Keys.UI_SCALE] = bootUiScale
            }
            if (prefs[Keys.FONT_SCALE] == null && bootFontScale != null) {
                prefs[Keys.FONT_SCALE] = bootFontScale
            }
            if (prefs[Keys.APP_THEME] == null && !bootTheme.isNullOrBlank()) {
                prefs[Keys.APP_THEME] = bootTheme
            }
            if (prefs[Keys.APP_FONT] == null && !bootFont.isNullOrBlank()) {
                prefs[Keys.APP_FONT] = bootFont
            }
        }
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

    suspend fun setCheckUpdatesOnStartup(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CHECK_UPDATES_ON_STARTUP] = enabled
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
        cacheBootSettings(appTheme = theme)
    }

    suspend fun setAppFont(font: AppFont) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_FONT] = font.name
        }
        cacheBootSettings(appFont = font)
    }

    suspend fun setUiScale(scale: Float) {
        val clamped = scale.coerceIn(UI_SCALE_MIN, UI_SCALE_MAX)
        context.dataStore.edit { prefs ->
            prefs[Keys.UI_SCALE] = clamped
        }
        cacheBootSettings(uiScale = clamped)
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_SCALE] = scale.coerceIn(0.7f, 1.4f)
        }
        cacheBootSettings(fontScale = scale.coerceIn(0.7f, 1.4f))
    }

    suspend fun setClockFormat(clockFormat: ClockFormatOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CLOCK_FORMAT] = clockFormat.name
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

    suspend fun isStartupUpdateCheckEnabled(): Boolean {
        return context.dataStore.data.firstOrNull()?.get(Keys.CHECK_UPDATES_ON_STARTUP) ?: false
    }

    private fun parseAppTheme(value: String?): AppThemeOption {
        return AppThemeOption.values().firstOrNull { it.name == value } ?: AppThemeOption.DEFAULT
    }

    private fun parseAppFont(value: String?): AppFont {
        if (value == null) return AppFont.DEFAULT
        return AppFont.values().firstOrNull { it.name == value } ?: AppFont.DEFAULT
    }

    private fun parseClockFormat(value: String?): ClockFormatOption {
        if (value == null) return ClockFormatOption.AM_PM
        return ClockFormatOption.values().firstOrNull { it.name == value } ?: ClockFormatOption.AM_PM
    }

    private fun cacheBootSettings(
        appTheme: AppThemeOption? = null,
        appFont: AppFont? = null,
        uiScale: Float? = null,
        fontScale: Float? = null
    ) {
        val editor = bootPrefs.edit()
        if (appTheme != null) editor.putString(Keys.BOOT_APP_THEME, appTheme.name)
        if (appFont != null) editor.putString(Keys.BOOT_APP_FONT, appFont.name)
        if (uiScale != null) editor.putFloat(Keys.BOOT_UI_SCALE, uiScale)
        if (fontScale != null) editor.putFloat(Keys.BOOT_FONT_SCALE, fontScale)
        editor.apply()
    }

    private fun resolveUiScale(prefs: androidx.datastore.preferences.core.Preferences): Float {
        val stored = prefs[Keys.UI_SCALE]
        val value =
            when {
                stored != null -> stored
                bootPrefs.contains(Keys.BOOT_UI_SCALE) ->
                    bootPrefs.getFloat(Keys.BOOT_UI_SCALE, UI_SCALE_BASE)
                else -> UI_SCALE_BASE
            }
        return value.coerceIn(UI_SCALE_MIN, UI_SCALE_MAX)
    }

    private object Keys {
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val NEXT_EPISODE_THRESHOLD = intPreferencesKey("next_episode_threshold")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val CHECK_UPDATES_ON_STARTUP = booleanPreferencesKey("check_updates_on_startup")
        val REMEMBER_LOGIN = booleanPreferencesKey("remember_login")
        val AUTO_SIGN_IN = booleanPreferencesKey("auto_sign_in")
        val APP_THEME = stringPreferencesKey("app_theme")
        val APP_FONT = stringPreferencesKey("app_font")
        val UI_SCALE = floatPreferencesKey("ui_scale")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val CLOCK_FORMAT = stringPreferencesKey("clock_format")
        val OPENSUBTITLES_API_KEY = stringPreferencesKey("opensubtitles_api_key")
        val OPENSUBTITLES_USER_AGENT = stringPreferencesKey("opensubtitles_user_agent")
        const val BOOT_APP_THEME = "boot_app_theme"
        const val BOOT_APP_FONT = "boot_app_font"
        const val BOOT_UI_SCALE = "boot_ui_scale"
        const val BOOT_FONT_SCALE = "boot_font_scale"

        // Progressive sync state keys
        val SYNC_PHASE = stringPreferencesKey("sync_phase")
        val SYNC_FAST_START_READY = booleanPreferencesKey("sync_fast_start_ready")
        val SYNC_FULL_COMPLETE = booleanPreferencesKey("sync_full_complete")
        val SYNC_LAST_TIMESTAMP = longPreferencesKey("sync_last_timestamp")
        val SYNC_PAUSED = booleanPreferencesKey("sync_paused")
        val SYNC_ACCOUNT_KEY = stringPreferencesKey("sync_account_key")
    }

    /**
     * Save progressive sync state to DataStore
     */
    suspend fun saveSyncState(
        state: com.example.xtreamplayer.content.ProgressiveSyncState,
        accountKey: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYNC_PHASE] = state.phase.name
            prefs[Keys.SYNC_FAST_START_READY] = state.fastStartReady
            prefs[Keys.SYNC_FULL_COMPLETE] = state.fullIndexComplete
            prefs[Keys.SYNC_LAST_TIMESTAMP] = state.lastSyncTimestamp
            prefs[Keys.SYNC_PAUSED] = state.isPaused
            prefs[Keys.SYNC_ACCOUNT_KEY] = accountKey
        }
    }

    /**
     * Load progressive sync state from DataStore
     */
    suspend fun loadSyncState(accountKey: String): com.example.xtreamplayer.content.ProgressiveSyncState? {
        return context.dataStore.data.firstOrNull()?.let { prefs ->
            val phaseStr = prefs[Keys.SYNC_PHASE] ?: return@let null
            val storedAccountKey = prefs[Keys.SYNC_ACCOUNT_KEY] ?: return@let null
            if (storedAccountKey != accountKey) return@let null
            com.example.xtreamplayer.content.ProgressiveSyncState(
                phase = com.example.xtreamplayer.content.SyncPhase.valueOf(phaseStr),
                sectionsCompleted = emptySet(),
                fastStartReady = prefs[Keys.SYNC_FAST_START_READY] ?: false,
                fullIndexComplete = prefs[Keys.SYNC_FULL_COMPLETE] ?: false,
                currentSection = null,
                sectionProgress = emptyMap(),
                isPaused = prefs[Keys.SYNC_PAUSED] ?: false,
                lastSyncTimestamp = prefs[Keys.SYNC_LAST_TIMESTAMP] ?: 0L
            )
        }
    }
}

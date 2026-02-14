package com.example.xtreamplayer.settings

import androidx.lifecycle.ViewModel
import com.example.xtreamplayer.ui.theme.AppFont
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            repository.migrateBootSettingsToDataStore()
        }
    }

    val settings: StateFlow<SettingsState> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.cachedSettings()
    )

    fun toggleAutoPlayNext() {
        viewModelScope.launch {
            repository.setAutoPlayNext(!settings.value.autoPlayNext)
        }
    }

    fun setNextEpisodeThreshold(seconds: Int) {
        viewModelScope.launch {
            repository.setNextEpisodeThreshold(seconds)
        }
    }

    fun toggleSubtitles() {
        viewModelScope.launch {
            repository.setSubtitlesEnabled(!settings.value.subtitlesEnabled)
        }
    }

    fun setSubtitleCacheAutoClearInterval(intervalMs: Long) {
        viewModelScope.launch {
            repository.setSubtitleCacheAutoClearInterval(intervalMs)
        }
    }

    fun setMatchFrameRateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMatchFrameRateEnabled(enabled)
        }
    }

    fun toggleCheckUpdatesOnStartup() {
        viewModelScope.launch {
            repository.setCheckUpdatesOnStartup(!settings.value.checkUpdatesOnStartup)
        }
    }

    fun toggleRememberLogin() {
        viewModelScope.launch {
            repository.setRememberLogin(!settings.value.rememberLogin)
        }
    }

    fun toggleAutoSignIn() {
        viewModelScope.launch {
            repository.setAutoSignIn(!settings.value.autoSignIn)
        }
    }

    fun setAppTheme(theme: AppThemeOption) {
        viewModelScope.launch {
            repository.setAppTheme(theme)
        }
    }

    fun setAppFont(font: AppFont) {
        viewModelScope.launch {
            repository.setAppFont(font)
        }
    }

    fun setUiScale(scale: Float) {
        viewModelScope.launch {
            repository.setUiScale(scale)
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            repository.setFontScale(scale)
        }
    }

    fun toggleClockFormat() {
        val next =
            if (settings.value.clockFormat == ClockFormatOption.AM_PM) {
                ClockFormatOption.HOUR_24
            } else {
                ClockFormatOption.AM_PM
            }
        viewModelScope.launch {
            repository.setClockFormat(next)
        }
    }

    fun setOpenSubtitlesApiKey(apiKey: String) {
        viewModelScope.launch {
            repository.setOpenSubtitlesApiKey(apiKey)
        }
    }

    fun setOpenSubtitlesUserAgent(userAgent: String) {
        viewModelScope.launch {
            repository.setOpenSubtitlesUserAgent(userAgent)
        }
    }
}

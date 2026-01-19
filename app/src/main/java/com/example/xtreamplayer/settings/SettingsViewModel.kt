package com.example.xtreamplayer.settings

import androidx.lifecycle.ViewModel
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
    val settings: StateFlow<SettingsState> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState()
    )

    fun toggleAutoPlayNext() {
        viewModelScope.launch {
            repository.setAutoPlayNext(!settings.value.autoPlayNext)
        }
    }

    fun toggleSubtitles() {
        viewModelScope.launch {
            repository.setSubtitlesEnabled(!settings.value.subtitlesEnabled)
        }
    }

    fun cyclePlaybackQuality() {
        val options = PlaybackQuality.values()
        val currentIndex = options.indexOf(settings.value.playbackQuality).coerceAtLeast(0)
        val next = options[(currentIndex + 1) % options.size]
        viewModelScope.launch {
            repository.setPlaybackQuality(next)
        }
    }

    fun cycleAudioLanguage() {
        val options = AudioLanguage.values()
        val currentIndex = options.indexOf(settings.value.audioLanguage).coerceAtLeast(0)
        val next = options[(currentIndex + 1) % options.size]
        viewModelScope.launch {
            repository.setAudioLanguage(next)
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

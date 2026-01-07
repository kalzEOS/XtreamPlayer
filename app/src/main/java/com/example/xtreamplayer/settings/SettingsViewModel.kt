package com.example.xtreamplayer.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
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

    fun toggleMatchFrameRate() {
        viewModelScope.launch {
            repository.setMatchFrameRate(!settings.value.matchFrameRate)
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

    fun toggleWifiOnlyStreaming() {
        viewModelScope.launch {
            repository.setWifiOnlyStreaming(!settings.value.wifiOnlyStreaming)
        }
    }

    fun toggleDataSaver() {
        viewModelScope.launch {
            repository.setDataSaverEnabled(!settings.value.dataSaverEnabled)
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

    fun toggleParentalPin() {
        viewModelScope.launch {
            repository.setParentalPinEnabled(!settings.value.parentalPinEnabled)
        }
    }

    fun cycleParentalRating() {
        val options = ParentalRating.values()
        val currentIndex = options.indexOf(settings.value.parentalRating).coerceAtLeast(0)
        val next = options[(currentIndex + 1) % options.size]
        viewModelScope.launch {
            repository.setParentalRating(next)
        }
    }
}

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val repository = SettingsRepository(context)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

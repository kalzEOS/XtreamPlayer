package com.example.xtreamplayer.settings

import com.example.xtreamplayer.player.PlaybackEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlaybackSettingsController {
    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings
    private var engine: PlaybackEngine? = null

    fun bind(engine: PlaybackEngine) {
        this.engine = engine
        engine.applySettings(_settings.value)
    }

    fun unbind(engine: PlaybackEngine) {
        if (this.engine == engine) {
            this.engine = null
        }
    }

    fun apply(settings: SettingsState) {
        _settings.value = settings
        engine?.applySettings(settings)
    }
}

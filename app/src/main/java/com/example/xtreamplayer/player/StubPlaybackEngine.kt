package com.example.xtreamplayer.player

import com.example.xtreamplayer.settings.SettingsState

class StubPlaybackEngine : PlaybackEngine {
    var lastSettings: SettingsState = SettingsState()
        private set

    override fun applySettings(settings: SettingsState) {
        lastSettings = settings
    }
}

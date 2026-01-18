package com.example.xtreamplayer.player

import com.example.xtreamplayer.settings.SettingsState

interface PlaybackEngine {
    fun applySettings(settings: SettingsState)
}

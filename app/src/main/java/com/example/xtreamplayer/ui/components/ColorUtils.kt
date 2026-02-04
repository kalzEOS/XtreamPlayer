package com.example.xtreamplayer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

internal fun bestContrastText(background: Color, primary: Color, onAccent: Color): Color {
    val bgLuminance = background.luminance()
    val primaryLuminance = primary.luminance()
    val onAccentLuminance = onAccent.luminance()
    val primaryContrast =
        (max(bgLuminance, primaryLuminance) + 0.05f) /
            (min(bgLuminance, primaryLuminance) + 0.05f)
    val onAccentContrast =
        (max(bgLuminance, onAccentLuminance) + 0.05f) /
            (min(bgLuminance, onAccentLuminance) + 0.05f)
    return if (onAccentContrast >= primaryContrast) onAccent else primary
}

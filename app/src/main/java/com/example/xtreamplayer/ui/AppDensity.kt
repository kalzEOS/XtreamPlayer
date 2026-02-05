package com.example.xtreamplayer.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Density

data class AppScale(
    val uiScale: Float = 1f,
    val fontScale: Float = 1f
)

val LocalAppScale = staticCompositionLocalOf { AppScale() }
val LocalAppBaseDensity = staticCompositionLocalOf<Density?> { null }

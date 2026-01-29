package com.example.xtreamplayer.settings

import kotlin.math.roundToInt

const val UI_SCALE_BASE = 0.9f
const val UI_SCALE_MIN = 0.7f
const val UI_SCALE_MAX = 1.3f

fun uiScaleToDisplay(scale: Float): Float {
    return scale / UI_SCALE_BASE
}

fun displayToUiScale(display: Float): Float {
    return display * UI_SCALE_BASE
}

fun uiScaleDisplayPercent(scale: Float): Int {
    return (uiScaleToDisplay(scale) * 100).roundToInt()
}

fun uiScaleMinDisplayPercent(): Int {
    return (uiScaleToDisplay(UI_SCALE_MIN) * 100).roundToInt()
}

fun uiScaleMaxDisplayPercent(): Int {
    return (uiScaleToDisplay(UI_SCALE_MAX) * 100).roundToInt()
}

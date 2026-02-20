package com.example.xtreamplayer.settings

import android.graphics.Color
import android.util.TypedValue
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import kotlin.math.roundToInt

const val SUBTITLE_TEXT_SIZE_MIN_SP = 12f
const val SUBTITLE_TEXT_SIZE_MAX_SP = 36f
const val SUBTITLE_BOTTOM_PADDING_MIN = 0.02f
const val SUBTITLE_BOTTOM_PADDING_MAX = 0.20f

data class SubtitleAppearanceSettings(
    val customStyleEnabled: Boolean = false,
    val textSizeSp: Float = 18f,
    val textColor: Int = Color.WHITE,
    val backgroundOpacityPercent: Int = 55,
    val edgeType: Int = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
    val bottomPaddingFraction: Float = SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION,
    val overrideEmbeddedStyles: Boolean = false
)

data class SubtitleColorPreset(
    val label: String,
    val color: Int
)

enum class SubtitleEdgeStyle(
    val value: Int,
    val label: String
) {
    OUTLINE(CaptionStyleCompat.EDGE_TYPE_OUTLINE, "Outline"),
    DROP_SHADOW(CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW, "Shadow"),
    NONE(CaptionStyleCompat.EDGE_TYPE_NONE, "None");

    companion object {
        fun fromValue(value: Int): SubtitleEdgeStyle {
            return entries.firstOrNull { it.value == value } ?: OUTLINE
        }
    }
}

val subtitleTextColorPresets: List<SubtitleColorPreset> = listOf(
    SubtitleColorPreset(label = "White", color = Color.WHITE),
    SubtitleColorPreset(label = "Yellow", color = 0xFFFFFF00.toInt()),
    SubtitleColorPreset(label = "Cyan", color = 0xFF00FFFF.toInt()),
    SubtitleColorPreset(label = "Green", color = 0xFF7CFF7C.toInt())
)

fun SubtitleAppearanceSettings.normalized(): SubtitleAppearanceSettings {
    return copy(
        textSizeSp = textSizeSp.coerceIn(SUBTITLE_TEXT_SIZE_MIN_SP, SUBTITLE_TEXT_SIZE_MAX_SP),
        backgroundOpacityPercent = backgroundOpacityPercent.coerceIn(0, 100),
        edgeType = SubtitleEdgeStyle.fromValue(edgeType).value,
        bottomPaddingFraction =
            bottomPaddingFraction.coerceIn(SUBTITLE_BOTTOM_PADDING_MIN, SUBTITLE_BOTTOM_PADDING_MAX)
    )
}

fun subtitleAppearanceLabel(settings: SubtitleAppearanceSettings): String {
    val normalized = settings.normalized()
    if (!normalized.customStyleEnabled) {
        return "Default"
    }
    val sizeLabel = normalized.textSizeSp.roundToInt()
    return "${sizeLabel}sp • ${normalized.backgroundOpacityPercent}% bg"
}

fun subtitleEdgeStyleLabel(edgeType: Int): String {
    return SubtitleEdgeStyle.fromValue(edgeType).label
}

fun SubtitleView.applySubtitleAppearanceSettings(settings: SubtitleAppearanceSettings) {
    val normalized = settings.normalized()
    if (!normalized.customStyleEnabled) {
        setUserDefaultStyle()
        setUserDefaultTextSize()
        setApplyEmbeddedStyles(true)
        setApplyEmbeddedFontSizes(true)
        setBottomPaddingFraction(SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION)
        return
    }

    val alpha = (normalized.backgroundOpacityPercent * 255 / 100).coerceIn(0, 255)
    val backgroundColor = Color.argb(alpha, 0, 0, 0)
    val style = CaptionStyleCompat(
        normalized.textColor,
        backgroundColor,
        Color.TRANSPARENT,
        normalized.edgeType,
        Color.BLACK,
        null
    )

    setStyle(style)
    setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, normalized.textSizeSp)
    setBottomPaddingFraction(normalized.bottomPaddingFraction)
    val applyEmbeddedStyles = !normalized.overrideEmbeddedStyles
    setApplyEmbeddedStyles(applyEmbeddedStyles)
    setApplyEmbeddedFontSizes(applyEmbeddedStyles)
}

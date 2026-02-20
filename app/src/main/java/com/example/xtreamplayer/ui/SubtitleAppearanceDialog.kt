package com.example.xtreamplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.media3.ui.CaptionStyleCompat
import android.text.Layout
import androidx.media3.common.text.Cue
import androidx.media3.ui.SubtitleView
import android.util.TypedValue
import com.example.xtreamplayer.settings.SUBTITLE_BOTTOM_PADDING_MAX
import com.example.xtreamplayer.settings.SUBTITLE_BOTTOM_PADDING_MIN
import com.example.xtreamplayer.settings.SUBTITLE_TEXT_SIZE_MAX_SP
import com.example.xtreamplayer.settings.SUBTITLE_TEXT_SIZE_MIN_SP
import com.example.xtreamplayer.settings.SubtitleAppearanceSettings
import com.example.xtreamplayer.settings.SubtitleEdgeStyle
import com.example.xtreamplayer.settings.applySubtitleAppearanceSettings
import com.example.xtreamplayer.settings.normalized
import com.example.xtreamplayer.settings.subtitleEdgeStyleLabel
import com.example.xtreamplayer.settings.subtitleTextColorPresets
import com.example.xtreamplayer.ui.components.TopBarButton
import com.example.xtreamplayer.ui.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun SubtitleAppearanceDialog(
    initialSettings: SubtitleAppearanceSettings,
    onSave: (SubtitleAppearanceSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val previewHeight = 260.dp
    val defaults = remember { SubtitleAppearanceSettings().normalized() }
    var draft by remember(initialSettings) { mutableStateOf(initialSettings.normalized()) }
    var showResetAllConfirmation by remember { mutableStateOf(false) }
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val panelShape = RoundedCornerShape(12.dp)
    val enableCustomStyleFocusRequester = remember { FocusRequester() }
    val resetAllFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    val sampleCues = remember {
        listOf(
            Cue.Builder()
                .setText("Subtitle preview sample")
                .setTextAlignment(Layout.Alignment.ALIGN_CENTER)
                .setPosition(0.5f)
                .setPositionAnchor(Cue.ANCHOR_TYPE_MIDDLE)
                .build()
        )
    }
    fun updateDraft(updated: SubtitleAppearanceSettings): Boolean {
        val normalized = updated.normalized()
        if (normalized == draft) {
            return false
        }
        draft = normalized
        onSave(normalized)
        return true
    }
    suspend fun requestFocusWithFrames(target: FocusRequester) {
        repeat(3) {
            withFrameNanos {}
            runCatching { target.requestFocus() }.onSuccess { return }
        }
    }
    LaunchedEffect(Unit) {
        requestFocusWithFrames(closeFocusRequester)
    }
    val canResetAll = draft != defaults

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .background(colors.background, shape)
                .border(1.dp, colors.borderStrong, shape)
                .padding(20.dp)
        ) {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SUBTITLE APPEARANCE",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Live preview updates instantly as you change settings.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = AppTheme.fontFamily
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TopBarButton(
                            label = "RESET ALL",
                            onActivate = {
                                showResetAllConfirmation = true
                            },
                            enabled = canResetAll,
                            modifier = Modifier.focusRequester(resetAllFocusRequester),
                            onMoveDown = { enableCustomStyleFocusRequester.requestFocus() }
                        )
                        TopBarButton(
                            label = "CLOSE",
                            onActivate = onDismiss,
                            modifier = Modifier.focusRequester(closeFocusRequester),
                            onMoveLeft = if (canResetAll) ({ resetAllFocusRequester.requestFocus() }) else null,
                            onMoveDown = { enableCustomStyleFocusRequester.requestFocus() }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(colors.surface, panelShape)
                            .border(1.dp, colors.border, panelShape)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Style Controls",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                        SubtitleAppearanceToggleRow(
                            label = "Enable custom subtitle style",
                            checked = draft.customStyleEnabled,
                            focusRequester = enableCustomStyleFocusRequester
                        ) { checked ->
                            updateDraft(draft.copy(customStyleEnabled = checked))
                        }
                        SubtitleAppearanceControlWithReset(
                            canReset = draft.textSizeSp != defaults.textSizeSp,
                            onReset = { updateDraft(draft.copy(textSizeSp = defaults.textSizeSp)) }
                        ) { modifier ->
                            SubtitleAppearanceStepperRow(
                                label = "Text size",
                                value = "${draft.textSizeSp.roundToInt()}sp",
                                enabled = draft.customStyleEnabled,
                                onDecrease = {
                                    updateDraft(
                                        draft.copy(
                                            textSizeSp = (draft.textSizeSp - 1f)
                                                .coerceIn(SUBTITLE_TEXT_SIZE_MIN_SP, SUBTITLE_TEXT_SIZE_MAX_SP)
                                        )
                                    )
                                },
                                onIncrease = {
                                    updateDraft(
                                        draft.copy(
                                            textSizeSp = (draft.textSizeSp + 1f)
                                                .coerceIn(SUBTITLE_TEXT_SIZE_MIN_SP, SUBTITLE_TEXT_SIZE_MAX_SP)
                                        )
                                    )
                                },
                                modifier = modifier
                            )
                        }
                        SubtitleAppearanceControlWithReset(
                            canReset = draft.backgroundOpacityPercent != defaults.backgroundOpacityPercent,
                            onReset = {
                                updateDraft(
                                    draft.copy(backgroundOpacityPercent = defaults.backgroundOpacityPercent)
                                )
                            }
                        ) { modifier ->
                            SubtitleAppearanceStepperRow(
                                label = "Background opacity",
                                value = "${draft.backgroundOpacityPercent}%",
                                enabled = draft.customStyleEnabled,
                                onDecrease = {
                                    updateDraft(
                                        draft.copy(
                                            backgroundOpacityPercent = (draft.backgroundOpacityPercent - 10)
                                                .coerceIn(0, 100)
                                        )
                                    )
                                },
                                onIncrease = {
                                    updateDraft(
                                        draft.copy(
                                            backgroundOpacityPercent = (draft.backgroundOpacityPercent + 10)
                                                .coerceIn(0, 100)
                                        )
                                    )
                                },
                                modifier = modifier
                            )
                        }
                        SubtitleAppearanceControlWithReset(
                            canReset = draft.textColor != defaults.textColor,
                            onReset = { updateDraft(draft.copy(textColor = defaults.textColor)) }
                        ) { modifier ->
                            SubtitleAppearanceActionRow(
                                label = "Text color",
                                value = subtitleTextColorPresets.firstOrNull { it.color == draft.textColor }?.label ?: "Custom",
                                enabled = draft.customStyleEnabled,
                                onClick = {
                                    val currentIndex =
                                        subtitleTextColorPresets.indexOfFirst { it.color == draft.textColor }
                                            .takeIf { it >= 0 } ?: 0
                                    val nextIndex = (currentIndex + 1) % subtitleTextColorPresets.size
                                    updateDraft(draft.copy(textColor = subtitleTextColorPresets[nextIndex].color))
                                },
                                modifier = modifier
                            )
                        }
                        SubtitleAppearanceControlWithReset(
                            canReset = draft.edgeType != defaults.edgeType,
                            onReset = { updateDraft(draft.copy(edgeType = defaults.edgeType)) }
                        ) { modifier ->
                            SubtitleAppearanceActionRow(
                                label = "Edge style",
                                value = subtitleEdgeStyleLabel(draft.edgeType),
                                enabled = draft.customStyleEnabled,
                                onClick = {
                                    val styles = SubtitleEdgeStyle.entries
                                    val currentIndex =
                                        styles.indexOfFirst { it.value == draft.edgeType }
                                            .takeIf { it >= 0 } ?: 0
                                    val nextIndex = (currentIndex + 1) % styles.size
                                    updateDraft(draft.copy(edgeType = styles[nextIndex].value))
                                },
                                modifier = modifier
                            )
                        }
                        SubtitleAppearanceControlWithReset(
                            canReset = draft.bottomPaddingFraction != defaults.bottomPaddingFraction,
                            onReset = {
                                updateDraft(
                                    draft.copy(bottomPaddingFraction = defaults.bottomPaddingFraction)
                                )
                            }
                        ) { modifier ->
                            SubtitleAppearanceStepperRow(
                                label = "Vertical position",
                                value = "${(draft.bottomPaddingFraction * 100f).roundToInt()}%",
                                enabled = draft.customStyleEnabled,
                                onDecrease = {
                                    updateDraft(
                                        draft.copy(
                                            bottomPaddingFraction = (draft.bottomPaddingFraction - 0.02f)
                                                .coerceIn(SUBTITLE_BOTTOM_PADDING_MIN, SUBTITLE_BOTTOM_PADDING_MAX)
                                        )
                                    )
                                },
                                onIncrease = {
                                    updateDraft(
                                        draft.copy(
                                            bottomPaddingFraction = (draft.bottomPaddingFraction + 0.02f)
                                                .coerceIn(SUBTITLE_BOTTOM_PADDING_MIN, SUBTITLE_BOTTOM_PADDING_MAX)
                                        )
                                    )
                                },
                                modifier = modifier
                            )
                        }
                        SubtitleAppearanceToggleRow(
                            label = "Override embedded subtitle styles",
                            checked = draft.overrideEmbeddedStyles,
                            enabled = draft.customStyleEnabled
                        ) { checked ->
                            updateDraft(draft.copy(overrideEmbeddedStyles = checked))
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Live Preview",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(previewHeight)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF758495), Color(0xFF69798A))
                                    ),
                                    shape = panelShape
                                )
                                .border(1.dp, colors.border, panelShape)
                        ) {
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(previewHeight),
                                factory = { context ->
                                    SubtitleView(context).apply {
                                        // Keep preview rendering close to the real player subtitle view.
                                        setPadding(0, 0, 0, 0)
                                        setCues(sampleCues)
                                    }
                                },
                                update = { subtitleView ->
                                    subtitleView.setCues(sampleCues)
                                    if (draft.customStyleEnabled) {
                                        subtitleView.applySubtitleAppearanceSettings(draft)
                                    } else {
                                        subtitleView.setStyle(
                                            CaptionStyleCompat(
                                                Color.White.toArgb(),
                                                Color(0x8C000000).toArgb(),
                                                Color.Transparent.toArgb(),
                                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                                Color.Black.toArgb(),
                                                null
                                            )
                                        )
                                        subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                                        subtitleView.setApplyEmbeddedStyles(true)
                                        subtitleView.setApplyEmbeddedFontSizes(true)
                                        subtitleView.setBottomPaddingFraction(SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION)
                                    }
                                }
                            )
                        }
                        if (draft.customStyleEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(14.dp)
                                        .background(Color(draft.textColor))
                                        .border(1.dp, colors.borderStrong)
                                )
                                Text(
                                    text = "Current color: #${draft.textColor.toUInt().toString(16).uppercase()}",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = AppTheme.fontFamily
                                )
                            }
                        }
                        Text(
                            text = "Changes are saved automatically.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = AppTheme.fontFamily
                        )
                    }
                }
            }
        }
    }
    if (showResetAllConfirmation) {
        SubtitleAppearanceResetConfirmationDialog(
            onConfirm = {
                updateDraft(defaults)
                showResetAllConfirmation = false
            },
            onDismiss = { showResetAllConfirmation = false }
        )
    }
}

@Composable
private fun SubtitleAppearanceResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val confirmFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .background(colors.background, RoundedCornerShape(12.dp))
                .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Reset Subtitle Appearance",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Are you sure you want to reset all subtitle appearance settings to default values?",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DialogActionButton(
                        label = "Confirm",
                        focusRequester = confirmFocusRequester,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                    DialogActionButton(
                        label = "Cancel",
                        focusRequester = cancelFocusRequester,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleAppearanceControlWithReset(
    canReset: Boolean,
    onReset: () -> Unit,
    control: @Composable (Modifier) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        control(Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        FocusableButton(
            onClick = onReset,
            enabled = canReset,
            modifier = Modifier.width(72.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.surfaceAlt,
                contentColor = AppTheme.colors.textPrimary
            )
        ) {
            Text(
                "Reset",
                fontFamily = AppTheme.fontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SubtitleAppearanceToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor =
        if (isFocused) AppTheme.colors.focus else AppTheme.colors.border
    val toggle = { if (enabled) onCheckedChange(!checked) }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                )
                .focusable(interactionSource = interactionSource, enabled = enabled)
                .onKeyEvent { event ->
                    if (
                        enabled &&
                            event.type == KeyEventType.KeyDown &&
                            (event.key == Key.Enter ||
                                event.key == Key.NumPadEnter ||
                                event.key == Key.DirectionCenter)
                    ) {
                        toggle()
                        true
                    } else {
                        false
                    }
                }
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null
                ) { toggle() }
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.textTertiary,
            fontSize = 13.sp,
            fontFamily = AppTheme.fontFamily
        )
        Switch(
            checked = checked,
            enabled = enabled,
            // Keep switch visual; row is the single focus + toggle target for TV remotes.
            onCheckedChange = null
        )
    }
}

@Composable
private fun SubtitleAppearanceStepperRow(
    label: String,
    value: String,
    enabled: Boolean,
    onDecrease: () -> Boolean,
    onIncrease: () -> Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.textTertiary,
            fontSize = 13.sp,
            fontFamily = AppTheme.fontFamily,
            modifier = Modifier.weight(1f)
        )
        RepeatableFocusableButton(
            onStep = onDecrease,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.surfaceAlt,
                contentColor = AppTheme.colors.textPrimary
            )
        ) {
            Text("-", fontFamily = AppTheme.fontFamily, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = if (enabled) AppTheme.colors.textSecondary else AppTheme.colors.textTertiary,
            fontSize = 12.sp,
            fontFamily = AppTheme.fontFamily
        )
        Spacer(modifier = Modifier.width(8.dp))
        RepeatableFocusableButton(
            onStep = onIncrease,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.surfaceAlt,
                contentColor = AppTheme.colors.textPrimary
            )
        ) {
            Text("+", fontFamily = AppTheme.fontFamily, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SubtitleAppearanceActionRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.textTertiary,
            fontSize = 13.sp,
            fontFamily = AppTheme.fontFamily,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = if (enabled) AppTheme.colors.textSecondary else AppTheme.colors.textTertiary,
            fontSize = 12.sp,
            fontFamily = AppTheme.fontFamily
        )
        Spacer(modifier = Modifier.width(10.dp))
        FocusableButton(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.surfaceAlt,
                contentColor = AppTheme.colors.textPrimary
            )
        ) {
            Text("Change", fontFamily = AppTheme.fontFamily, fontSize = 12.sp)
        }
    }
}

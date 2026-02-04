package com.example.xtreamplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.ui.theme.AppTheme

internal val NAV_WIDTH = 220.dp

@Composable
internal fun SideNav(
    selectedSection: Section,
    onSectionSelected: (Section) -> Unit,
    onMoveRight: () -> Unit,
    expanded: Boolean,
    layoutExpanded: Boolean,
    allNavItemFocusRequester: FocusRequester,
    continueWatchingNavItemFocusRequester: FocusRequester,
    favoritesNavItemFocusRequester: FocusRequester,
    moviesNavItemFocusRequester: FocusRequester,
    seriesNavItemFocusRequester: FocusRequester,
    liveNavItemFocusRequester: FocusRequester,
    categoriesNavItemFocusRequester: FocusRequester,
    localFilesNavItemFocusRequester: FocusRequester,
    settingsNavItemFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val scrollState = rememberScrollState()

    val items = remember(
        allNavItemFocusRequester,
        continueWatchingNavItemFocusRequester,
        favoritesNavItemFocusRequester,
        moviesNavItemFocusRequester,
        seriesNavItemFocusRequester,
        liveNavItemFocusRequester,
        categoriesNavItemFocusRequester,
        localFilesNavItemFocusRequester,
        settingsNavItemFocusRequester
    ) {
        listOf(
            NavEntry("All", Section.ALL, allNavItemFocusRequester),
            NavEntry(
                "Continue Watching",
                Section.CONTINUE_WATCHING,
                continueWatchingNavItemFocusRequester
            ),
            NavEntry("Favorites", Section.FAVORITES, favoritesNavItemFocusRequester),
            NavEntry("Movies", Section.MOVIES, moviesNavItemFocusRequester),
            NavEntry("Series", Section.SERIES, seriesNavItemFocusRequester),
            NavEntry("Live", Section.LIVE, liveNavItemFocusRequester),
            NavEntry("Categories", Section.CATEGORIES, categoriesNavItemFocusRequester),
            NavEntry(
                "Play Local Files",
                Section.LOCAL_FILES,
                localFilesNavItemFocusRequester
            ),
            NavEntry("Settings", Section.SETTINGS, settingsNavItemFocusRequester)
        )
    }

    val navWidth = if (layoutExpanded) NAV_WIDTH else 0.dp
    Box(modifier = modifier.fillMaxHeight().width(navWidth).clipToBounds()) {
        Column(
            modifier =
                Modifier.fillMaxHeight()
                    .width(NAV_WIDTH)
                    .verticalScroll(scrollState)
                    .background(colors.panelBackground)
                    .border(1.dp, colors.panelBorder)
                    .padding(horizontal = 18.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items.forEach { item ->
                NavItem(
                    label = item.label,
                    isSelected = selectedSection == item.section,
                    onClick = { onSectionSelected(item.section) },
                    focusRequester = item.focusRequester,
                    onMoveRight = onMoveRight,
                    enabled = expanded
                )
            }
        }
    }
}

@Composable
internal fun MenuButton(expanded: Boolean, onToggle: () -> Unit, onMoveRight: (() -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val label = if (expanded) "CLOSE" else "MENU"
    val shape = remember { RoundedCornerShape(12.dp) }
    val colors = AppTheme.colors
    val borderColor = if (isFocused) colors.focus else colors.borderStrong
    val focusedBrush = remember(colors.accent, colors.accentAlt) {
        Brush.horizontalGradient(colors = listOf(colors.accent, colors.accentAlt))
    }
    val unfocusedBrush = remember(colors.accentMutedAlt, colors.surfaceAlt) {
        Brush.horizontalGradient(colors = listOf(colors.accentMutedAlt, colors.surfaceAlt))
    }
    val buttonBrush = if (isFocused) focusedBrush else unfocusedBrush

    Box(
        modifier =
            Modifier.width(140.dp)
                .height(46.dp)
                .focusable(interactionSource = interactionSource)
                .onKeyEvent {
                    if (it.type != KeyEventType.KeyDown) {
                        false
                    } else if (it.key == Key.DirectionRight && onMoveRight != null) {
                        onMoveRight()
                        true
                    } else if (it.key == Key.Enter ||
                        it.key == Key.NumPadEnter ||
                        it.key == Key.DirectionCenter
                    ) {
                        onToggle()
                        true
                    } else {
                        false
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                )
                .background(brush = buttonBrush, shape = shape)
                .border(width = 1.dp, color = borderColor, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier =
                            Modifier.width(18.dp)
                                .height(2.dp)
                                .background(
                                    color = if (isFocused) colors.textOnAccent else colors.textPrimary,
                                    shape = RoundedCornerShape(2.dp)
                                )
                    )
                }
            }
            androidx.compose.material3.Text(
                text = label,
                color = if (isFocused) colors.textOnAccent else colors.textPrimary,
                fontSize = 16.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    focusRequester: FocusRequester,
    onMoveRight: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var longPressTriggered by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val colors = AppTheme.colors
    val borderColor =
        when {
            isFocused -> colors.focus
            isSelected -> colors.accentSelected
            else -> colors.border
        }
    val selectedTextColor =
        bestContrastText(colors.accentSelected, colors.textPrimary, colors.textOnAccent)
    val textColor =
        when {
            isFocused -> colors.textOnAccent
            isSelected -> selectedTextColor
            else -> colors.navText
        }
    val backgroundBrush =
        when {
            isFocused ->
                Brush.horizontalGradient(
                    colors = listOf(colors.accent, colors.accentAlt)
                )
            isSelected ->
                Brush.horizontalGradient(
                    colors = listOf(colors.accentSelected, colors.accentSelectedAlt)
                )
            else ->
                Brush.horizontalGradient(
                    colors = listOf(colors.accentMutedAlt, colors.surfaceAlt)
                )
        }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(58.dp)
                .focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .then(
                    if (enabled) {
                        Modifier.focusable(interactionSource = interactionSource)
                            .onKeyEvent {
                                if (it.type != KeyEventType.KeyDown) {
                                    false
                                } else if (it.key == Key.DirectionRight) {
                                    onMoveRight()
                                    true
                                } else if (it.key == Key.Enter ||
                                    it.key == Key.NumPadEnter ||
                                    it.key == Key.DirectionCenter
                                ) {
                                    onClick()
                                    true
                                } else {
                                    false
                                }
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = onClick
                            )
                    } else {
                        Modifier
                    }
                )
                .background(brush = backgroundBrush, shape = shape)
                .border(width = 1.dp, color = borderColor, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier =
                    Modifier.align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .width(4.dp)
                        .height(28.dp)
                        .background(
                            color = if (isFocused) colors.textPrimary else colors.accentAlt,
                            shape = RoundedCornerShape(4.dp)
                        )
            )
        }
        androidx.compose.material3.Text(
            text = label,
            color = textColor,
            fontSize = 18.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private data class NavEntry(val label: String, val section: Section, val focusRequester: FocusRequester)

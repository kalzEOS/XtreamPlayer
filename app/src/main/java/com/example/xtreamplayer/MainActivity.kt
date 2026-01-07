package com.example.xtreamplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.xtreamplayer.ui.theme.XtreamPlayerTheme
import com.example.xtreamplayer.settings.PlaybackSettingsController
import com.example.xtreamplayer.settings.SettingsState
import com.example.xtreamplayer.settings.SettingsViewModel
import com.example.xtreamplayer.settings.SettingsViewModelFactory
import com.example.xtreamplayer.player.Media3PlaybackEngine
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

private const val DEMO_VIDEO_URL =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XtreamPlayerTheme {
                RootScreen()
            }
        }
    }
}

@Composable
fun RootScreen() {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext)
    )
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val playbackSettingsController = remember { PlaybackSettingsController() }
    val playbackEngine = remember { Media3PlaybackEngine(context.applicationContext) }

    var selectedSection by remember { mutableStateOf(Section.ALL) }
    var navExpanded by remember { mutableStateOf(true) }

    var moveFocusToContent by remember { mutableStateOf(false) }
    var moveFocusToNav by remember { mutableStateOf(false) }

    val allNavItemFocusRequester = remember { FocusRequester() }
    val moviesNavItemFocusRequester = remember { FocusRequester() }
    val seriesNavItemFocusRequester = remember { FocusRequester() }
    val liveNavItemFocusRequester = remember { FocusRequester() }
    val categoriesNavItemFocusRequester = remember { FocusRequester() }
    val settingsNavItemFocusRequester = remember { FocusRequester() }
    val contentItemFocusRequester = remember { FocusRequester() }

    // Move focus INTO content safely
    LaunchedEffect(moveFocusToContent) {
        if (moveFocusToContent) {
            contentItemFocusRequester.requestFocus()
            moveFocusToContent = false
        }
    }

    // Move focus BACK to nav safely
    LaunchedEffect(moveFocusToNav) {
        if (moveFocusToNav) {
            when (selectedSection) {
                Section.ALL -> allNavItemFocusRequester.requestFocus()
                Section.MOVIES -> moviesNavItemFocusRequester.requestFocus()
                Section.SERIES -> seriesNavItemFocusRequester.requestFocus()
                Section.LIVE -> liveNavItemFocusRequester.requestFocus()
                Section.CATEGORIES -> categoriesNavItemFocusRequester.requestFocus()
                Section.SETTINGS -> settingsNavItemFocusRequester.requestFocus()
            }
            moveFocusToNav = false
        }
    }

    DisposableEffect(playbackEngine) {
        playbackSettingsController.bind(playbackEngine)
        playbackEngine.setMedia(Uri.parse(DEMO_VIDEO_URL))
        onDispose {
            playbackSettingsController.unbind(playbackEngine)
            playbackEngine.release()
        }
    }

    LaunchedEffect(settings) {
        playbackSettingsController.apply(settings)
    }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(start = 24.dp, top = 24.dp)
            ) {
                MenuButton(
                    expanded = navExpanded,
                    onToggle = { navExpanded = !navExpanded }
                )
            }

            Row(modifier = Modifier.fillMaxSize()) {

                SideNav(
                    selectedSection = selectedSection,
                    onSectionSelected = { selectedSection = it },
                    onMoveRight = { moveFocusToContent = true },
                    expanded = navExpanded,
                    allNavItemFocusRequester = allNavItemFocusRequester,
                    moviesNavItemFocusRequester = moviesNavItemFocusRequester,
                    seriesNavItemFocusRequester = seriesNavItemFocusRequester,
                    liveNavItemFocusRequester = liveNavItemFocusRequester,
                    categoriesNavItemFocusRequester = categoriesNavItemFocusRequester,
                    settingsNavItemFocusRequester = settingsNavItemFocusRequester
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    val sectionTitle = when (selectedSection) {
                        Section.ALL -> "ALL CONTENT"
                        Section.MOVIES -> "MOVIES CONTENT"
                        Section.SERIES -> "SERIES CONTENT"
                        Section.LIVE -> "LIVE CONTENT"
                        Section.CATEGORIES -> "CATEGORIES CONTENT"
                        Section.SETTINGS -> "SETTINGS"
                    }

                    val handleMoveLeft = {
                        if (!navExpanded) {
                            navExpanded = true
                        }
                        moveFocusToNav = true
                    }

                    if (selectedSection == Section.SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            contentItemFocusRequester = contentItemFocusRequester,
                            onMoveLeft = handleMoveLeft,
                            onToggleAutoPlay = settingsViewModel::toggleAutoPlayNext,
                            onToggleSubtitles = settingsViewModel::toggleSubtitles,
                            onToggleMatchFrameRate = settingsViewModel::toggleMatchFrameRate,
                            onCyclePlaybackQuality = settingsViewModel::cyclePlaybackQuality,
                            onCycleAudioLanguage = settingsViewModel::cycleAudioLanguage,
                            onToggleWifiOnly = settingsViewModel::toggleWifiOnlyStreaming,
                            onToggleDataSaver = settingsViewModel::toggleDataSaver,
                            onToggleRememberLogin = settingsViewModel::toggleRememberLogin,
                            onToggleAutoSignIn = settingsViewModel::toggleAutoSignIn,
                            onToggleParentalPin = settingsViewModel::toggleParentalPin,
                            onCycleParentalRating = settingsViewModel::cycleParentalRating
                        )
                    } else {
                        SectionScreen(
                            title = sectionTitle,
                            player = playbackEngine.player,
                            contentItemFocusRequester = contentItemFocusRequester,
                            onMoveLeft = handleMoveLeft
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0F1A),
                        Color(0xFF141B2C)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(260.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x332B5BFF),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}


enum class Section {
    ALL,
    MOVIES,
    SERIES,
    LIVE,
    CATEGORIES,
    SETTINGS
}

@Composable
fun SideNav(
    selectedSection: Section,
    onSectionSelected: (Section) -> Unit,
    onMoveRight: () -> Unit,
    expanded: Boolean,
    allNavItemFocusRequester: FocusRequester,
    moviesNavItemFocusRequester: FocusRequester,
    seriesNavItemFocusRequester: FocusRequester,
    liveNavItemFocusRequester: FocusRequester,
    categoriesNavItemFocusRequester: FocusRequester,
    settingsNavItemFocusRequester: FocusRequester
) {
    val navWidth by animateDpAsState(
        targetValue = if (expanded) 220.dp else 0.dp
    )
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val navAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 140)
    )
    val navPadding = if (expanded) 18.dp else 0.dp
    val scrollState = rememberScrollState()

    val items = listOf(
        NavEntry("All", Section.ALL, allNavItemFocusRequester),
        NavEntry("Movies", Section.MOVIES, moviesNavItemFocusRequester),
        NavEntry("Series", Section.SERIES, seriesNavItemFocusRequester),
        NavEntry("Live", Section.LIVE, liveNavItemFocusRequester),
        NavEntry("Categories", Section.CATEGORIES, categoriesNavItemFocusRequester),
        NavEntry("Settings", Section.SETTINGS, settingsNavItemFocusRequester)
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(navWidth)
            .graphicsLayer { alpha = navAlpha }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1626),
                            Color(0xFF162236)
                        )
                    )
                )
                .border(1.dp, Color(0xFF1F2B3E))
                .padding(horizontal = navPadding, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items.forEach { item ->
                NavItem(
                    label = item.label,
                    requestFocus = selectedSection == item.section,
                    isSelected = selectedSection == item.section,
                    onClick = { onSectionSelected(item.section) },
                    focusRequester = item.focusRequester,
                    onMoveRight = onMoveRight,
                    pulse = pulse,
                    enabled = expanded
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC0F1626),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xCC162236)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(2.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x553B7BFF),
                            Color.Transparent,
                            Color(0x553B7BFF)
                        )
                    )
                )
        )
    }
}

data class NavEntry(
    val label: String,
    val section: Section,
    val focusRequester: FocusRequester
)

@Composable
fun NavItem(
    label: String,
    requestFocus: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    focusRequester: FocusRequester,
    onMoveRight: () -> Unit,
    pulse: Float,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = tween(durationMillis = 120)
    )
    val glowElevation by animateDpAsState(
        targetValue = if (isFocused) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 120)
    )
    val borderColor = when {
        isFocused -> Color(0xFFB6D9FF)
        isSelected -> Color(0xFF42539A)
        else -> Color(0xFF1E2533)
    }
    val textColor = when {
        isFocused -> Color(0xFF0C1730)
        isSelected -> Color(0xFFE3EDFF)
        else -> Color(0xFFD5DAE6)
    }
    val backgroundBrush = when {
        isFocused -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF4F8CFF),
                Color(0xFF7FCBFF)
            )
        )
        isSelected -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF273479),
                Color(0xFF1E275C)
            )
        )
        else -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2B3240),
                Color(0xFF222833)
            )
        )
    }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .focusRequester(focusRequester)
            .bringIntoViewRequester(bringIntoViewRequester)
            .then(
                if (enabled) {
                    Modifier
                        .focusable(interactionSource = interactionSource)
                        .onKeyEvent {
                            if (it.type != KeyEventType.KeyDown) {
                                false
                            } else if (it.key == Key.DirectionRight) {
                                onMoveRight()
                                true
                            } else if (
                                it.key == Key.Enter ||
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
            .shadow(if (isFocused) glowElevation * pulse else glowElevation, shape)
            .graphicsLayer {
                val appliedScale = if (isFocused) focusScale * pulse else focusScale
                scaleX = appliedScale
                scaleY = appliedScale
            }
            .background(
                brush = backgroundBrush,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ){
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .width(4.dp)
                    .height(28.dp)
                    .background(
                        color = if (isFocused) Color(0xFFEAF2FF) else Color(0xFF8EA2E8),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MenuButton(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val label = if (expanded) "CLOSE" else "MENU"
    val shape = RoundedCornerShape(12.dp)
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = tween(durationMillis = 120)
    )
    val glowElevation by animateDpAsState(
        targetValue = if (isFocused) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 120)
    )
    val borderColor = if (isFocused) Color(0xFFB6D9FF) else Color(0xFF2A3348)
    val buttonBrush = if (isFocused) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF4F8CFF),
                Color(0xFF7FCBFF)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2C3550),
                Color(0xFF1E2438)
            )
        )
    }

    Box(
        modifier = Modifier
            .width(140.dp)
            .height(46.dp)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown &&
                    (it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.DirectionCenter)
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
            .shadow(if (isFocused) glowElevation * pulse else glowElevation, shape)
            .graphicsLayer {
                val appliedScale = if (isFocused) focusScale * pulse else focusScale
                scaleX = appliedScale
                scaleY = appliedScale
            }
            .background(
                brush = buttonBrush,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(
                                color = if (isFocused) Color(0xFF0C1730) else Color.White,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            Text(
                text = label,
                color = if (isFocused) Color(0xFF0C1730) else Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SectionScreen(
    title: String,
    player: ExoPlayer,
    contentItemFocusRequester: FocusRequester,
    onMoveLeft: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.99f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = tween(durationMillis = 140)
    )
    val glowElevation by animateDpAsState(
        targetValue = if (isFocused) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 140)
    )
    val borderColor = if (isFocused) Color(0xFFB6D9FF) else Color(0xFF2A3348)

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(16f / 9f)
                .focusRequester(contentItemFocusRequester)
                .focusable(interactionSource = interactionSource)
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown &&
                        it.key == Key.DirectionLeft
                    ) {
                        onMoveLeft()
                        true
                    } else {
                        false
                    }
                }
                .shadow(glowElevation, shape)
                .graphicsLayer {
                    val appliedScale = if (isFocused) focusScale * pulse else focusScale
                    scaleX = appliedScale
                    scaleY = appliedScale
                }
                .clip(shape)
                .background(Color(0xFF0C121F))
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        isFocusable = false
                    }
                },
                update = { view ->
                    view.player = player
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x660A0F1A)
                            )
                        )
                    )
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun SettingsScreen(
    settings: SettingsState,
    contentItemFocusRequester: FocusRequester,
    onMoveLeft: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleMatchFrameRate: () -> Unit,
    onCyclePlaybackQuality: () -> Unit,
    onCycleAudioLanguage: () -> Unit,
    onToggleWifiOnly: () -> Unit,
    onToggleDataSaver: () -> Unit,
    onToggleRememberLogin: () -> Unit,
    onToggleAutoSignIn: () -> Unit,
    onToggleParentalPin: () -> Unit,
    onCycleParentalRating: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 48.dp, top = 16.dp, end = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SETTINGS",
                color = Color.White,
                fontSize = 26.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionHeader(title = "Playback")
            SettingsItem(
                title = "Autoplay Next",
                value = if (settings.autoPlayNext) "On" else "Off",
                focusRequester = contentItemFocusRequester,
                onActivate = onToggleAutoPlay,
                onMoveLeft = onMoveLeft
            )
            SettingsItem(
                title = "Playback Quality",
                value = settings.playbackQuality.label,
                onActivate = onCyclePlaybackQuality,
                onMoveLeft = onMoveLeft
            )
            SettingsItem(
                title = "Match Frame Rate",
                value = if (settings.matchFrameRate) "On" else "Off",
                onActivate = onToggleMatchFrameRate,
                onMoveLeft = onMoveLeft
            )
            Spacer(modifier = Modifier.height(4.dp))
            SettingsSectionHeader(title = "Audio & Subtitles")
            SettingsItem(
                title = "Subtitles",
                value = if (settings.subtitlesEnabled) "On" else "Off",
                onActivate = onToggleSubtitles,
                onMoveLeft = onMoveLeft
            )
            SettingsItem(
                title = "Audio Language",
                value = settings.audioLanguage.label,
                onActivate = onCycleAudioLanguage,
                onMoveLeft = onMoveLeft
            )
            Spacer(modifier = Modifier.height(4.dp))
            SettingsSectionHeader(title = "Network")
            SettingsItem(
                title = "Wi-Fi Only Streaming",
                value = if (settings.wifiOnlyStreaming) "On" else "Off",
                onActivate = onToggleWifiOnly,
                onMoveLeft = onMoveLeft
            )
            SettingsItem(
                title = "Data Saver",
                value = if (settings.dataSaverEnabled) "On" else "Off",
                onActivate = onToggleDataSaver,
                onMoveLeft = onMoveLeft
            )
            Spacer(modifier = Modifier.height(4.dp))
            SettingsSectionHeader(title = "Account")
            SettingsItem(
                title = "Remember Login",
                value = if (settings.rememberLogin) "On" else "Off",
                onActivate = onToggleRememberLogin,
                onMoveLeft = onMoveLeft
            )
            SettingsItem(
                title = "Auto Sign-In",
                value = if (settings.autoSignIn) "On" else "Off",
                onActivate = onToggleAutoSignIn,
                onMoveLeft = onMoveLeft
            )
            Spacer(modifier = Modifier.height(4.dp))
            SettingsSectionHeader(title = "Parental Controls")
            SettingsItem(
                title = "Parental PIN",
                value = if (settings.parentalPinEnabled) "On" else "Off",
                onActivate = onToggleParentalPin,
                onMoveLeft = onMoveLeft
            )
            SettingsItem(
                title = "Rating Limit",
                value = settings.parentalRating.label,
                onActivate = onCycleParentalRating,
                onMoveLeft = onMoveLeft
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC0A0F1A),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xCC141B2C)
                        )
                    )
                )
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFF9FB0D4),
        fontSize = 13.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    value: String,
    onActivate: () -> Unit,
    onMoveLeft: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val borderColor = if (isFocused) Color(0xFFB6D9FF) else Color(0xFF1E2533)
    val textColor = if (isFocused) Color(0xFF0C1730) else Color(0xFFE7ECF7)
    val valueColor = if (isFocused) Color(0xFF0C1730) else Color(0xFFAFC0E6)
    val backgroundBrush = if (isFocused) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF4F8CFF),
                Color(0xFF7FCBFF)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2B3240),
                Color(0xFF222833)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    false
                } else if (it.key == Key.DirectionLeft) {
                    onMoveLeft()
                    true
                } else if (
                    it.key == Key.Enter ||
                    it.key == Key.NumPadEnter ||
                    it.key == Key.DirectionCenter
                ) {
                    onActivate()
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onActivate
            )
            .background(
                brush = backgroundBrush,
                shape = shape
            )
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium
            )
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            bringIntoViewRequester.bringIntoView()
        }
    }
}

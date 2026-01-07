package com.example.xtreamplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.FilterQuality
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.xtreamplayer.api.XtreamApi
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.auth.AuthViewModel
import com.example.xtreamplayer.auth.AuthViewModelFactory
import com.example.xtreamplayer.content.CategoryItem
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.content.ContentCache
import com.example.xtreamplayer.content.ContentRepository
import com.example.xtreamplayer.content.ContentType
import com.example.xtreamplayer.content.SearchNormalizer
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.media3.common.Player
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.graphics.Color as AndroidColor
import com.example.xtreamplayer.player.XtreamPlayerView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.res.painterResource
import com.example.xtreamplayer.R

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
    val coroutineScope = rememberCoroutineScope()
    val playbackSettingsController = remember { PlaybackSettingsController() }
    val playbackEngine = remember { Media3PlaybackEngine(context.applicationContext) }
    val api = remember { XtreamApi() }
    val contentCache = remember { ContentCache(context.applicationContext) }
    val contentRepository = remember { ContentRepository(api, contentCache) }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context.applicationContext, api)
    )
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val savedConfig by authViewModel.savedConfig.collectAsStateWithLifecycle()

    var selectedSection by remember { mutableStateOf(Section.ALL) }
    var navExpanded by remember { mutableStateOf(true) }
    var showManageLists by remember { mutableStateOf(false) }
    var activePlayback by remember { mutableStateOf<PlaybackItem?>(null) }
    var resumeFocusId by remember { mutableStateOf<String?>(null) }
    val resumeFocusRequester = remember { FocusRequester() }

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

    LaunchedEffect(settings.autoSignIn, settings.rememberLogin, savedConfig) {
        authViewModel.tryAutoSignIn(settings)
    }

    LaunchedEffect(authState.activeConfig?.username, authState.activeConfig?.baseUrl) {
        contentRepository.clearCache()
        coroutineScope.launch {
            contentRepository.clearDiskCache()
        }
    }

    LaunchedEffect(authState.isSignedIn) {
        if (authState.isSignedIn) {
            navExpanded = true
            moveFocusToNav = true
            showManageLists = false
        }
    }

    LaunchedEffect(selectedSection) {
        if (selectedSection != Section.SETTINGS) {
            showManageLists = false
        }
    }

    LaunchedEffect(showManageLists) {
        if (showManageLists) {
            moveFocusToContent = true
        }
    }

    LaunchedEffect(activePlayback?.uri) {
        if (activePlayback != null) {
            playbackEngine.setMedia(activePlayback!!.uri)
            playbackEngine.player.playWhenReady = true
        } else {
            playbackEngine.player.stop()
            if (resumeFocusId != null) {
                resumeFocusRequester.requestFocus()
            }
        }
    }

    val handleItemFocused: (ContentItem) -> Unit = { item ->
        resumeFocusId = item.id
    }

    val handlePlayItem: (ContentItem) -> Unit = { item ->
        val config = authState.activeConfig
        if (config != null) {
            resumeFocusId = item.id
            val url = StreamUrlBuilder.buildUrl(
                config = config,
                type = item.contentType,
                streamId = item.streamId,
                extension = item.containerExtension
            )
            activePlayback = PlaybackItem(
                uri = Uri.parse(url),
                title = item.title,
                type = item.contentType
            )
        }
    }

    DisposableEffect(playbackEngine) {
        playbackSettingsController.bind(playbackEngine)
        onDispose {
            playbackSettingsController.unbind(playbackEngine)
            playbackEngine.release()
        }
    }

    LaunchedEffect(settings) {
        playbackSettingsController.apply(settings)
    }

    AppBackground {
        val showAuthLoading = !authState.isSignedIn &&
            authState.errorMessage == null &&
            (authState.isLoading ||
                (settings.autoSignIn && settings.rememberLogin && savedConfig != null))

        if (showAuthLoading) {
            AuthLoadingScreen()
        } else if (authState.isSignedIn) {
            Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(start = 20.dp, top = 12.dp)
            ) {
                MenuButton(
                    expanded = navExpanded,
                    onToggle = { navExpanded = !navExpanded }
                )
                }

                Row(modifier = Modifier.fillMaxSize()) {

                    SideNav(
                        selectedSection = selectedSection,
                        onSectionSelected = {
                            selectedSection = it
                            moveFocusToContent = true
                        },
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
                            if (showManageLists) {
                                ManageListsScreen(
                                    savedConfig = savedConfig,
                                    activeConfig = authState.activeConfig,
                                    contentItemFocusRequester = contentItemFocusRequester,
                                    onMoveLeft = handleMoveLeft,
                                    onBack = { showManageLists = false },
                                    onEditList = {
                                        showManageLists = false
                                        contentRepository.clearCache()
                                        coroutineScope.launch {
                                            contentRepository.clearDiskCache()
                                        }
                                        authViewModel.enterEditMode()
                                    },
                                    onSignOut = {
                                        showManageLists = false
                                        contentRepository.clearCache()
                                        coroutineScope.launch {
                                            contentRepository.clearDiskCache()
                                        }
                                        authViewModel.signOut(keepSaved = true)
                                    },
                                    onForgetList = {
                                        showManageLists = false
                                        contentRepository.clearCache()
                                        coroutineScope.launch {
                                            contentRepository.clearDiskCache()
                                        }
                                        authViewModel.signOut(keepSaved = false)
                                    }
                                )
                            } else {
                                val activeListName = authState.activeConfig?.listName
                                    ?: savedConfig?.listName
                                    ?: "Not set"
                                SettingsScreen(
                                    settings = settings,
                                    activeListName = activeListName,
                                    contentItemFocusRequester = contentItemFocusRequester,
                                    onMoveLeft = handleMoveLeft,
                                    onToggleAutoPlay = settingsViewModel::toggleAutoPlayNext,
                                    onToggleSubtitles = settingsViewModel::toggleSubtitles,
                                    onToggleMatchFrameRate = settingsViewModel::toggleMatchFrameRate,
                                    onCyclePlaybackQuality = settingsViewModel::cyclePlaybackQuality,
                                    onCycleAudioLanguage = settingsViewModel::cycleAudioLanguage,
                                    onToggleRememberLogin = settingsViewModel::toggleRememberLogin,
                                    onToggleAutoSignIn = settingsViewModel::toggleAutoSignIn,
                                    onToggleParentalPin = settingsViewModel::toggleParentalPin,
                                    onCycleParentalRating = settingsViewModel::cycleParentalRating,
                                    onManageLists = { showManageLists = true },
                                    onRefreshContent = {
                                        contentRepository.clearCache()
                                        coroutineScope.launch {
                                            contentRepository.clearDiskCache()
                                        }
                                    },
                                    onSignOut = {
                                        contentRepository.clearCache()
                                        coroutineScope.launch {
                                            contentRepository.clearDiskCache()
                                        }
                                        authViewModel.signOut(keepSaved = true)
                                    }
                                )
                            }
                        } else {
                            val activeConfig = authState.activeConfig
                            if (activeConfig != null) {
                                if (selectedSection == Section.CATEGORIES) {
                                    CategorySectionScreen(
                                        title = sectionTitle,
                                        contentRepository = contentRepository,
                                        authConfig = activeConfig,
                                        contentItemFocusRequester = contentItemFocusRequester,
                                        resumeFocusId = resumeFocusId,
                                        resumeFocusRequester = resumeFocusRequester,
                                        onItemFocused = handleItemFocused,
                                        onPlay = handlePlayItem,
                                        onMoveLeft = handleMoveLeft
                                    )
                                } else {
                                    SectionScreen(
                                        title = sectionTitle,
                                        section = selectedSection,
                                        contentRepository = contentRepository,
                                        authConfig = activeConfig,
                                        contentItemFocusRequester = contentItemFocusRequester,
                                        resumeFocusId = resumeFocusId,
                                        resumeFocusRequester = resumeFocusRequester,
                                        onItemFocused = handleItemFocused,
                                        onPlay = handlePlayItem,
                                        onMoveLeft = handleMoveLeft
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LoginScreen(
                authState = authState,
                initialConfig = savedConfig,
                onSignIn = { listName, baseUrl, username, password ->
                    authViewModel.signIn(
                        listName = listName,
                        baseUrl = baseUrl,
                        username = username,
                        password = password,
                        rememberLogin = settings.rememberLogin
                    )
                }
            )
        }

        if (activePlayback != null) {
            PlayerOverlay(
                title = activePlayback!!.title,
                player = playbackEngine.player,
                onExit = { activePlayback = null }
            )
        }
    }
}

@Composable
private fun AuthLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "XTREAM PLAYER",
            color = Color.White,
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerOverlay(
    title: String,
    player: Player,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    var controlsVisible by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(PlayerResizeMode.FIT) }
    var playerView by remember { mutableStateOf<XtreamPlayerView?>(null) }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        if (activity != null) {
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
            onDispose {
                controller.show(WindowInsetsCompat.Type.statusBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            playerView?.resetControllerFocus()
            focusRequester.requestFocus()
        }
    }

    BackHandler(enabled = true) {
        val dismissed = playerView?.dismissSettingsWindowIfShowing() == true
        if (!dismissed) {
            onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource),
            factory = { context ->
                XtreamPlayerView(context).apply {
                    this.player = player
                    useController = true
                    controllerAutoShow = false
                    setControllerShowTimeoutMs(3000)
                    setShutterBackgroundColor(AndroidColor.BLACK)
                    setBackgroundColor(AndroidColor.BLACK)
                    this.resizeMode = resizeMode.resizeMode
                    forcedAspectRatio = resizeMode.forcedAspectRatio
                    setResizeModeLabel(resizeMode.label)
                    onResizeModeClick = { resizeMode = nextResizeMode(resizeMode) }
                    onBackClick = onExit
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            val visible = visibility == View.VISIBLE
                            controlsVisible = visible
                            if (visible) {
                                focusPlayPause()
                            } else {
                                resetControllerFocus()
                            }
                        }
                    )
                    setOnKeyListener { _, _, event ->
                        if (event.action != KeyEvent.ACTION_DOWN) {
                            return@setOnKeyListener false
                        }
                        if (event.keyCode == KeyEvent.KEYCODE_BACK ||
                            event.keyCode == KeyEvent.KEYCODE_ESCAPE
                        ) {
                            val dismissed = dismissSettingsWindowIfShowing()
                            if (!dismissed) {
                                onExit()
                            }
                            return@setOnKeyListener true
                        }
                        val isSelect =
                            event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                event.keyCode == KeyEvent.KEYCODE_ENTER ||
                                event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                        if (isSelect && !isControllerFullyVisible()) {
                            showController()
                            return@setOnKeyListener true
                        }
                        false
                    }
                    playerView = this
                }
            },
            update = { view ->
                view.player = player
                view.resizeMode = resizeMode.resizeMode
                view.forcedAspectRatio = resizeMode.forcedAspectRatio
                view.setResizeModeLabel(resizeMode.label)
                view.onResizeModeClick = { resizeMode = nextResizeMode(resizeMode) }
                view.onBackClick = onExit
                if (playerView != view) {
                    playerView = view
                }
            }
        )
        if (controlsVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private enum class PlayerResizeMode(
    val label: String,
    val resizeMode: Int,
    val forcedAspectRatio: Float?
) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT, null),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL, null),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM, null),
    ONE_TO_ONE("1:1", AspectRatioFrameLayout.RESIZE_MODE_FIT, 1f)
}

private fun nextResizeMode(current: PlayerResizeMode): PlayerResizeMode {
    val values = PlayerResizeMode.values()
    return values[(current.ordinal + 1) % values.size]
}

private fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
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
    val navWidth = if (expanded) 220.dp else 0.dp
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
    ) {
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .background(Color(0xFF111826))
                    .border(1.dp, Color(0xFF1F2B3E))
                    .padding(horizontal = navPadding, vertical = 28.dp),
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
}

data class NavEntry(
    val label: String,
    val section: Section,
    val focusRequester: FocusRequester
)

@Composable
fun NavItem(
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    focusRequester: FocusRequester,
    onMoveRight: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
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

@Composable
private fun ContentCard(
    item: ContentItem?,
    focusRequester: FocusRequester?,
    isLeftEdge: Boolean,
    onActivate: (() -> Unit)?,
    onFocused: ((ContentItem) -> Unit)?,
    onMoveLeft: () -> Unit,
    onMoveUp: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isFocused) Color(0xFFB6D9FF) else Color(0xFF2A3348)
    val backgroundColor = if (isFocused) Color(0xFF222E44) else Color(0xFF161E2E)
    val title = item?.title ?: "Loading..."
    val subtitle = item?.subtitle ?: "Please wait"
    val imageUrl = item?.imageUrl
    val context = LocalContext.current
    val imageRequest = remember(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            null
        } else {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .size(600)
                .build()
        }
    }
    LaunchedEffect(isFocused, item?.id) {
        if (isFocused && item != null) {
            onFocused?.invoke(item)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    false
                } else if (isLeftEdge && it.key == Key.DirectionLeft) {
                    onMoveLeft()
                    true
                } else if (onMoveUp != null && it.key == Key.DirectionUp) {
                    onMoveUp()
                    true
                } else if (
                    onActivate != null &&
                    (it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.DirectionCenter)
                ) {
                    onActivate()
                    true
                } else {
                    false
                }
            }
            .then(
                if (onActivate != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onActivate
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(12.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(Color(0x990A0F1A))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = Color(0xFFE6ECF7),
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    label: String,
    focusRequester: FocusRequester?,
    isLeftEdge: Boolean,
    onActivate: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveUp: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isFocused) Color(0xFFB6D9FF) else Color(0xFF2A3348)
    val backgroundColor = if (isFocused) Color(0xFF222E44) else Color(0xFF161E2E)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    false
                } else if (isLeftEdge && it.key == Key.DirectionLeft) {
                    onMoveLeft()
                    true
                } else if (onMoveUp != null && it.key == Key.DirectionUp) {
                    onMoveUp()
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
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(12.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = label,
            color = Color(0xFFE6ECF7),
            fontSize = 16.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun CategoryTypeTab(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onActivate: () -> Unit,
    onMoveLeft: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val borderColor = when {
        isFocused -> Color(0xFFB6D9FF)
        selected -> Color(0xFF42539A)
        else -> Color(0xFF1E2533)
    }
    val backgroundColor = when {
        isFocused -> Color(0xFF4F8CFF)
        selected -> Color(0xFF273479)
        else -> Color(0xFF1A2233)
    }
    val textColor = if (isFocused) Color(0xFF0C1730) else Color(0xFFE6ECF7)
    Box(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    false
                } else if (onMoveLeft != null && it.key == Key.DirectionLeft) {
                    onMoveLeft()
                    true
                } else if (onMoveRight != null && it.key == Key.DirectionRight) {
                    onMoveRight()
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
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onMoveLeft: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isFocused) Color(0xFFB6D9FF) else Color(0xFF2A3348)
    val backgroundColor = if (isFocused) Color(0xFF222E44) else Color(0xFF161E2E)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Color(0xFFE6ECF7),
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        ),
        cursorBrush = SolidColor(Color(0xFFB6D9FF)),
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) {
                    false
                } else if (onMoveLeft != null && it.key == Key.DirectionLeft) {
                    onMoveLeft()
                    true
                } else if (onMoveRight != null && it.key == Key.DirectionRight) {
                    onMoveRight()
                    true
                } else if (onMoveDown != null && it.key == Key.DirectionDown) {
                    onMoveDown()
                    true
                } else {
                    false
                }
            }
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (query.isBlank()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 0.3.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun SectionScreen(
    title: String,
    section: Section,
    contentRepository: ContentRepository,
    authConfig: AuthConfig,
    contentItemFocusRequester: FocusRequester,
    resumeFocusId: String?,
    resumeFocusRequester: FocusRequester,
    onItemFocused: (ContentItem) -> Unit,
    onPlay: (ContentItem) -> Unit,
    onMoveLeft: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val columns = 3
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(section) {
        searchQuery = ""
        debouncedQuery = ""
    }
    LaunchedEffect(searchQuery) {
        delay(250)
        debouncedQuery = SearchNormalizer.normalizeQuery(searchQuery)
    }
    val activeQuery = debouncedQuery
    val pagerFlow = remember(section, authConfig, activeQuery) {
        if (activeQuery.isBlank()) {
            contentRepository.pager(section, authConfig).flow
        } else {
            contentRepository.searchPager(section, activeQuery, authConfig).flow
        }
    }
    val lazyItems = pagerFlow.collectAsLazyPagingItems()

    LaunchedEffect(lazyItems.itemCount, section, activeQuery) {
        if (lazyItems.itemCount > 0) {
            contentItemFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 12.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1626),
                            Color(0xFF141C2E)
                        )
                    )
                )
                .border(1.dp, Color(0xFF1E2738), shape)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                SearchInput(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search...",
                    focusRequester = searchFocusRequester,
                    modifier = Modifier.width(240.dp),
                    onMoveLeft = onMoveLeft,
                    onMoveDown = { contentItemFocusRequester.requestFocus() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (lazyItems.loadState.refresh is LoadState.Loading && lazyItems.itemCount == 0) {
                Text(
                    text = if (activeQuery.isBlank()) "Loading content..." else "Searching...",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.6.sp
                )
            } else if (lazyItems.loadState.refresh is LoadState.Error) {
                Text(
                    text = if (activeQuery.isBlank()) "Content failed to load" else "Search failed to load",
                    color = Color(0xFFE4A9A9),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.6.sp
                )
            } else if (lazyItems.itemCount == 0) {
                Text(
                    text = if (activeQuery.isBlank()) "No content yet" else "No results found",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.6.sp
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        count = lazyItems.itemCount,
                        key = { index ->
                            lazyItems[index]?.id ?: "item-$index"
                        }
                    ) { index ->
                        val item = lazyItems[index]
                        val requester = when {
                            item?.id != null && item.id == resumeFocusId -> resumeFocusRequester
                            index == 0 -> contentItemFocusRequester
                            else -> null
                        }
                        val isLeftEdge = index % columns == 0
                        val isTopRow = index < columns
                        ContentCard(
                            item = item,
                            focusRequester = requester,
                            isLeftEdge = isLeftEdge,
                            onActivate = if (item != null) {
                                { onPlay(item) }
                            } else {
                                null
                            },
                            onFocused = onItemFocused,
                            onMoveLeft = onMoveLeft,
                            onMoveUp = if (isTopRow) {
                                { searchFocusRequester.requestFocus() }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySectionScreen(
    title: String,
    contentRepository: ContentRepository,
    authConfig: AuthConfig,
    contentItemFocusRequester: FocusRequester,
    resumeFocusId: String?,
    resumeFocusRequester: FocusRequester,
    onItemFocused: (ContentItem) -> Unit,
    onPlay: (ContentItem) -> Unit,
    onMoveLeft: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    var activeType by remember { mutableStateOf(ContentType.LIVE) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var selectedSeries by remember { mutableStateOf<ContentItem?>(null) }
    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    val columns = 3
    val tabFocusRequesters = remember {
        ContentType.values().map { FocusRequester() }
    }
    val backTabFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(activeType) {
        searchQuery = ""
        debouncedQuery = ""
    }
    LaunchedEffect(searchQuery) {
        delay(250)
        debouncedQuery = SearchNormalizer.normalizeQuery(searchQuery)
    }
    val activeQuery = debouncedQuery

    BackHandler(enabled = selectedSeries != null || selectedCategory != null) {
        if (selectedSeries != null) {
            onItemFocused(selectedSeries!!)
            selectedSeries = null
        } else {
            selectedCategory = null
        }
    }

    LaunchedEffect(activeType, authConfig) {
        isLoading = true
        errorMessage = null
        selectedCategory = null
        selectedSeries = null
        runCatching { contentRepository.loadCategories(activeType, authConfig) }
            .onSuccess { categories = it }
            .onFailure { errorMessage = it.message ?: "Failed to load categories" }
        isLoading = false
    }

    LaunchedEffect(selectedCategory) {
        contentItemFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 12.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1626),
                            Color(0xFF141C2E)
                        )
                    )
                )
                .border(1.dp, Color(0xFF1E2738), shape)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedCategory != null) {
                            CategoryTypeTab(
                                label = "Back",
                                selected = false,
                                focusRequester = backTabFocusRequester,
                                onActivate = { selectedCategory = null },
                                onMoveLeft = { searchFocusRequester.requestFocus() },
                                onMoveRight = { tabFocusRequesters.first().requestFocus() }
                            )
                        }
                        ContentType.values().forEachIndexed { index, type ->
                            val requester = tabFocusRequesters[index]
                            CategoryTypeTab(
                                label = type.label,
                                selected = activeType == type,
                                focusRequester = requester,
                                onActivate = { activeType = type },
                                onMoveLeft = if (index > 0) {
                                    { tabFocusRequesters[index - 1].requestFocus() }
                                } else if (selectedCategory != null) {
                                    { backTabFocusRequester.requestFocus() }
                                } else {
                                    { searchFocusRequester.requestFocus() }
                                },
                                onMoveRight = if (index < tabFocusRequesters.lastIndex) {
                                    { tabFocusRequesters[index + 1].requestFocus() }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SearchInput(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search...",
                        focusRequester = searchFocusRequester,
                        modifier = Modifier.width(240.dp),
                        onMoveLeft = onMoveLeft,
                        onMoveRight = {
                            if (selectedCategory != null) {
                                backTabFocusRequester.requestFocus()
                            } else {
                                tabFocusRequesters.firstOrNull()?.requestFocus()
                            }
                        },
                        onMoveDown = { contentItemFocusRequester.requestFocus() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (selectedCategory != null) {
                val category = selectedCategory!!
                if (selectedSeries != null) {
                    SeriesEpisodesScreen(
                        seriesItem = selectedSeries!!,
                        contentRepository = contentRepository,
                        authConfig = authConfig,
                        contentItemFocusRequester = contentItemFocusRequester,
                        resumeFocusId = resumeFocusId,
                        resumeFocusRequester = resumeFocusRequester,
                        onItemFocused = onItemFocused,
                        onPlay = onPlay,
                        onMoveLeft = onMoveLeft,
                        onBack = {
                            onItemFocused(selectedSeries!!)
                            selectedSeries = null
                        }
                    )
                } else {
                    val pagerFlow = remember(category.id, activeType, authConfig, activeQuery) {
                        if (activeQuery.isBlank()) {
                            contentRepository.categoryPager(activeType, category.id, authConfig).flow
                        } else {
                            contentRepository.categorySearchPager(
                                activeType,
                                category.id,
                                activeQuery,
                                authConfig
                            ).flow
                        }
                    }
                    val lazyItems = pagerFlow.collectAsLazyPagingItems()
                    LaunchedEffect(lazyItems.itemCount, category.id, activeType, activeQuery) {
                        if (lazyItems.itemCount > 0) {
                            contentItemFocusRequester.requestFocus()
                        }
                    }
                    Text(
                        text = if (activeQuery.isBlank()) category.name else "Search results",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (lazyItems.loadState.refresh is LoadState.Loading && lazyItems.itemCount == 0) {
                        Text(
                            text = if (activeQuery.isBlank()) "Loading content..." else "Searching...",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 0.6.sp
                        )
                    } else if (lazyItems.loadState.refresh is LoadState.Error) {
                        Text(
                            text = if (activeQuery.isBlank()) "Content failed to load" else "Search failed to load",
                            color = Color(0xFFE4A9A9),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 0.6.sp
                        )
                    } else if (lazyItems.itemCount == 0) {
                        Text(
                            text = if (activeQuery.isBlank()) "No content yet" else "No results found",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 0.6.sp
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(
                                count = lazyItems.itemCount,
                                key = { index ->
                                    lazyItems[index]?.id ?: "cat-item-$index"
                                }
                            ) { index ->
                                val item = lazyItems[index]
                                val isLeftEdge = index % columns == 0
                                val isTopRow = index < columns
                                val requester = when {
                                    item?.id != null && item.id == resumeFocusId -> resumeFocusRequester
                                    index == 0 -> contentItemFocusRequester
                                    else -> null
                                }
                                ContentCard(
                                    item = item,
                                    focusRequester = requester,
                                    isLeftEdge = isLeftEdge,
                                    onActivate = if (item != null) {
                                        {
                                            if (activeType == ContentType.SERIES &&
                                                item.containerExtension.isNullOrBlank()
                                            ) {
                                                selectedSeries = item
                                            } else {
                                                onPlay(item)
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                    onFocused = onItemFocused,
                                    onMoveLeft = onMoveLeft,
                                    onMoveUp = if (isTopRow) {
                                        { searchFocusRequester.requestFocus() }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (isLoading) {
                Text(
                    text = "Loading categories...",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.6.sp
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Failed to load categories",
                    color = Color(0xFFE4A9A9),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.6.sp
                )
            } else {
                val filteredCategories = if (activeQuery.isBlank()) {
                    categories
                } else {
                    categories.filter {
                        SearchNormalizer.normalizeTitle(it.name)
                            .contains(activeQuery, ignoreCase = true)
                    }
                }
                if (activeQuery.isNotBlank() && filteredCategories.isEmpty()) {
                    Text(
                        text = "No categories found",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 0.6.sp
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredCategories.size) { index ->
                            val category = filteredCategories[index]
                            val requester = if (index == 0) contentItemFocusRequester else null
                            val isLeftEdge = index % columns == 0
                            val isTopRow = index < columns
                            CategoryCard(
                                label = category.name,
                                focusRequester = requester,
                                isLeftEdge = isLeftEdge,
                                onActivate = { selectedCategory = category },
                                onMoveLeft = onMoveLeft,
                                onMoveUp = if (isTopRow) {
                                    { searchFocusRequester.requestFocus() }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesEpisodesScreen(
    seriesItem: ContentItem,
    contentRepository: ContentRepository,
    authConfig: AuthConfig,
    contentItemFocusRequester: FocusRequester,
    resumeFocusId: String?,
    resumeFocusRequester: FocusRequester,
    onItemFocused: (ContentItem) -> Unit,
    onPlay: (ContentItem) -> Unit,
    onMoveLeft: () -> Unit,
    onBack: () -> Unit
) {
    val pagerFlow = remember(seriesItem.streamId, authConfig) {
        contentRepository.seriesPager(seriesItem.streamId, authConfig).flow
    }
    val lazyItems = pagerFlow.collectAsLazyPagingItems()
    val columns = 3
    val backFocusRequester = remember { FocusRequester() }

    LaunchedEffect(lazyItems.itemCount, lazyItems.loadState.refresh, seriesItem.id) {
        if (lazyItems.itemCount > 0) {
            contentItemFocusRequester.requestFocus()
        } else if (lazyItems.loadState.refresh !is LoadState.Loading) {
            backFocusRequester.requestFocus()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "< BACK",
                color = Color(0xFFE6ECF7),
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .focusRequester(backFocusRequester)
                    .focusable()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() }
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
                            onBack()
                            true
                        } else {
                            false
                        }
                    }
                    .padding(end = 12.dp)
            )
            Text(
                text = seriesItem.title,
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (lazyItems.loadState.refresh is LoadState.Loading && lazyItems.itemCount == 0) {
            Text(
                text = "Loading episodes...",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                letterSpacing = 0.6.sp
            )
        } else if (lazyItems.loadState.refresh is LoadState.Error) {
            Text(
                text = "Episodes failed to load",
                color = Color(0xFFE4A9A9),
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                letterSpacing = 0.6.sp
            )
        } else if (lazyItems.itemCount == 0) {
            Text(
                text = "No episodes yet",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                letterSpacing = 0.6.sp
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    count = lazyItems.itemCount,
                    key = { index ->
                        lazyItems[index]?.id ?: "episode-$index"
                    }
                ) { index ->
                    val item = lazyItems[index]
                    val requester = when {
                        item?.id != null && item.id == resumeFocusId -> resumeFocusRequester
                        index == 0 -> contentItemFocusRequester
                        else -> null
                    }
                    val isLeftEdge = index % columns == 0
                    val isTopRow = index < columns
                    ContentCard(
                        item = item,
                        focusRequester = requester,
                        isLeftEdge = isLeftEdge,
                        onActivate = if (item != null) {
                            { onPlay(item) }
                        } else {
                            null
                        },
                        onFocused = onItemFocused,
                        onMoveLeft = onMoveLeft,
                        onMoveUp = if (isTopRow) {
                            { backFocusRequester.requestFocus() }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

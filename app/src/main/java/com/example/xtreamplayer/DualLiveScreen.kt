package com.example.xtreamplayer

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.ui.theme.AppTheme
import kotlinx.coroutines.delay

private const val LONG_PRESS_THRESHOLD_MS = 600L

@OptIn(UnstableApi::class)
@Composable
internal fun DualLiveScreen(
    primaryPlayer: Player,
    secondaryPlayer: Player?,
    primaryChannel: ContentItem?,
    rightChannel: ContentItem?,
    focusedTile: DualTile,
    leftTileState: DualTilePlayerState = DualTilePlayerState.Ready,
    rightTileState: DualTilePlayerState,
    // Action menu state owned by parent so the parent BackHandler can dismiss it first.
    actionMenuTile: DualTile?,
    onOpenActionMenu: (DualTile) -> Unit,
    onCloseActionMenu: () -> Unit,
    onFocusChange: (DualTile) -> Unit,
    onEnterFullscreen: (DualTile) -> Unit,
    onOpenChannelPicker: (DualTile) -> Unit,
    onCloseTile: (DualTile) -> Unit,
    onRestartStream: (DualTile) -> Unit,
    onRetryError: (DualTile) -> Unit,
    onExitDualScreen: () -> Unit,
    isGuideOpen: Boolean = false,
    onGuideMove: (Int) -> Unit = {},
    onGuideSelect: () -> Unit = {},
    onGuideBack: () -> Unit = {},
    onGuideDismiss: () -> Unit = {},
    onGuideSearchKey: (KeyEvent) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    val focusTrapRequester = remember { FocusRequester() }

    // Claim focus on mount (isGuideOpen starts false) and whenever the guide opens.
    // Merged from the original separate LaunchedEffect(Unit) + LaunchedEffect(isGuideOpen).
    LaunchedEffect(isGuideOpen) {
        runCatching { focusTrapRequester.requestFocus() }
    }
    // After guide/menu close or when a new tile's AndroidView is composed, reclaim focus with
    // an 80 ms delay so the freshly-added PlayerView can't steal focus during layout.
    LaunchedEffect(isGuideOpen, actionMenuTile, primaryChannel?.id, rightChannel?.id) {
        if (!isGuideOpen && actionMenuTile == null) {
            delay(80)
            runCatching { focusTrapRequester.requestFocus() }
        }
    }

    val keyHandler = remember { Handler(Looper.getMainLooper()) }
    // Plain object refs — never drive recomposition, only read/written inside key handlers.
    val longPress = remember { object { var runnable: Runnable? = null; var handled = false } }
    val swallowOkUp = remember { object { var value = false } }
    // Tracks whether the action menu was opened by long-OK (needs suppress) or Menu key (no suppress).
    val menuOpenedViaLongPress = remember { object { var value = false } }

    fun cancelLongPress() {
        longPress.runnable?.let { keyHandler.removeCallbacks(it) }
        longPress.runnable = null
    }

    fun scheduleLongPress(tile: DualTile) {
        cancelLongPress()
        longPress.handled = false
        val runnable = Runnable {
            longPress.runnable = null
            longPress.handled = true
            menuOpenedViaLongPress.value = true
            onOpenActionMenu(tile)
        }
        longPress.runnable = runnable
        keyHandler.postDelayed(runnable, LONG_PRESS_THRESHOLD_MS)
    }

    DisposableEffect(Unit) {
        onDispose { cancelLongPress() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusTrapRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (actionMenuTile != null) return@onPreviewKeyEvent false
                if (isGuideOpen) {
                    if (keyEvent.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER,
                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE -> true
                            else -> false
                        }
                    }
                    return@onPreviewKeyEvent when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> { onGuideMove(-1); true }
                        KeyEvent.KEYCODE_DPAD_DOWN -> { onGuideMove(1); true }
                        KeyEvent.KEYCODE_DPAD_LEFT -> { onGuideBack(); true }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> true
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            swallowOkUp.value = true
                            onGuideSelect()
                            true
                        }
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE -> { onGuideDismiss(); true }
                        else -> onGuideSearchKey(keyEvent.nativeKeyEvent)
                    }
                }
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (focusedTile == DualTile.RIGHT) onFocusChange(DualTile.LEFT)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (focusedTile == DualTile.LEFT) onFocusChange(DualTile.RIGHT)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (keyEvent.nativeKeyEvent.repeatCount == 0) scheduleLongPress(focusedTile)
                            true
                        }
                        KeyEvent.KEYCODE_MENU -> { menuOpenedViaLongPress.value = false; onOpenActionMenu(focusedTile); true }
                        else -> false
                    }
                    KeyEventType.KeyUp -> when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (swallowOkUp.value) {
                                swallowOkUp.value = false
                                cancelLongPress()
                                longPress.handled = false
                                return@onPreviewKeyEvent true
                            }
                            val consumedByLongPress = longPress.handled
                            cancelLongPress()
                            if (!consumedByLongPress) {
                                val tile = focusedTile
                                val tileError = when (tile) {
                                    DualTile.LEFT -> leftTileState is DualTilePlayerState.Error
                                    DualTile.RIGHT -> rightTileState is DualTilePlayerState.Error
                                }
                                val hasChannel = if (tile == DualTile.LEFT) primaryChannel != null else rightChannel != null
                                when {
                                    tileError -> onRetryError(tile)
                                    hasChannel -> onEnterFullscreen(tile)
                                    else -> onOpenChannelPicker(tile)
                                }
                            }
                            longPress.handled = false
                            true
                        }
                        else -> false
                    }
                    else -> false
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            DualLiveTile(
                tile = DualTile.LEFT,
                player = primaryPlayer,
                channel = primaryChannel,
                playerState = leftTileState,
                isFocused = focusedTile == DualTile.LEFT,
                onRetryError = { onRetryError(DualTile.LEFT) },
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.width(2.dp).fillMaxSize().background(Color.Black))
            DualLiveTile(
                tile = DualTile.RIGHT,
                player = secondaryPlayer,
                channel = rightChannel,
                playerState = rightTileState,
                isFocused = focusedTile == DualTile.RIGHT,
                onAddChannel = { onOpenChannelPicker(DualTile.RIGHT) },
                onRetryError = { onRetryError(DualTile.RIGHT) },
                modifier = Modifier.weight(1f)
            )
        }

        val menuTile = actionMenuTile
        if (menuTile != null) {
            DualLiveActionMenu(
                tile = menuTile,
                openedViaLongPress = menuOpenedViaLongPress.value,
                hasChannel = if (menuTile == DualTile.LEFT) primaryChannel != null else rightChannel != null,
                onChangeChannel = { onCloseActionMenu(); onOpenChannelPicker(menuTile) },
                onCloseThisScreen = { onCloseActionMenu(); onCloseTile(menuTile) },
                onRestartStream = { onCloseActionMenu(); onRestartStream(menuTile) },
                onAddChannel = { onCloseActionMenu(); onOpenChannelPicker(menuTile) },
                onExitDualScreen = { onCloseActionMenu(); onExitDualScreen() },
                onDismiss = onCloseActionMenu
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun DualLiveTile(
    tile: DualTile,
    player: Player?,
    channel: ContentItem?,
    playerState: DualTilePlayerState,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    onAddChannel: (() -> Unit)? = null,
    onRetryError: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val accentColor = colors.accent

    var videoAspectRatio by remember { mutableStateOf(16f / 9f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (player != null && channel != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        isFocusable = false
                        isFocusableInTouchMode = false
                        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    }
                },
                update = { view ->
                    if (view.player != player) view.player = player
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { view -> view.player = null }
            )

            if (playerState is DualTilePlayerState.Loading || playerState is DualTilePlayerState.Reconnecting) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                }
            }

            if (playerState is DualTilePlayerState.Error) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .then(if (onRetryError != null) Modifier.clickable { onRetryError() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Stream failed",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        if (!playerState.message.isNullOrBlank()) {
                            Text(
                                text = playerState.message,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                fontFamily = AppTheme.fontFamily,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "Press OK to retry • Long-press for options",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontFamily = AppTheme.fontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (isFocused && playerState !is DualTilePlayerState.Error && playerState !is DualTilePlayerState.Reconnecting) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = channel.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (onAddChannel != null) Modifier.clickable { onAddChannel() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = if (isFocused) 0.18f else 0.10f))
                            .border(
                                1.dp,
                                if (isFocused) accentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = if (isFocused) accentColor else Color.White.copy(alpha = 0.6f),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                    Text(
                        text = "Add channel",
                        color = Color.White.copy(alpha = if (isFocused) 0.9f else 0.5f),
                        fontSize = 12.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                }
            }
        }

        // Focused border — sized to the video frame (aspect-ratio-matched), not the full tile.
        // The channel name label sits in the letterbox bar below, outside the border.
        if (isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .aspectRatio(videoAspectRatio)
                    .border(3.dp, accentColor)
            )
        }
    }
}

@Composable
private fun DualLiveMenuRow(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) accentColor else textColor,
            fontSize = 14.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DualLiveActionMenu(
    tile: DualTile,
    openedViaLongPress: Boolean,
    hasChannel: Boolean,
    onChangeChannel: () -> Unit,
    onCloseThisScreen: () -> Unit,
    onRestartStream: () -> Unit,
    onAddChannel: () -> Unit,
    onExitDualScreen: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val menuItems: List<Pair<String, () -> Unit>> = if (hasChannel) {
        listOf(
            "Change channel" to onChangeChannel,
            "Close this screen" to onCloseThisScreen,
            "Restart this stream" to onRestartStream,
            "Exit dual screen" to onExitDualScreen
        )
    } else {
        listOf(
            "Add channel" to onAddChannel,
            "Exit dual screen" to onExitDualScreen
        )
    }

    var selectedIndex by remember { mutableStateOf(0) }
    // Suppress first OK selection only when opened via long-OK (the OK key-up must be swallowed).
    // Menu/Gear-opened menus have no pending OK key-up so suppress is not needed.
    val suppressSelect = remember { object { var value = openedViaLongPress } }
    val menuFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { menuFocusRequester.requestFocus() }
    }
    BackHandler(enabled = true) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.background)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp)
                .focusRequester(menuFocusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    when (keyEvent.type) {
                        KeyEventType.KeyUp -> when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                suppressSelect.value = false
                                true
                            }
                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE -> true
                            else -> false
                        }
                        KeyEventType.KeyDown -> when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                selectedIndex = (selectedIndex + 1).coerceAtMost(menuItems.lastIndex)
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                if (suppressSelect.value || keyEvent.nativeKeyEvent.repeatCount > 0) {
                                    return@onPreviewKeyEvent true
                                }
                                menuItems[selectedIndex].second.invoke()
                                true
                            }
                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE -> { onDismiss(); true }
                            else -> false
                        }
                        else -> false
                    }
                }
        ) {
            Text(
                text = if (tile == DualTile.LEFT) "Left tile" else "Right tile",
                color = colors.textTertiary,
                fontSize = 11.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
            menuItems.forEachIndexed { index, (label, _) ->
                DualLiveMenuRow(
                    label = label,
                    isSelected = index == selectedIndex,
                    accentColor = colors.accent,
                    textColor = colors.textPrimary
                )
            }
        }
    }
}

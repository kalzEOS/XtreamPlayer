package com.example.xtreamplayer.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.xtreamplayer.api.SubtitleSearchResult
import com.example.xtreamplayer.player.SubtitleTrackInfo
import com.example.xtreamplayer.player.VideoTrackInfo
import com.example.xtreamplayer.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

private val DialogBackground: Color
    @Composable get() = AppTheme.colors.background
private val SecondaryBorderColor: Color
    @Composable get() = AppTheme.colors.borderStrong
private val PrimaryButtonColor: Color
    @Composable get() = AppTheme.colors.accent
private val MutedTextColor: Color
    @Composable get() = AppTheme.colors.textSecondary
private val FocusBorderColor: Color
    @Composable get() = AppTheme.colors.focus
private val EnabledIndicatorColor: Color
    @Composable get() = AppTheme.colors.success
private val DisabledIndicatorColor: Color
    @Composable get() = AppTheme.colors.textTertiary
private val InputBackground: Color
    @Composable get() = AppTheme.colors.surfaceAlt
private val InputTextColor: Color
    @Composable get() = AppTheme.colors.textPrimary

private val ContentAreaBackground: Color
    @Composable get() = AppTheme.colors.surface

sealed class SubtitleDialogState {
    object Idle : SubtitleDialogState()
    object Searching : SubtitleDialogState()
    data class Results(val subtitles: List<SubtitleSearchResult>) : SubtitleDialogState()
    data class Downloading(val subtitle: SubtitleSearchResult) : SubtitleDialogState()
    data class Error(val message: String) : SubtitleDialogState()
}

@Composable
fun SubtitleSearchDialog(
    initialQuery: String,
    state: SubtitleDialogState,
    subtitlesEnabled: Boolean,
    embeddedSubtitlesAvailable: Boolean,
    onSearch: (String) -> Unit,
    onSelect: (SubtitleSearchResult) -> Unit,
    onToggleSubtitles: () -> Unit,
    onSelectSubtitleTrack: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    val searchFocusRequester = remember { FocusRequester() }
    val searchButtonFocusRequester = remember { FocusRequester() }
    val subtitleTrackFocusRequester = remember { FocusRequester() }
    val toggleFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(DialogBackground)
                .border(1.dp, SecondaryBorderColor, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
        ) {
            Column {
                Text(
                    text = "DOWNLOAD SUBTITLES",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (embeddedSubtitlesAvailable) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EnabledIndicatorColor)
                        )
                        Text(
                            text = "Embedded subtitles detected for this video",
                            color = MutedTextColor,
                            fontSize = 12.sp,
                            fontFamily = AppTheme.fontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        focusRequester = searchFocusRequester,
                        onSearch = { onSearch(query) },
                        onMoveRight = { searchButtonFocusRequester.requestFocus() },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    FocusableButton(
                        onClick = { onSearch(query) },
                        enabled = query.isNotBlank() && state !is SubtitleDialogState.Searching,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryButtonColor,
                            contentColor = AppTheme.colors.textOnAccent,
                            disabledContainerColor = SecondaryBorderColor,
                            disabledContentColor = DisabledIndicatorColor
                        ),
                        modifier = Modifier
                            .focusRequester(searchButtonFocusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            searchFocusRequester.requestFocus()
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            subtitleTrackFocusRequester.requestFocus()
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            closeFocusRequester.requestFocus()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                    ) {
                        Text(
                            text = "Search",
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    FocusableButton(
                        onClick = onSelectSubtitleTrack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryBorderColor,
                            contentColor = AppTheme.colors.textPrimary
                        ),
                        modifier = Modifier
                            .focusRequester(subtitleTrackFocusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            searchButtonFocusRequester.requestFocus()
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            toggleFocusRequester.requestFocus()
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            closeFocusRequester.requestFocus()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                    ) {
                        Text(
                            text = "Subtitle track",
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val toggleIndicatorColor =
                        if (subtitlesEnabled) EnabledIndicatorColor else DisabledIndicatorColor
                    val toggleButtonColor =
                        if (subtitlesEnabled) AppTheme.colors.success.copy(alpha = 0.35f)
                        else SecondaryBorderColor

                    FocusableButton(
                        onClick = onToggleSubtitles,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = toggleButtonColor,
                            contentColor = AppTheme.colors.textPrimary
                        ),
                        modifier = Modifier
                            .focusRequester(toggleFocusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            subtitleTrackFocusRequester.requestFocus()
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            closeFocusRequester.requestFocus()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(toggleIndicatorColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (subtitlesEnabled) "Subtitles On" else "Subtitles Off",
                                fontFamily = AppTheme.fontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ContentAreaBackground)
                        .padding(8.dp)
                ) {
                    when (state) {
                        is SubtitleDialogState.Idle -> {
                            Text(
                                text = "Enter a title to search for subtitles",
                                color = MutedTextColor,
                                fontSize = 14.sp,
                                fontFamily = AppTheme.fontFamily,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is SubtitleDialogState.Searching -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = PrimaryButtonColor,
                                    modifier = Modifier.padding(8.dp)
                                )
                                Text(
                                    text = "Searching...",
                                    color = MutedTextColor,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily
                                )
                            }
                        }
                        is SubtitleDialogState.Results -> {
                            if (state.subtitles.isEmpty()) {
                                Text(
                                    text = "No subtitles found",
                                    color = MutedTextColor,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn {
                                    items(state.subtitles) { subtitle ->
                                        SubtitleItem(
                                            subtitle = subtitle,
                                            onClick = { onSelect(subtitle) }
                                        )
                                    }
                                }
                            }
                        }
                        is SubtitleDialogState.Downloading -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = PrimaryButtonColor,
                                    modifier = Modifier.padding(8.dp)
                                )
                                Text(
                                    text = "Downloading subtitle...",
                                    color = MutedTextColor,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily
                                )
                            }
                        }
                        is SubtitleDialogState.Error -> {
                            Text(
                                text = state.message,
                                color = AppTheme.colors.error,
                                fontSize = 14.sp,
                                fontFamily = AppTheme.fontFamily,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FocusableButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryBorderColor,
                            contentColor = AppTheme.colors.textPrimary
                        ),
                        modifier = Modifier
                            .focusRequester(closeFocusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionUp -> {
                                            searchButtonFocusRequester.requestFocus()
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                    ) {
                        Text(
                            text = "Close",
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSearch: () -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wrapperInteractionSource = remember { MutableInteractionSource() }
    val isWrapperFocused by wrapperInteractionSource.collectIsFocusedAsState()
    val textFieldFocusRequester = remember { FocusRequester() }
    var isTextFieldFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val borderColor =
        if (isWrapperFocused || isTextFieldFocused) FocusBorderColor else SecondaryBorderColor
    val context = LocalContext.current
    val inputMethodManager = remember(context) {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    val showKeyboard = {
        @Suppress("DEPRECATION")
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
    val activateTextField = {
        textFieldFocusRequester.requestFocus()
        showKeyboard()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(InputBackground)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .focusRequester(focusRequester)
            .focusable(interactionSource = wrapperInteractionSource)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else when (event.key) {
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        activateTextField()
                        true
                    }
                    Key.DirectionRight -> {
                        onMoveRight()
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = InputTextColor,
                fontSize = 14.sp,
                fontFamily = AppTheme.fontFamily
            ),
            cursorBrush = SolidColor(FocusBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(textFieldFocusRequester)
                .onFocusChanged { state ->
                    isTextFieldFocused = state.isFocused
                },
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search for subtitles...",
                            color = MutedTextColor,
                            fontSize = 14.sp,
                            fontFamily = AppTheme.fontFamily
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun SubtitleOptionsDialog(
    subtitlesEnabled: Boolean,
    showSubtitleTrackOption: Boolean,
    onToggleSubtitles: () -> Unit,
    onSelectSubtitleTrack: () -> Unit,
    onDownloadSubtitles: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryFocusRequester = remember { FocusRequester() }
    val secondaryFocusRequester = remember { FocusRequester() }
    val tertiaryFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        primaryFocusRequester.requestFocus()
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(16.dp))
                .background(DialogBackground)
                .border(1.dp, SecondaryBorderColor, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "SUBTITLES",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                val statusColor = if (subtitlesEnabled) EnabledIndicatorColor else DisabledIndicatorColor
                val toggleColor = if (subtitlesEnabled) SecondaryBorderColor else PrimaryButtonColor
                val toggleTextColor =
                    if (subtitlesEnabled) AppTheme.colors.textPrimary
                    else AppTheme.colors.textOnAccent

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = if (subtitlesEnabled) "Status: On" else "Status: Off",
                        color = MutedTextColor,
                        fontSize = 12.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                }

                FocusableButton(
                    onClick = onToggleSubtitles,
                    modifier = Modifier.focusRequester(primaryFocusRequester),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = toggleColor,
                        contentColor = toggleTextColor
                    )
                ) {
                    Text(
                        text = if (subtitlesEnabled) "Turn subtitles off" else "Turn subtitles on",
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showSubtitleTrackOption) {
                    FocusableButton(
                        onClick = onSelectSubtitleTrack,
                        modifier = Modifier.focusRequester(secondaryFocusRequester),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryBorderColor,
                            contentColor = AppTheme.colors.textPrimary
                        )
                    ) {
                        Text(
                            text = "Subtitle track",
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                FocusableButton(
                    onClick = onDownloadSubtitles,
                    modifier = Modifier.focusRequester(tertiaryFocusRequester),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryBorderColor,
                        contentColor = AppTheme.colors.textPrimary
                    )
                ) {
                    Text(
                        text = "Download subtitles",
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FocusableButton(
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(closeFocusRequester),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.surfaceAlt,
                        contentColor = AppTheme.colors.textPrimary
                    )
                ) {
                    Text(
                        text = "Close",
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ApiKeyInputDialog(
    currentKey: String,
    currentUserAgent: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(currentKey) }
    var userAgent by remember { mutableStateOf(currentUserAgent) }
    val context = LocalContext.current
    val inputFocusRequester = remember { FocusRequester() }
    val userAgentFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(16.dp))
                .background(DialogBackground)
                .border(1.dp, SecondaryBorderColor, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "OPENSUBTITLES API KEY",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Get your free API key at opensubtitles.com",
                    color = MutedTextColor,
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "API key",
                    color = MutedTextColor,
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily
                )

                Spacer(modifier = Modifier.height(6.dp))

                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                val shape = RoundedCornerShape(8.dp)
                val borderColor = if (isFocused) FocusBorderColor else SecondaryBorderColor

                BasicTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = InputTextColor,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily
                    ),
                    cursorBrush = SolidColor(FocusBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(InputBackground)
                        .border(1.dp, borderColor, shape)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .focusRequester(inputFocusRequester)
                        .focusable(interactionSource = interactionSource)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionDown -> {
                                        userAgentFocusRequester.requestFocus()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    decorationBox = { innerTextField ->
                        Box {
                            if (apiKey.isEmpty()) {
                                Text(
                                    text = "Enter your API key...",
                                    color = MutedTextColor,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "User agent (app name)",
                    color = MutedTextColor,
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily
                )

                Spacer(modifier = Modifier.height(6.dp))

                val userAgentInteractionSource = remember { MutableInteractionSource() }
                val userAgentFocused by userAgentInteractionSource.collectIsFocusedAsState()
                val userAgentBorderColor =
                    if (userAgentFocused) FocusBorderColor else SecondaryBorderColor

                BasicTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = InputTextColor,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily
                    ),
                    cursorBrush = SolidColor(FocusBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(InputBackground)
                        .border(1.dp, userAgentBorderColor, shape)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .focusRequester(userAgentFocusRequester)
                        .focusable(interactionSource = userAgentInteractionSource)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionDown -> {
                                        saveFocusRequester.requestFocus()
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        inputFocusRequester.requestFocus()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    decorationBox = { innerTextField ->
                        Box {
                            if (userAgent.isEmpty()) {
                                Text(
                                    text = "Example: XtreamPlayer",
                                    color = MutedTextColor,
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FocusableButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryBorderColor,
                            contentColor = AppTheme.colors.textPrimary
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    FocusableButton(
                        onClick = {
                            if (apiKey.isBlank() || userAgent.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Enter API key and user agent",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@FocusableButton
                            }
                            onSave(apiKey, userAgent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryButtonColor,
                            contentColor = AppTheme.colors.textOnAccent
                        ),
                        modifier = Modifier.focusRequester(saveFocusRequester)
                    ) {
                        Text(
                            text = "Save",
                            fontFamily = AppTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleItem(
    subtitle: SubtitleSearchResult,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val backgroundColor =
        if (isFocused) AppTheme.colors.surfaceAlt else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subtitle.release,
                color = AppTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Row {
                Text(
                    text = subtitle.languageName.uppercase(),
                    color = PrimaryButtonColor,
                    fontSize = 11.sp,
                    fontFamily = AppTheme.fontFamily
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${subtitle.downloadCount} downloads",
                    color = MutedTextColor,
                    fontSize = 11.sp,
                    fontFamily = AppTheme.fontFamily
                )
                if (subtitle.hearingImpaired) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HI",
                        color = AppTheme.colors.warning,
                        fontSize = 11.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SubtitleTrackDialog(
    availableTracks: List<SubtitleTrackInfo>,
    onTrackSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val closeFocusRequester = remember { FocusRequester() }
    val itemFocusRequesters =
        remember(availableTracks.size) { List(availableTracks.size) { FocusRequester() } }
    val listState = rememberLazyListState()
    val supportedTrackCount = availableTracks.count { it.isSupported }
    val hasSingleTrack = supportedTrackCount <= 1
    val selectedTrackIndex =
        availableTracks.indexOfFirst { it.isSelected && it.isSupported && !hasSingleTrack }
            .takeIf { it >= 0 }
            ?: availableTracks.indexOfFirst { it.isSupported && !hasSingleTrack }.takeIf { it >= 0 }

    LaunchedEffect(availableTracks.size, selectedTrackIndex, listState) {
        if (selectedTrackIndex != null) {
            listState.scrollToItem(selectedTrackIndex)
            delay(16)
            itemFocusRequesters[selectedTrackIndex].requestFocus()
        } else {
            closeFocusRequester.requestFocus()
        }
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(12.dp))
                .background(DialogBackground)
                .border(1.dp, SecondaryBorderColor, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Subtitle Tracks",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availableTracks.isEmpty()) {
                    Text(
                        text = "No subtitle tracks available",
                        color = MutedTextColor,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    if (!availableTracks.any { it.isSupported }) {
                        Text(
                            text = "No supported subtitle tracks available",
                            color = MutedTextColor,
                            fontSize = 12.sp,
                            fontFamily = AppTheme.fontFamily,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    if (hasSingleTrack) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.textSecondary)
                            )
                            Text(
                                text = "Only one subtitle track available",
                                color = MutedTextColor,
                                fontSize = 12.sp,
                                fontFamily = AppTheme.fontFamily
                            )
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        itemsIndexed(availableTracks) { index, track ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            val isSelectable = track.isSupported && !hasSingleTrack
                            val isSelected = track.isSelected || (hasSingleTrack && track.isSupported)
                            val borderColor =
                                if (isFocused && isSelectable) {
                                    FocusBorderColor
                                } else {
                                    SecondaryBorderColor
                                }
                            val backgroundColor =
                                if (isSelected) AppTheme.colors.panelBackground
                                else ContentAreaBackground

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(backgroundColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .then(
                                        if (isSelectable) {
                                            Modifier.clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                onTrackSelected(track.groupIndex, track.trackIndex)
                                                onDismiss()
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(16.dp)
                                    .then(
                                        if (isSelectable) {
                                            Modifier
                                                .focusRequester(itemFocusRequesters[index])
                                                .focusable(interactionSource = interactionSource)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(EnabledIndicatorColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Text(
                                    text = track.label,
                                    color = if (!track.isSupported || hasSingleTrack) {
                                        MutedTextColor
                                    } else {
                                        AppTheme.colors.textPrimary
                                    },
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily
                                )

                                if (track.language != "Unknown" && !track.label.contains(track.language)) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${track.language})",
                                        color = MutedTextColor,
                                        fontSize = 12.sp,
                                        fontFamily = AppTheme.fontFamily
                                    )
                                }

                                if (!track.isSupported) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Unsupported",
                                        color = MutedTextColor,
                                        fontSize = 12.sp,
                                        fontFamily = AppTheme.fontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FocusableButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.borderStrong,
                        contentColor = AppTheme.colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .focusRequester(closeFocusRequester)
                ) {
                    Text(
                        text = "Close",
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AudioTrackDialog(
    availableTracks: List<com.example.xtreamplayer.player.AudioTrackInfo>,
    onTrackSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val closeFocusRequester = remember { FocusRequester() }
    val hasSingleTrack = availableTracks.size == 1
    val supportedTrackCount = availableTracks.count { it.isSupported }
    val hasSupportedTracks = supportedTrackCount > 0

    LaunchedEffect(Unit) {
        closeFocusRequester.requestFocus()
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(12.dp))
                .background(DialogBackground)
                .border(1.dp, SecondaryBorderColor, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Audio Tracks",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availableTracks.isEmpty()) {
                    Text(
                        text = "No audio tracks available",
                        color = MutedTextColor,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    if (!hasSupportedTracks) {
                        Text(
                            text = "No supported audio tracks available",
                            color = MutedTextColor,
                            fontSize = 12.sp,
                            fontFamily = AppTheme.fontFamily,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    if (hasSingleTrack) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.textSecondary)
                            )
                            Text(
                                text = "Only one audio track available",
                                color = MutedTextColor,
                                fontSize = 12.sp,
                                fontFamily = AppTheme.fontFamily
                            )
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(availableTracks) { track ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            val isSelectable = track.isSupported && !hasSingleTrack
                            val borderColor = if (isFocused && isSelectable) {
                                FocusBorderColor
                            } else {
                                SecondaryBorderColor
                            }
                            val backgroundColor = if (track.isSelected) {
                                AppTheme.colors.panelBackground
                            } else {
                                ContentAreaBackground
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(backgroundColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .then(
                                        if (isSelectable) {
                                            Modifier.clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                onTrackSelected(track.groupIndex, track.trackIndex)
                                                onDismiss()
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(16.dp)
                                    .then(
                                        if (isSelectable) {
                                            Modifier.focusable(interactionSource = interactionSource)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (track.isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(EnabledIndicatorColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Text(
                                    text = track.label,
                                    color = if (!track.isSupported || hasSingleTrack) {
                                        MutedTextColor
                                    } else {
                                        AppTheme.colors.textPrimary
                                    },
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily
                                )

                                if (track.language != track.label && track.language != "Unknown") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${track.language})",
                                        color = MutedTextColor,
                                        fontSize = 12.sp,
                                        fontFamily = AppTheme.fontFamily
                                    )
                                }

                                if (!track.isSupported) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Unsupported",
                                        color = MutedTextColor,
                                        fontSize = 12.sp,
                                        fontFamily = AppTheme.fontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FocusableButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.borderStrong,
                        contentColor = AppTheme.colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .focusRequester(closeFocusRequester)
                ) {
                    Text(
                        text = "Close",
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AudioBoostDialog(
    boostDb: Float,
    onBoostChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val maxBoostDb = 12f
    var localBoost by remember { mutableFloatStateOf(boostDb.coerceIn(0f, maxBoostDb)) }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(boostDb) {
        localBoost = boostDb.coerceIn(0f, maxBoostDb)
    }

    LaunchedEffect(Unit) {
        closeFocusRequester.requestFocus()
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .clip(RoundedCornerShape(12.dp))
                .background(DialogBackground)
                .border(1.dp, SecondaryBorderColor, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Audio Boost",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Increase source volume. High values can distort audio.",
                    color = MutedTextColor,
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FocusableButton(
                        onClick = {
                            val newBoost = (localBoost - 1f).coerceAtLeast(0f)
                            localBoost = newBoost
                            onBoostChange(newBoost)
                        },
                        enabled = localBoost > 0f,
                        colors = ButtonDefaults.buttonColors(containerColor = InputBackground)
                    ) {
                        Text("-", color = InputTextColor, fontSize = 18.sp)
                    }

                    Text(
                        text = "${localBoost.toInt()} dB",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )

                    FocusableButton(
                        onClick = {
                            val newBoost = (localBoost + 1f).coerceAtMost(maxBoostDb)
                            localBoost = newBoost
                            onBoostChange(newBoost)
                        },
                        enabled = localBoost < maxBoostDb,
                        colors = ButtonDefaults.buttonColors(containerColor = InputBackground)
                    ) {
                        Text("+", color = InputTextColor, fontSize = 18.sp)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FocusableButton(
                        onClick = {
                            localBoost = 0f
                            onBoostChange(0f)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = InputBackground)
                    ) {
                        Text("Reset", color = InputTextColor, fontSize = 14.sp)
                    }

                    FocusableButton(
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(closeFocusRequester),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButtonColor)
                    ) {
                        Text("Close", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackSettingsDialog(
    speedLabel: String,
    resolutionLabel: String,
    matchFrameRateLabel: String,
    nerdStatsLabel: String,
    showSpeedOption: Boolean,
    onSpeed: () -> Unit,
    onResolution: () -> Unit,
    onToggleMatchFrameRate: () -> Unit,
    onToggleNerdStats: () -> Unit,
    onDismiss: () -> Unit
) {
    val speedFocusRequester = remember { FocusRequester() }
    val resolutionFocusRequester = remember { FocusRequester() }
    val matchFrameRateFocusRequester = remember { FocusRequester() }
    val nerdStatsFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showSpeedOption) {
        if (showSpeedOption) {
            speedFocusRequester.requestFocus()
        } else {
            resolutionFocusRequester.requestFocus()
        }
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .clip(RoundedCornerShape(12.dp))
                .background(DialogBackground)
                .border(1.dp, SecondaryBorderColor, RoundedCornerShape(12.dp))
                .padding(24.dp)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                if (showSpeedOption) {
                    SettingsOptionRow(
                        label = "Speed",
                        value = speedLabel,
                        focusRequester = speedFocusRequester,
                        onClick = onSpeed
                    )
                }
                SettingsOptionRow(
                    label = "Resolution",
                    value = resolutionLabel,
                    focusRequester = resolutionFocusRequester,
                    onClick = onResolution
                )
                SettingsOptionRow(
                    label = "Match frame rate",
                    value = matchFrameRateLabel,
                    focusRequester = matchFrameRateFocusRequester,
                    onClick = onToggleMatchFrameRate
                )
                SettingsOptionRow(
                    label = "Stats for nerds",
                    value = nerdStatsLabel,
                    focusRequester = nerdStatsFocusRequester,
                    onClick = onToggleNerdStats
                )

                Spacer(modifier = Modifier.height(4.dp))

                FocusableButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.borderStrong,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .focusRequester(closeFocusRequester)
                ) {
                    Text(
                        text = "Close",
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val closeFocusRequester = remember { FocusRequester() }
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    LaunchedEffect(Unit) {
        closeFocusRequester.requestFocus()
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .clip(RoundedCornerShape(12.dp))
                .background(DialogBackground)
                .border(1.dp, SecondaryBorderColor, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Playback Speed",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(speeds) { speed ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()
                        val isSelected = abs(speed - currentSpeed) < 0.01f
                        val borderColor = if (isFocused) FocusBorderColor else SecondaryBorderColor
                        val backgroundColor =
                            if (isSelected) AppTheme.colors.panelBackground
                            else ContentAreaBackground

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(backgroundColor)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    onSpeedSelected(speed)
                                    onDismiss()
                                }
                                .focusable(interactionSource = interactionSource)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(EnabledIndicatorColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = "${speed}x",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = AppTheme.fontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FocusableButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.borderStrong,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .focusRequester(closeFocusRequester)
                ) {
                    Text(
                        text = "Close",
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun VideoResolutionDialog(
    availableTracks: List<VideoTrackInfo>,
    onTrackSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val closeFocusRequester = remember { FocusRequester() }
    val supportedTrackCount = availableTracks.count { it.isSupported }
    val hasSingleTrack = supportedTrackCount <= 1

    LaunchedEffect(Unit) {
        closeFocusRequester.requestFocus()
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(12.dp))
                .background(DialogBackground)
                .border(1.dp, SecondaryBorderColor, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Resolution",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availableTracks.isEmpty()) {
                    Text(
                        text = "No video tracks available",
                        color = MutedTextColor,
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    if (!availableTracks.any { it.isSupported }) {
                        Text(
                            text = "No supported video tracks available",
                            color = MutedTextColor,
                            fontSize = 12.sp,
                            fontFamily = AppTheme.fontFamily,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    if (hasSingleTrack) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.textSecondary)
                            )
                            Text(
                                text = "Only one resolution available",
                                color = MutedTextColor,
                                fontSize = 12.sp,
                                fontFamily = AppTheme.fontFamily
                            )
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(availableTracks) { track ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            val isSelectable = track.isSupported && !hasSingleTrack
                            val isSelected = track.isSelected || (hasSingleTrack && track.isSupported)
                            val borderColor =
                                if (isFocused && isSelectable) {
                                    FocusBorderColor
                                } else {
                                    SecondaryBorderColor
                                }
                            val backgroundColor =
                                if (isSelected) AppTheme.colors.panelBackground
                                else ContentAreaBackground

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(backgroundColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .then(
                                        if (isSelectable) {
                                            Modifier.clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                onTrackSelected(track.groupIndex, track.trackIndex)
                                                onDismiss()
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(16.dp)
                                    .then(
                                        if (isSelectable) {
                                            Modifier.focusable(interactionSource = interactionSource)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(EnabledIndicatorColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Text(
                                    text = track.label,
                                    color = if (!track.isSupported || hasSingleTrack) {
                                        MutedTextColor
                                    } else {
                                        Color.White
                                    },
                                    fontSize = 14.sp,
                                    fontFamily = AppTheme.fontFamily
                                )

                                if (!track.isSupported) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Unsupported",
                                        color = MutedTextColor,
                                        fontSize = 12.sp,
                                        fontFamily = AppTheme.fontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FocusableButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.borderStrong,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .focusRequester(closeFocusRequester)
                ) {
                    Text(
                        text = "Close",
                        fontSize = 14.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsOptionRow(
    label: String,
    value: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) FocusBorderColor else SecondaryBorderColor
    val backgroundColor =
        if (isFocused) AppTheme.colors.panelBackground else ContentAreaBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = MutedTextColor,
            fontSize = 12.sp,
            fontFamily = AppTheme.fontFamily
        )
    }
}

@Composable
fun SubtitleOffsetDialog(
    offsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, end = 20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            SubtitleOffsetControls(
                offsetMs = offsetMs,
                onOffsetChange = onOffsetChange,
                onDismiss = onDismiss,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun SubtitleOffsetControls(
    offsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onCancel: (() -> Unit)?
) {
    val smallStep = 100L  // 0.1 seconds
    val largeStep = 500L  // 0.5 seconds
    val minusLargeRequester = remember { FocusRequester() }
    val plusLargeRequester = remember { FocusRequester() }
    val minusSmallRequester = remember { FocusRequester() }
    val resetRequester = remember { FocusRequester() }
    val plusSmallRequester = remember { FocusRequester() }
    val cancelRequester = remember { FocusRequester() }
    val doneRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60)
        minusLargeRequester.requestFocus()
    }

    val offsetSeconds = offsetMs / 1000f
    val offsetText = when {
        offsetMs == 0L -> "0.0s"
        offsetMs > 0 -> "+${String.format(Locale.US, "%.1f", offsetSeconds)}s"
        else -> "${String.format(Locale.US, "%.1f", offsetSeconds)}s"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.30f)
            .clip(RoundedCornerShape(12.dp))
            .background(DialogBackground)
            .border(1.dp, SecondaryBorderColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Subtitle Timing",
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Adjust subtitle sync offset.",
                color = MutedTextColor,
                fontSize = 12.sp,
                fontFamily = AppTheme.fontFamily
            )

            Text(
                text = offsetText,
                color = PrimaryButtonColor,
                fontSize = 18.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OffsetButton(
                    text = "-0.5s",
                    onClick = { onOffsetChange(offsetMs - largeStep) },
                    focusRequester = minusLargeRequester
                )
                OffsetButton(
                    text = "+0.5s",
                    onClick = { onOffsetChange(offsetMs + largeStep) },
                    focusRequester = plusLargeRequester
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OffsetButton(
                    text = "-0.1s",
                    onClick = { onOffsetChange(offsetMs - smallStep) },
                    small = true,
                    focusRequester = minusSmallRequester
                )
                OffsetButton(
                    text = "Reset",
                    onClick = { onOffsetChange(0L) },
                    small = true,
                    focusRequester = resetRequester
                )
                OffsetButton(
                    text = "+0.1s",
                    onClick = { onOffsetChange(offsetMs + smallStep) },
                    small = true,
                    focusRequester = plusSmallRequester
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogCloseButton(
                    focusRequester = cancelRequester,
                    onDismiss = { (onCancel ?: onDismiss).invoke() },
                    onNavigateUp = { plusSmallRequester.requestFocus() },
                    label = "Cancel",
                    modifier = Modifier.fillMaxWidth(0.32f)
                )
                DialogCloseButton(
                    focusRequester = doneRequester,
                    onDismiss = onDismiss,
                    onNavigateUp = { plusSmallRequester.requestFocus() },
                    label = "Done",
                    modifier = Modifier.fillMaxWidth(0.32f)
                )
            }
        }
    }
}

@Composable
private fun OffsetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor = if (isFocused) PrimaryButtonColor else SecondaryBorderColor
    val textColor = if (isFocused) Color.Black else Color.White

    val baseModifier = modifier
        .clip(RoundedCornerShape(6.dp))
        .background(bgColor)
        .focusable(interactionSource = interactionSource)
        .onPreviewKeyEvent { event ->
            val isSelectKey =
                event.key == Key.Enter ||
                    event.key == Key.NumPadEnter ||
                    event.key == Key.DirectionCenter
            if (event.type == KeyEventType.KeyDown && isSelectKey) {
                onClick()
                true
            } else {
                false
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                focusRequester?.requestFocus()
                onClick()
            }
        )
        .padding(
            horizontal = if (small) 8.dp else 12.dp,
            vertical = if (small) 4.dp else 6.dp
        )

    Box(
        modifier = if (focusRequester != null) {
            baseModifier.focusRequester(focusRequester)
        } else {
            baseModifier
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (small) 11.sp else 13.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}

package com.example.xtreamplayer.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.xtreamplayer.api.SubtitleSearchResult

private val DialogBackground = Color(0xFF0F1626)
private val SecondaryBorderColor = Color(0xFF2A3348)
private val PrimaryButtonColor = Color(0xFF4F8CFF)
private val MutedTextColor = Color(0xFF94A3B8)
private val FocusBorderColor = Color(0xFFB6D9FF)
private val EnabledIndicatorColor = Color(0xFF3CCB7F)
private val DisabledIndicatorColor = Color(0xFF8A95B1)
private val InputBackground = Color(0xFF222E44)
private val InputTextColor = Color(0xFFE6ECF7)
private val ContentAreaBackground = Color(0xFF161E2E)

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
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    val searchFocusRequester = remember { FocusRequester() }
    val searchButtonFocusRequester = remember { FocusRequester() }
    val toggleFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Dialog(
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
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
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
                            fontFamily = FontFamily.Serif
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
                            contentColor = Color(0xFF0C1730),
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
                            text = "Search",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val toggleIndicatorColor =
                        if (subtitlesEnabled) EnabledIndicatorColor else DisabledIndicatorColor
                    val toggleButtonColor =
                        if (subtitlesEnabled) Color(0xFF1F3A2A) else SecondaryBorderColor

                    FocusableButton(
                        onClick = onToggleSubtitles,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = toggleButtonColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .focusRequester(toggleFocusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            searchButtonFocusRequester.requestFocus()
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
                                fontFamily = FontFamily.Serif,
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
                                fontFamily = FontFamily.Serif,
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
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                        is SubtitleDialogState.Results -> {
                            if (state.subtitles.isEmpty()) {
                                Text(
                                    text = "No subtitles found",
                                    color = MutedTextColor,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Serif,
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
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                        is SubtitleDialogState.Error -> {
                            Text(
                                text = state.message,
                                color = Color(0xFFE4A9A9),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                FocusableButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryBorderColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.End)
                        .focusRequester(closeFocusRequester)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                searchButtonFocusRequester.requestFocus()
                                true
                            } else false
                        }
                ) {
                    Text(
                        text = "Close",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (isFocused) FocusBorderColor else SecondaryBorderColor

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = InputTextColor,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif
        ),
        cursorBrush = SolidColor(FocusBorderColor),
        modifier = modifier
            .clip(shape)
            .background(InputBackground)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            onSearch()
                            true
                        }
                        Key.DirectionRight -> {
                            onMoveRight()
                            true
                        }
                        else -> false
                    }
                } else false
            },
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "Search for subtitles...",
                        color = MutedTextColor,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun SubtitleOptionsDialog(
    subtitlesEnabled: Boolean,
    onToggleSubtitles: () -> Unit,
    onDownloadSubtitles: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryFocusRequester = remember { FocusRequester() }
    val secondaryFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        primaryFocusRequester.requestFocus()
    }

    Dialog(
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
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                val statusColor = if (subtitlesEnabled) EnabledIndicatorColor else DisabledIndicatorColor
                val toggleColor = if (subtitlesEnabled) SecondaryBorderColor else PrimaryButtonColor
                val toggleTextColor = if (subtitlesEnabled) Color.White else Color(0xFF0C1730)

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
                        fontFamily = FontFamily.Serif
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
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FocusableButton(
                    onClick = onDownloadSubtitles,
                    modifier = Modifier.focusRequester(secondaryFocusRequester),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryBorderColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Download subtitles",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FocusableButton(
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(closeFocusRequester),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E2438),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Close",
                        fontFamily = FontFamily.Serif,
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
    val inputFocusRequester = remember { FocusRequester() }
    val userAgentFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    Dialog(
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
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Get your free API key at opensubtitles.com",
                    color = MutedTextColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "API key",
                    color = MutedTextColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif
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
                        fontFamily = FontFamily.Serif
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
                                    fontFamily = FontFamily.Serif
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
                    fontFamily = FontFamily.Serif
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
                        fontFamily = FontFamily.Serif
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
                                    fontFamily = FontFamily.Serif
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
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    FocusableButton(
                        onClick = { onSave(apiKey, userAgent) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryButtonColor,
                            contentColor = Color(0xFF0C1730)
                        ),
                        modifier = Modifier.focusRequester(saveFocusRequester)
                    ) {
                        Text(
                            text = "Save",
                            fontFamily = FontFamily.Serif,
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
    val backgroundColor = if (isFocused) Color(0xFF2A3A5E) else Color.Transparent

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
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Row {
                Text(
                    text = subtitle.languageName.uppercase(),
                    color = PrimaryButtonColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${subtitle.downloadCount} downloads",
                    color = MutedTextColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif
                )
                if (subtitle.hearingImpaired) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HI",
                        color = Color(0xFFFFB74D),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
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

    Dialog(
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
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availableTracks.isEmpty()) {
                    Text(
                        text = "No audio tracks available",
                        color = MutedTextColor,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    if (!hasSupportedTracks) {
                        Text(
                            text = "No supported audio tracks available",
                            color = MutedTextColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
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
                                    .background(Color(0xFF94A3B8))
                            )
                            Text(
                                text = "Only one audio track available",
                                color = MutedTextColor,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif
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
                                Color(0xFF1E2738)
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
                                        Color.White
                                    },
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Serif
                                )

                                if (track.language != track.label && track.language != "Unknown") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${track.language})",
                                        color = MutedTextColor,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Serif
                                    )
                                }

                                if (!track.isSupported) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Unsupported",
                                        color = MutedTextColor,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Serif
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
                        containerColor = Color(0xFF2A3348),
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
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

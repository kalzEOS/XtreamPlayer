package com.example.xtreamplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.example.xtreamplayer.ui.FocusableButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.auth.AuthUiState
import com.example.xtreamplayer.ui.theme.AppTheme

@Composable
fun LoginScreen(
    authState: AuthUiState,
    initialConfig: AuthConfig?,
    onSignIn: (String, String, String, String) -> Unit,
    onOpenLocalFiles: () -> Unit
) {
    val initialKey = remember(initialConfig) {
        initialConfig?.let {
            "${it.listName}|${it.baseUrl}|${it.username}|${it.password}"
        } ?: "empty"
    }
    var listName by remember(initialKey) { mutableStateOf(initialConfig?.listName.orEmpty()) }
    var serviceUrl by remember(initialKey) { mutableStateOf(initialConfig?.baseUrl.orEmpty()) }
    var username by remember(initialKey) { mutableStateOf(initialConfig?.username.orEmpty()) }
    var password by remember(initialKey) { mutableStateOf(initialConfig?.password.orEmpty()) }

    val listNameFocusRequester = remember { FocusRequester() }
    val serviceUrlFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val submitFocusRequester = remember { FocusRequester() }
    val localFilesFocusRequester = remember { FocusRequester() }
    val loginButtonInteraction = remember { MutableInteractionSource() }
    val localFilesButtonInteraction = remember { MutableInteractionSource() }
    val isLoginButtonFocused by loginButtonInteraction.collectIsFocusedAsState()
    val isLocalFilesButtonFocused by localFilesButtonInteraction.collectIsFocusedAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    val buttonShape = RoundedCornerShape(50)
    val colors = AppTheme.colors
    val borderColor = if (isFocused) colors.focus else colors.border
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val isCompactHeight = screenHeightDp < 520.dp
    val verticalSpacing = if (isCompactHeight) 10.dp else 14.dp
    val panelPadding = if (isCompactHeight) 18.dp else 28.dp
    val titleSize = if (isCompactHeight) 20.sp else 24.sp
    val fieldTextStyle = TextStyle(fontFamily = AppTheme.fontFamily, fontSize = 16.sp)

    val canSubmit = listName.isNotBlank() &&
        serviceUrl.isNotBlank() &&
        username.isNotBlank() &&
        password.isNotBlank() &&
        !authState.isLoading

    LaunchedEffect(Unit) {
        listNameFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.background,
                            colors.backgroundAlt
                        )
                    )
                )
                .border(1.dp, borderColor, shape)
                .padding(panelPadding)
                .focusable(interactionSource = interactionSource)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            Text(
                text = "LOG IN",
                color = colors.textPrimary,
                fontSize = titleSize,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            TvTextField(
                value = listName,
                onValueChange = { listName = it },
                label = "List name",
                focusRequester = listNameFocusRequester,
                onMoveDown = { serviceUrlFocusRequester.requestFocus() },
                textStyle = fieldTextStyle
            )
            TvTextField(
                value = serviceUrl,
                onValueChange = { serviceUrl = it },
                label = "Xtream service URL",
                focusRequester = serviceUrlFocusRequester,
                onMoveUp = { listNameFocusRequester.requestFocus() },
                onMoveDown = { usernameFocusRequester.requestFocus() },
                textStyle = fieldTextStyle
            )
            TvTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                focusRequester = usernameFocusRequester,
                onMoveUp = { serviceUrlFocusRequester.requestFocus() },
                onMoveDown = { passwordFocusRequester.requestFocus() },
                textStyle = fieldTextStyle
            )
            TvTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                focusRequester = passwordFocusRequester,
                onMoveUp = { usernameFocusRequester.requestFocus() },
                onMoveDown = {
                    if (canSubmit) {
                        submitFocusRequester.requestFocus()
                    } else {
                        localFilesFocusRequester.requestFocus()
                    }
                },
                textStyle = fieldTextStyle
            )
            if (authState.errorMessage != null) {
                Text(
                    text = authState.errorMessage,
                    color = colors.error,
                    fontSize = 14.sp,
                    fontFamily = AppTheme.fontFamily
                )
            }
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color =
                                if (isLoginButtonFocused) colors.textPrimary else Color.Transparent,
                            shape = buttonShape
                        )
                        .padding(2.dp)
            ) {
                FocusableButton(
                    onClick = { onSignIn(listName, serviceUrl, username, password) },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.textOnAccent,
                        disabledContainerColor = colors.borderStrong,
                        disabledContentColor = colors.textTertiary
                    ),
                    showFocusBorder = false,
                    interactionSource = loginButtonInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .focusRequester(submitFocusRequester)
                        .onPreviewKeyEvent {
                            handleNavKey(
                                event = it,
                                onUp = { passwordFocusRequester.requestFocus() },
                                onDown = { localFilesFocusRequester.requestFocus() }
                            )
                        }
                ) {
                    Text(
                        text = if (authState.isLoading) "Logging in..." else "Log In",
                        fontSize = 16.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp
                    )
                }
            }
            Text(
                text = "Use your Xtream list URL, username, and password.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = AppTheme.fontFamily
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .height(1.dp)
                            .background(colors.borderStrong)
                )
                Text(
                    text = "OR",
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .height(1.dp)
                            .background(colors.borderStrong)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color =
                                if (isLocalFilesButtonFocused) colors.textPrimary
                                else Color.Transparent,
                            shape = buttonShape
                        )
                        .padding(2.dp)
            ) {
                FocusableButton(
                    onClick = onOpenLocalFiles,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.textOnAccent
                    ),
                    showFocusBorder = false,
                    interactionSource = localFilesButtonInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .focusRequester(localFilesFocusRequester)
                        .onPreviewKeyEvent {
                            handleNavKey(
                                event = it,
                                onUp = {
                                    if (canSubmit) {
                                        submitFocusRequester.requestFocus()
                                    } else {
                                        passwordFocusRequester.requestFocus()
                                    }
                                }
                            )
                        }
                ) {
                    Text(
                        text = "Play Local Files",
                        fontSize = 16.sp,
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp
                    )
                }
            }
            Text(
                text =
                    "Play Local Files can scan and play video/audio from internal storage and connected external drives.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = AppTheme.fontFamily,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun handleNavKey(
    event: KeyEvent,
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null
): Boolean {
    if (event.type != KeyEventType.KeyDown) {
        return false
    }
    return when (event.key) {
        Key.Tab, Key.DirectionDown -> {
            onDown?.invoke()
            onDown != null
        }
        Key.DirectionUp -> {
            onUp?.invoke()
            onUp != null
        }
        Key.DirectionLeft -> {
            onLeft?.invoke()
            onLeft != null
        }
        Key.DirectionRight -> {
            onRight?.invoke()
            onRight != null
        }
        else -> false
    }
}

@Composable
private fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    textStyle: TextStyle = TextStyle.Default
) {
    val wrapperInteractionSource = remember { MutableInteractionSource() }
    val isWrapperFocused by wrapperInteractionSource.collectIsFocusedAsState()
    val textFieldFocusRequester = remember { FocusRequester() }
    var isTextFieldActive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = AppTheme.colors
    val inputMethodManager = remember(context) {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    val showKeyboard = {
        @Suppress("DEPRECATION")
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    val activateTextField = {
        isTextFieldActive = true
        textFieldFocusRequester.requestFocus()
        showKeyboard()
    }

    // Wrapper border color changes based on focus state
    val borderColor =
        if (isWrapperFocused || isTextFieldActive) colors.focus else colors.borderStrong

    Box(
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusable(interactionSource = wrapperInteractionSource)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else when (event.key) {
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        activateTextField()
                        true
                    }
                    Key.DirectionUp -> {
                        onMoveUp?.invoke()
                        onMoveUp != null
                    }
                    Key.Tab, Key.DirectionDown -> {
                        onMoveDown?.invoke()
                        onMoveDown != null
                    }
                    else -> false
                }
            }
            .border(
                width = if (isWrapperFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(textFieldFocusRequester)
                .onPreviewKeyEvent { event ->
                    // When text field is active, handle navigation to exit it
                    if (!isTextFieldActive) return@onPreviewKeyEvent false
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            isTextFieldActive = false
                            onMoveUp?.invoke() ?: focusRequester.requestFocus()
                            true
                        }
                        Key.Tab, Key.DirectionDown -> {
                            isTextFieldActive = false
                            onMoveDown?.invoke() ?: focusRequester.requestFocus()
                            true
                        }
                        Key.Escape, Key.Back -> {
                            isTextFieldActive = false
                            focusRequester.requestFocus()
                            true
                        }
                        else -> false
                    }
                },
            textStyle = textStyle
        )
    }
}

package com.example.xtreamplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.auth.AuthUiState

@Composable
fun LoginScreen(
    authState: AuthUiState,
    initialConfig: AuthConfig?,
    onSignIn: (String, String, String, String) -> Unit
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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    val borderColor = if (isFocused) Color(0xFFB6D9FF) else Color(0xFF1E2738)
    val scrollState = rememberScrollState()
    val isCompactHeight = LocalConfiguration.current.screenHeightDp < 520
    val verticalSpacing = if (isCompactHeight) 10.dp else 14.dp
    val panelPadding = if (isCompactHeight) 18.dp else 28.dp
    val titleSize = if (isCompactHeight) 20.sp else 24.sp
    val fieldTextStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp)

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
                            Color(0xFF0F1626),
                            Color(0xFF141C2E)
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
                color = Color.White,
                fontSize = titleSize,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text("List name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(listNameFocusRequester)
                    .onPreviewKeyEvent {
                        handleNavKey(
                            event = it,
                            onDown = { serviceUrlFocusRequester.requestFocus() }
                        )
                    },
                textStyle = fieldTextStyle
            )
            OutlinedTextField(
                value = serviceUrl,
                onValueChange = { serviceUrl = it },
                label = { Text("Xtream service URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(serviceUrlFocusRequester)
                    .onPreviewKeyEvent {
                        handleNavKey(
                            event = it,
                            onUp = { listNameFocusRequester.requestFocus() },
                            onDown = { usernameFocusRequester.requestFocus() }
                        )
                    },
                textStyle = fieldTextStyle
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(usernameFocusRequester)
                    .onPreviewKeyEvent {
                        handleNavKey(
                            event = it,
                            onUp = { serviceUrlFocusRequester.requestFocus() },
                            onDown = { passwordFocusRequester.requestFocus() }
                        )
                    },
                textStyle = fieldTextStyle
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester)
                    .onPreviewKeyEvent {
                        handleNavKey(
                            event = it,
                            onUp = { usernameFocusRequester.requestFocus() },
                            onDown = { submitFocusRequester.requestFocus() }
                        )
                    },
                textStyle = fieldTextStyle
            )
            if (authState.errorMessage != null) {
                Text(
                    text = authState.errorMessage,
                    color = Color(0xFFE4A9A9),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif
                )
            }
            FocusableButton(
                onClick = { onSignIn(listName, serviceUrl, username, password) },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F8CFF),
                    contentColor = Color(0xFF0C1730),
                    disabledContainerColor = Color(0xFF2A3348),
                    disabledContentColor = Color(0xFF8A95B1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .focusRequester(submitFocusRequester)
                    .onPreviewKeyEvent {
                        handleNavKey(
                            event = it,
                            onUp = { passwordFocusRequester.requestFocus() }
                        )
                    }
            ) {
                Text(
                    text = if (authState.isLoading) "Logging in..." else "Log In",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                )
            }
            Text(
                text = "Use your Xtream list URL, username, and password.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif
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

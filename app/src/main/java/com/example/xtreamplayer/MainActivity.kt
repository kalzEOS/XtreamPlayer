package com.example.xtreamplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.xtreamplayer.ui.theme.XtreamPlayerTheme


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

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Left navigation rail (reversed structure)
        SideNav()

        // Main content here
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Content area",
                color = Color.White,
                fontSize = 22.sp
            )
        }
    }

    Column (
        modifier = Modifier.fillMaxSize()
    ) {
        // this is resesrved for the top area. Will leave empty for now untill app is done/functional
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp))
    }
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FocusItem("Item One", requestFocus = true)
        Spacer(modifier = Modifier.height(24.dp))
        FocusItem("Item Two")
    }
}

@Composable
fun SideNav() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(200.dp)
            .background(Color(0XFF1E1E1E))
    ) {
        NavItem(
            label = "All",
            requestFocus = true
            )
        NavItem(
            label = "Movies",
            requestFocus = false
        )
    }
}

@Composable
fun NavItem(
    label: String,
    requestFocus: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .padding(12.dp)
            .height(60.dp)
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .background(
                color = if (isFocused) Color(0xFF3D5AFE) else Color.DarkGray,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ){
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
@Composable
fun FocusItem(label: String, requestFocus: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .width(300.dp)
            .height(80.dp)
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .background(
                color = if (isFocused) Color(0xFF3D5AFE) else Color.DarkGray,
                shape = RoundedCornerShape(12.dp)

            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 20.sp
        )
    }
}

@Composable
fun HorizontalRow(requestFocus: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically

    ){
        FocusItem("A", requestFocus = requestFocus)
        FocusItem("B")
        FocusItem("C")
    }
}

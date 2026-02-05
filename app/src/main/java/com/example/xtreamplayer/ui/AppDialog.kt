package com.example.xtreamplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val appScale = LocalAppScale.current
    val baseDensity = LocalAppBaseDensity.current ?: LocalDensity.current
    val scaledDensity = remember(appScale, baseDensity) {
        Density(
            density = baseDensity.density * appScale.uiScale,
            fontScale = baseDensity.fontScale * appScale.uiScale * appScale.fontScale
        )
    }

    CompositionLocalProvider(LocalDensity provides baseDensity) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties
        ) {
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                content()
            }
        }
    }
}

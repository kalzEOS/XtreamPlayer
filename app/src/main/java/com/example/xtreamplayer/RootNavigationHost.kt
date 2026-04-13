package com.example.xtreamplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.example.xtreamplayer.content.ProgressiveSyncCoordinator
import com.example.xtreamplayer.content.ProgressiveSyncState
import com.example.xtreamplayer.settings.SettingsState
import com.example.xtreamplayer.ui.AppScale
import com.example.xtreamplayer.ui.LocalAppBaseDensity
import com.example.xtreamplayer.ui.LocalAppScale
import com.example.xtreamplayer.ui.components.AppBackground
import com.example.xtreamplayer.ui.theme.AppTheme
import com.example.xtreamplayer.ui.theme.XtreamPlayerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun RootNavigationHost(
    settings: SettingsState,
    isPlaybackActive: Boolean,
    isLegacySyncActive: Boolean,
    syncState: ProgressiveSyncState,
    progressiveSyncCoordinator: ProgressiveSyncCoordinator?,
    quickSearchReady: Boolean,
    coroutineScope: CoroutineScope,
    headerContent: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    XtreamPlayerTheme(appTheme = settings.appTheme, fontFamily = settings.appFont.fontFamily) {
        val baseDensity = LocalDensity.current
        val uiScale = settings.uiScale.coerceIn(0.7f, 1.3f)
        val fontScale = settings.fontScale.coerceIn(0.7f, 1.4f)
        val appScale = remember(uiScale, fontScale) { AppScale(uiScale, fontScale) }
        val scaledDensity = Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale * uiScale * fontScale
        )

        CompositionLocalProvider(
            LocalAppBaseDensity provides baseDensity,
            LocalAppScale provides appScale,
            LocalDensity provides scaledDensity
        ) {
            AppBackground {
                val colors = AppTheme.colors
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(72.dp)
                                .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            headerContent()
                        }
                    }

                    val shouldShowSyncUi =
                        !isPlaybackActive && (syncState.isProgressiveSyncActive() || isLegacySyncActive)

                    if (shouldShowSyncUi) {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.TopEnd),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (quickSearchReady) {
                                    Row(
                                        modifier =
                                            Modifier.background(
                                                    colors.success,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.textOnAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Quick Search Ready",
                                            fontSize = 12.sp,
                                            color = colors.textOnAccent,
                                            fontFamily = AppTheme.fontFamily
                                        )
                                    }
                                }

                                if (syncState.isProgressiveSyncActive()) {
                                    Row(
                                        modifier =
                                            Modifier.background(
                                                    colors.surfaceAlt,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .clickable {
                                                    coroutineScope.launch {
                                                        if (syncState.isPaused) {
                                                            progressiveSyncCoordinator?.resumeBackgroundSync()
                                                        } else {
                                                            progressiveSyncCoordinator?.pauseBackgroundSync()
                                                        }
                                                    }
                                                },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.textPrimary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val currentSection = syncState.currentSection
                                        val progress = currentSection?.let { syncState.sectionProgress[it] }
                                        val text =
                                            if (syncState.isPaused) {
                                                "Sync paused"
                                            } else if (currentSection != null && progress != null) {
                                                "Syncing ${currentSection.name.lowercase()}... (${progress.itemsIndexed} items)"
                                            } else {
                                                "Syncing library..."
                                            }
                                        Text(
                                            text,
                                            fontSize = 11.sp,
                                            color = colors.textPrimary,
                                            fontFamily = AppTheme.fontFamily
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = if (syncState.isPaused) "Resume" else "Pause",
                                            fontSize = 11.sp,
                                            color = colors.accent,
                                            fontFamily = AppTheme.fontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }

                    content()
                }
            }
        }
    }
}

private fun ProgressiveSyncState.isProgressiveSyncActive(): Boolean {
    return phase == com.example.xtreamplayer.content.SyncPhase.FAST_START ||
        phase == com.example.xtreamplayer.content.SyncPhase.BACKGROUND_FULL ||
        phase == com.example.xtreamplayer.content.SyncPhase.ON_DEMAND_BOOST ||
        phase == com.example.xtreamplayer.content.SyncPhase.PAUSED
}

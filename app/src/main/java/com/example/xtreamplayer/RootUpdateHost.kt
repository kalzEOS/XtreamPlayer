package com.example.xtreamplayer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.xtreamplayer.update.UpdateRelease
import com.example.xtreamplayer.update.compareVersions
import com.example.xtreamplayer.update.downloadUpdateApk
import com.example.xtreamplayer.update.fetchLatestRelease
import com.example.xtreamplayer.update.parseVersionParts
import com.example.xtreamplayer.ui.AppDialog
import com.example.xtreamplayer.ui.FocusableButton
import com.example.xtreamplayer.ui.theme.AppTheme
import com.example.xtreamplayer.viewmodel.UpdateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class RootUpdateCheckSource {
    MANUAL,
    STARTUP
}

@Composable
internal fun RootUpdateHost(
    updateViewModel: UpdateViewModel,
    appVersionName: String,
    updateHttpClient: okhttp3.OkHttpClient,
    isSignedIn: Boolean,
    onUpdateDownload: (UpdateRelease) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var updateUiState by updateViewModel.updateUiState
    var updateCheckJob by updateViewModel.updateCheckJob
    var startupUpdateCheckEnabled by updateViewModel.startupUpdateCheckEnabled
    var startupUpdateCheckHandled by updateViewModel.startupUpdateCheckHandled

    LaunchedEffect(startupUpdateCheckEnabled, startupUpdateCheckHandled, isSignedIn) {
        if (startupUpdateCheckHandled) return@LaunchedEffect
        val enabled = startupUpdateCheckEnabled ?: return@LaunchedEffect
        if (!enabled) {
            startupUpdateCheckHandled = true
            return@LaunchedEffect
        }
        if (!isSignedIn) return@LaunchedEffect
        startupUpdateCheckHandled = true
        checkForUpdates(
            context = context,
            coroutineScope = coroutineScope,
            updateHttpClient = updateHttpClient,
            appVersionName = appVersionName,
            updateUiState = { updateUiState },
            onUpdateUiStateChange = { updateUiState = it },
            updateCheckJob = { updateCheckJob },
            onUpdateCheckJobChange = { updateCheckJob = it },
            source = RootUpdateCheckSource.STARTUP
        )
    }

    val pendingRelease = updateUiState.pendingRelease
    if (updateUiState.showDialog && pendingRelease != null) {
        UpdatePromptDialog(
            release = pendingRelease,
            isDownloading = updateUiState.inProgress,
            onUpdate = { onUpdateDownload(pendingRelease) },
            onLater = { updateUiState = updateUiState.copy(showDialog = false) }
        )
    }
}

private fun checkForUpdates(
    context: Context,
    coroutineScope: CoroutineScope,
    updateHttpClient: okhttp3.OkHttpClient,
    appVersionName: String,
    updateUiState: () -> UpdateUiState,
    onUpdateUiStateChange: (UpdateUiState) -> Unit,
    updateCheckJob: () -> Job?,
    onUpdateCheckJobChange: (Job?) -> Unit,
    source: RootUpdateCheckSource
) {
    if (updateCheckJob()?.isActive == true) return
    onUpdateCheckJobChange(
        coroutineScope.launch {
            val result = runCatching { fetchLatestRelease(updateHttpClient) }
            val latest = result.getOrNull()
            if (latest == null) {
                if (source == RootUpdateCheckSource.MANUAL) {
                    Toast.makeText(context, "Update check failed", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            val localParts = parseVersionParts(appVersionName)
            if (localParts.isEmpty() || latest.versionParts.isEmpty()) {
                if (source == RootUpdateCheckSource.MANUAL) {
                    Toast.makeText(context, "Update info unavailable", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            if (compareVersions(localParts, latest.versionParts) >= 0) {
                if (source == RootUpdateCheckSource.MANUAL) {
                    Toast.makeText(context, "Already up to date", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            onUpdateUiStateChange(
                updateUiState().copy(
                    pendingRelease = latest,
                    showDialog = true
                )
            )
        }
    )
}

internal fun startUpdateDownload(
    context: Context,
    coroutineScope: CoroutineScope,
    updateHttpClient: okhttp3.OkHttpClient,
    updateUiState: () -> UpdateUiState,
    onUpdateUiStateChange: (UpdateUiState) -> Unit,
    release: UpdateRelease
) {
    if (updateUiState().inProgress) return
    onUpdateUiStateChange(updateUiState().copy(inProgress = true))
    coroutineScope.launch {
        val apkUri = runCatching {
            downloadUpdateApk(context, release, updateHttpClient)
        }.getOrNull()
        onUpdateUiStateChange(updateUiState().copy(inProgress = false))
        if (apkUri == null) {
            Toast.makeText(context, "Update download failed", Toast.LENGTH_SHORT).show()
            return@launch
        }
        if (!ensureInstallPermission(context)) {
            return@launch
        }
        onUpdateUiStateChange(updateUiState().copy(showDialog = false))
        launchApkInstall(context, apkUri)
    }
}

private fun launchApkInstall(context: Context, apkUri: android.net.Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(apkUri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun ensureInstallPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    if (context.packageManager.canRequestPackageInstalls()) return true
    val intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        "package:${context.packageName}".toUri()
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    Toast.makeText(
        context,
        "Allow installs from this app to continue",
        Toast.LENGTH_LONG
    ).show()
    return false
}

@Composable
private fun UpdatePromptDialog(
    release: UpdateRelease,
    isDownloading: Boolean,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val updateFocusRequester = remember { FocusRequester() }
    val laterFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isDownloading) {
        if (!isDownloading) {
            updateFocusRequester.requestFocus()
        }
    }

    AppDialog(
        onDismissRequest = onLater,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.40f)
                .widthIn(min = 360.dp, max = 680.dp)
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.borderStrong, shape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Update available",
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontFamily = AppTheme.fontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Version ${release.versionName} is ready to install.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = AppTheme.fontFamily
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isDownloading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.backgroundAlt)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Downloading update...",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = AppTheme.fontFamily
                    )
                    LinearProgressIndicator(
                        progress = { 0.35f },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                        trackColor = colors.surfaceAlt
                    )
                }
            } else {
                Text(
                    text = "Install now or choose Later.",
                    color = colors.textTertiary,
                    fontSize = 13.sp,
                    fontFamily = AppTheme.fontFamily
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FocusableButton(
                    onClick = onUpdate,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .focusRequester(updateFocusRequester),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.textOnAccent,
                        disabledContainerColor = colors.surfaceAlt,
                        disabledContentColor = colors.textTertiary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    focusBorderWidth = 1.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isDownloading) "Updating..." else "Update now",
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FocusableButton(
                    onClick = onLater,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .focusRequester(laterFocusRequester),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceAlt,
                        contentColor = colors.textPrimary,
                        disabledContainerColor = colors.surfaceAlt,
                        disabledContentColor = colors.textTertiary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    focusBorderWidth = 1.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Later",
                        fontFamily = AppTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

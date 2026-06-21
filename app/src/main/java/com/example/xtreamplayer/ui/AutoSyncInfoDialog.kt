package com.example.xtreamplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.xtreamplayer.ui.theme.AppTheme

@Composable
fun AutoSyncInfoDialog(onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val closeFocusRequester = remember { FocusRequester() }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "About Auto-Sync",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontFamily = AppTheme.fontFamily,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                InfoLine("What it does", "Runs a full library refresh (Movies, Series, and Live) on a repeating schedule, so your content stays up to date automatically.", colors)
                Spacer(modifier = Modifier.height(12.dp))
                InfoLine("Works when app is closed", "Syncs happen in the background even when you're not using the app. Your library is updated by the time you open it.", colors)
                Spacer(modifier = Modifier.height(12.dp))
                InfoLine("Requires network", "The sync only runs when your device has an active internet connection.", colors)
                Spacer(modifier = Modifier.height(12.dp))
                InfoLine("Conflict-free", "If you're actively using the app and a sync is already running, the scheduled sync will skip that round and retry at the next interval.", colors)
                Spacer(modifier = Modifier.height(12.dp))
                InfoLine("Manual sync still works", "You can always trigger an immediate sync from Settings using \"Sync library\".", colors)

                Spacer(modifier = Modifier.height(20.dp))

                DialogCloseButton(
                    focusRequester = closeFocusRequester,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, body: String, colors: com.example.xtreamplayer.ui.theme.AppColors) {
    Column {
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontFamily = AppTheme.fontFamily,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = body,
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontFamily = AppTheme.fontFamily
        )
    }
}

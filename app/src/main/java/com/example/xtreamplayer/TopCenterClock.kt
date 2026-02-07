package com.example.xtreamplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xtreamplayer.settings.ClockFormatOption
import com.example.xtreamplayer.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
internal fun TopCenterClock(
    clockFormat: ClockFormatOption,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val pattern = if (clockFormat == ClockFormatOption.AM_PM) "h:mm a" else "HH:mm"
    val formatter = remember(pattern) { SimpleDateFormat(pattern, Locale.getDefault()) }

    LaunchedEffect(clockFormat) {
        while (true) {
            val now = System.currentTimeMillis()
            nowMillis = now
            val delayMs = (60_000L - (now % 60_000L)).coerceAtLeast(250L)
            delay(delayMs)
        }
    }

    Box(
        modifier =
            modifier.clip(RoundedCornerShape(12.dp))
                .background(AppTheme.colors.surfaceAlt.copy(alpha = 0.78f))
                .border(1.dp, AppTheme.colors.borderStrong, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = formatter.format(Date(nowMillis)),
            color = AppTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}

package com.example.xtreamplayer

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.SystemClock
import com.example.xtreamplayer.content.ContentRepository
import com.example.xtreamplayer.observability.AppDiagnostics
import java.util.ArrayDeque
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

internal enum class PlaybackRecoveryAction {
    NONE,
    SOFT_RECOVERY,
    PROCESS_RESTART
}

internal class PlaybackRecoveryTracker(
    private val failureWindowMs: Long = 90_000L,
    private val softRecoveryCooldownMs: Long = 5 * 60_000L,
    private val processRestartWindowMs: Long = 60_000L,
    private val failureThreshold: Int = 4,
    private val distinctMediaThreshold: Int = 2
) {
    private data class FailureEvent(
        val mediaId: String,
        val occurredAtMs: Long
    )

    private val recentFailures = ArrayDeque<FailureEvent>()
    private var lastSoftRecoveryAtMs: Long? = null
    private var restartMonitorUntilMs: Long? = null

    fun recordFailure(mediaId: String?, nowMs: Long): PlaybackRecoveryAction {
        val normalizedMediaId = mediaId?.takeIf { it.isNotBlank() } ?: UNKNOWN_MEDIA_ID
        pruneOldFailures(nowMs)

        val restartMonitorUntil = restartMonitorUntilMs
        if (restartMonitorUntil != null && nowMs <= restartMonitorUntil) {
            recentFailures.clear()
            return PlaybackRecoveryAction.PROCESS_RESTART
        }

        recentFailures.addLast(FailureEvent(mediaId = normalizedMediaId, occurredAtMs = nowMs))
        pruneOldFailures(nowMs)

        val distinctMediaCount = recentFailures.asSequence().map { it.mediaId }.distinct().count()
        if (recentFailures.size < failureThreshold || distinctMediaCount < distinctMediaThreshold) {
            return PlaybackRecoveryAction.NONE
        }

        val lastSoftRecovery = lastSoftRecoveryAtMs
        if (lastSoftRecovery != null && nowMs - lastSoftRecovery < softRecoveryCooldownMs) {
            return PlaybackRecoveryAction.NONE
        }

        return PlaybackRecoveryAction.SOFT_RECOVERY
    }

    fun markSoftRecoveryPerformed(nowMs: Long) {
        lastSoftRecoveryAtMs = nowMs
        restartMonitorUntilMs = nowMs + processRestartWindowMs
        recentFailures.clear()
    }

    fun markPlaybackHealthy() {
        recentFailures.clear()
        restartMonitorUntilMs = null
    }

    private fun pruneOldFailures(nowMs: Long) {
        while (recentFailures.isNotEmpty() && nowMs - recentFailures.first().occurredAtMs > failureWindowMs) {
            recentFailures.removeFirst()
        }
    }

    private companion object {
        const val UNKNOWN_MEDIA_ID = "unknown"
    }
}

internal class AppRecoveryManager(
    private val appContext: Context,
    private val contentRepository: ContentRepository,
    private val okHttpClient: OkHttpClient
) {
    suspend fun performSoftRecovery(reason: String) {
        withContext(Dispatchers.IO) {
            AppDiagnostics.recordWarning(
                event = "app_soft_recovery",
                fields = mapOf("reason" to reason)
            )
            okHttpClient.dispatcher.cancelAll()
            okHttpClient.connectionPool.evictAll()
            contentRepository.clearCache()
        }
    }

    fun restartApp(reason: String) {
        val launchIntent =
            appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ?: return
        val pendingIntent =
            PendingIntent.getActivity(
                appContext,
                RESTART_REQUEST_CODE,
                launchIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        if (alarmManager != null) {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + RESTART_DELAY_MS,
                pendingIntent
            )
        } else {
            appContext.startActivity(launchIntent)
        }
        AppDiagnostics.recordWarning(
            event = "app_process_restart",
            fields = mapOf("reason" to reason)
        )
        appContext.findActivity()?.finishAffinity()
        exitProcess(0)
    }

    private fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }

    private companion object {
        const val RESTART_REQUEST_CODE = 41_407
        const val RESTART_DELAY_MS = 300L
    }
}

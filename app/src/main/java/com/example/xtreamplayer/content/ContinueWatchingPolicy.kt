package com.example.xtreamplayer.content

private const val DEFAULT_MIN_WATCH_MS = 30_000L
private const val DEFAULT_COMPLETION_THRESHOLD_PERCENT = 98L

internal fun resolveContinueWatchingMinWatchMs(
    durationMs: Long,
    baseMinWatchMs: Long = DEFAULT_MIN_WATCH_MS
): Long {
    if (durationMs <= 0L) return baseMinWatchMs
    return minOf(baseMinWatchMs, durationMs / 10L)
}

internal fun shouldStoreContinueWatchingEntry(
    positionMs: Long,
    durationMs: Long,
    baseMinWatchMs: Long = DEFAULT_MIN_WATCH_MS,
    completionThresholdPercent: Long = DEFAULT_COMPLETION_THRESHOLD_PERCENT
): Boolean {
    if (positionMs < resolveContinueWatchingMinWatchMs(durationMs, baseMinWatchMs)) {
        return false
    }
    if (durationMs <= 0L) return true
    val progressPercent = (positionMs.coerceAtLeast(0L) * 100L) / durationMs.coerceAtLeast(1L)
    return progressPercent < completionThresholdPercent
}

package com.example.xtreamplayer.content

internal fun shouldRetryLiveEpgError(error: Throwable?): Boolean {
    if (error == null) return false
    if (error is java.io.IOException) return true
    val message = error.message?.lowercase().orEmpty()
    return message.contains("request failed: 5") ||
        message.contains("request failed: 429") ||
        message.contains("request failed: 408") ||
        message.contains("timeout") ||
        message.contains("temporar")
}

package com.example.xtreamplayer.content

import com.example.xtreamplayer.api.XtreamApi
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.observability.AppDiagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class LiveContentRepository(
    private val api: XtreamApi
) {
    private data class LiveEpgCacheEntry(val data: LiveNowNextEpg?, val cachedAtMs: Long)

    private val liveEpgCache =
        object : LinkedHashMap<String, LiveEpgCacheEntry>(100, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, LiveEpgCacheEntry>
            ): Boolean {
                return size > 100
            }
        }
    private val liveEpgMutex = Mutex()
    private val liveEpgInFlightMutex = Mutex()
    private val liveEpgInFlight =
        mutableMapOf<String, CompletableDeferred<Result<LiveNowNextEpg?>>>()

    suspend fun loadLiveNowNext(
        streamId: String,
        authConfig: AuthConfig
    ): Result<LiveNowNextEpg?> {
        if (streamId.isBlank()) {
            return Result.success(null)
        }
        val key = "live-epg-${accountKey(authConfig)}-$streamId"
        val now = System.currentTimeMillis()
        liveEpgMutex.withLock {
            val cached = liveEpgCache[key]
            if (cached != null && now - cached.cachedAtMs <= 20_000L) {
                return Result.success(cached.data)
            }
        }
        val (deferred, isRequestOwner) =
            liveEpgInFlightMutex.withLock {
                val existing = liveEpgInFlight[key]
                if (existing != null) {
                    existing to false
                } else {
                    val created = CompletableDeferred<Result<LiveNowNextEpg?>>()
                    liveEpgInFlight[key] = created
                    created to true
                }
            }
        if (!isRequestOwner) {
            return deferred.await()
        }
        try {
            val result = try {
                val networkResult = fetchLiveNowNextWithRetry(authConfig, streamId)
                if (networkResult.isSuccess) {
                    val data = networkResult.getOrNull()
                    liveEpgMutex.withLock {
                        liveEpgCache[key] =
                            LiveEpgCacheEntry(data = data, cachedAtMs = System.currentTimeMillis())
                    }
                    networkResult
                } else {
                    val staleEntry = liveEpgMutex.withLock { liveEpgCache[key] }
                    if (
                        staleEntry != null &&
                            now - staleEntry.cachedAtMs <= 3 * 60_000L
                    ) {
                        val error = networkResult.exceptionOrNull()
                        AppDiagnostics.recordWarning(
                            event = "live_epg_served_stale",
                            fields = mapOf(
                                "streamId" to streamId,
                                "error" to (error?.message ?: "unknown")
                            )
                        )
                        Timber.w(
                            error,
                            "Live EPG request failed for stream=$streamId; serving stale cache"
                        )
                        liveEpgMutex.withLock {
                            liveEpgCache[key] = staleEntry.copy(cachedAtMs = System.currentTimeMillis())
                        }
                        Result.success(staleEntry.data)
                    } else {
                        AppDiagnostics.recordError(
                            event = "live_epg_failed_no_cache",
                            throwable = networkResult.exceptionOrNull(),
                            fields = mapOf("streamId" to streamId)
                        )
                        networkResult
                    }
                }
            } catch (cancelled: CancellationException) {
                deferred.cancel(cancelled)
                throw cancelled
            } catch (error: Exception) {
                AppDiagnostics.recordError(
                    event = "live_epg_exception",
                    throwable = error,
                    fields = mapOf("streamId" to streamId)
                )
                Result.failure(error)
            }
            deferred.complete(result)
            return result
        } finally {
            liveEpgInFlightMutex.withLock {
                liveEpgInFlight.remove(key)
            }
        }
    }

    suspend fun clearCache() {
        liveEpgMutex.withLock { liveEpgCache.clear() }
    }

    private suspend fun fetchLiveNowNextWithRetry(
        authConfig: AuthConfig,
        streamId: String
    ): Result<LiveNowNextEpg?> {
        var attempt = 0
        var backoffMs = 250L
        var lastResult: Result<LiveNowNextEpg?> = Result.success(null)
        while (attempt <= 2) {
            val result = api.fetchLiveNowNext(authConfig, streamId)
            if (result.isSuccess) {
                return result
            }
            lastResult = result
            if (attempt >= 2 || !shouldRetryLiveEpgError(result.exceptionOrNull())) {
                break
            }
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(1_600L)
            attempt++
        }
        return lastResult
    }
}

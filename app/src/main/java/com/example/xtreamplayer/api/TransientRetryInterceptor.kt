package com.example.xtreamplayer.api

import java.io.IOException
import kotlin.math.min
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * Retries transient GET/HEAD requests (timeouts, connection resets, and retryable HTTP codes).
 */
class TransientRetryInterceptor(
    private val maxRetries: Int = 2,
    private val initialBackoffMs: Long = 400L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val safeUrl = redactUrl(request.url)
        if (request.method != "GET" && request.method != "HEAD") {
            return chain.proceed(request)
        }

        var attempt = 0
        var backoffMs = initialBackoffMs
        var lastError: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)
                if (!isRetryableStatus(response.code) || attempt >= maxRetries) {
                    return response
                }
                response.close()
                Timber.w(
                    "Retrying ${request.method} $safeUrl after HTTP ${response.code} " +
                        "(attempt ${attempt + 1}/$maxRetries)"
                )
            } catch (ioe: IOException) {
                lastError = ioe
                if (!isRetryableException(ioe) || attempt >= maxRetries) {
                    throw ioe
                }
                Timber.w(
                    ioe,
                    "Retrying ${request.method} $safeUrl after transient network failure " +
                        "(attempt ${attempt + 1}/$maxRetries)"
                )
            }

            sleepBackoff(backoffMs)
            attempt++
            backoffMs = min(backoffMs * 2, MAX_BACKOFF_MS)
        }

        throw lastError ?: IOException("Request failed after retries for $safeUrl")
    }

    private fun sleepBackoff(backoffMs: Long) {
        try {
            Thread.sleep(backoffMs)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun isRetryableStatus(code: Int): Boolean {
        return code == 408 || code == 429 || code in 500..599
    }

    private fun isRetryableException(error: IOException): Boolean {
        return error is java.net.SocketTimeoutException ||
            error is java.net.SocketException ||
            error is java.net.ConnectException ||
            error is java.net.UnknownHostException
    }

    private fun redactUrl(url: HttpUrl): String {
        // Never log credentials or other query params.
        return url.newBuilder()
            .query(null)
            .build()
            .toString()
    }

    private companion object {
        const val MAX_BACKOFF_MS = 2_000L
    }
}

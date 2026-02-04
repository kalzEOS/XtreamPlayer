package com.example.xtreamplayer.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber

data class SubtitleSearchResult(
    val id: String,
    val fileId: Long,
    val language: String,
    val languageName: String,
    val release: String,
    val downloadCount: Int,
    val hearingImpaired: Boolean,
    val fps: Double,
    val votes: Int,
    val rating: Double
)

data class SubtitleDownloadResult(
    val downloadUrl: String,
    val fileName: String
)

class OpenSubtitlesException(
    val code: Int,
    val apiMessage: String?
) : IllegalStateException(
    "OpenSubtitles error $code${apiMessage?.let { ": $it" } ?: ""}"
)

class OpenSubtitlesApi(
    private val client: OkHttpClient
) {
    private var authToken: String? = null
    private var tokenExpiry: Long = 0

    suspend fun search(
        apiKey: String,
        userAgent: String,
        query: String? = null,
        imdbId: String? = null,
        tmdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        languages: List<String> = listOf("en")
    ): Result<List<SubtitleSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder("$BASE_URL/subtitles?")
            val params = mutableListOf<String>()

            query?.let { params.add("query=${it.encodeUrl()}") }
            imdbId?.let { params.add("imdb_id=$it") }
            tmdbId?.let { params.add("tmdb_id=$it") }
            season?.let { params.add("season_number=$it") }
            episode?.let { params.add("episode_number=$it") }
            params.add("languages=${languages.joinToString(",")}")

            urlBuilder.append(params.joinToString("&"))

            val resolvedUserAgent = resolveUserAgent(userAgent)
            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Api-Key", apiKey)
                .addHeader("User-Agent", resolvedUserAgent)
                .addHeader("X-User-Agent", resolvedUserAgent)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = parseErrorMessage(body)
                    return@withContext Result.failure(
                        OpenSubtitlesException(response.code, message)
                    )
                }

                val json = JSONObject(body)
                val dataArray = json.optJSONArray("data") ?: return@withContext Result.success(emptyList())

                val results = mutableListOf<SubtitleSearchResult>()
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val attributes = item.getJSONObject("attributes")
                    val files = attributes.optJSONArray("files")
                    val fileId = files?.optJSONObject(0)?.optLong("file_id") ?: continue

                    results.add(
                        SubtitleSearchResult(
                            id = item.getString("id"),
                            fileId = fileId,
                            language = attributes.optString("language", "en"),
                            languageName = attributes.optString("language", "English"),
                            release = attributes.optString("release", "Unknown"),
                            downloadCount = attributes.optInt("download_count", 0),
                            hearingImpaired = attributes.optBoolean("hearing_impaired", false),
                            fps = attributes.optDouble("fps", 0.0),
                            votes = attributes.optInt("votes", 0),
                            rating = attributes.optDouble("ratings", 0.0)
                        )
                    )
                }

                Result.success(results.sortedByDescending { it.downloadCount })
            }
        } catch (e: Exception) {
            Timber.e(e, "Subtitle search failed")
            Result.failure(e)
        }
    }

    suspend fun getDownloadUrl(
        apiKey: String,
        userAgent: String,
        fileId: Long
    ): Result<SubtitleDownloadResult> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("file_id", fileId)
                put("sub_format", "srt")
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val resolvedUserAgent = resolveUserAgent(userAgent)
            val request = Request.Builder()
                .url("$BASE_URL/download")
                .addHeader("Api-Key", apiKey)
                .addHeader("User-Agent", resolvedUserAgent)
                .addHeader("X-User-Agent", resolvedUserAgent)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = parseErrorMessage(body)
                    return@withContext Result.failure(
                        OpenSubtitlesException(response.code, message)
                    )
                }

                val json = JSONObject(body)

                Result.success(
                    SubtitleDownloadResult(
                        downloadUrl = json.getString("link"),
                        fileName = json.optString("file_name", "subtitle.srt")
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get download URL")
            Result.failure(e)
        }
    }

    suspend fun downloadSubtitle(
        url: String,
        userAgent: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val resolvedUserAgent = resolveUserAgent(userAgent)
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", resolvedUserAgent)
                .addHeader("X-User-Agent", resolvedUserAgent)
                .addHeader("Accept", "application/octet-stream")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.bytes() ?: ByteArray(0)
                if (!response.isSuccessful) {
                    val message = parseErrorMessage(body.decodeToString())
                    return@withContext Result.failure(
                        OpenSubtitlesException(response.code, message)
                    )
                }

                Result.success(body)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to download subtitle")
            Result.failure(e)
        }
    }

    private fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, "UTF-8")

    companion object {
        private const val BASE_URL = "https://api.opensubtitles.com/api/v1"
        private const val USER_AGENT = "XtreamPlayer"
    }

    private fun resolveUserAgent(userAgent: String): String {
        return userAgent.ifBlank { USER_AGENT }
    }

    private fun parseErrorMessage(body: String): String? {
        if (body.isBlank()) {
            return null
        }
        return runCatching {
            val json = JSONObject(body)
            json.optString("message").ifBlank {
                json.optString("error")
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}

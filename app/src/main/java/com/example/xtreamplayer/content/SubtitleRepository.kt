package com.example.xtreamplayer.content

import android.content.Context
import android.net.Uri
import com.example.xtreamplayer.api.OpenSubtitlesApi
import com.example.xtreamplayer.api.OpenSubtitlesException
import com.example.xtreamplayer.api.SubtitleSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

data class CachedSubtitle(
    val uri: Uri,
    val language: String,
    val fileName: String
)

class SubtitleRepository(
    private val context: Context,
    private val api: OpenSubtitlesApi
) {
    private val subtitleDir: File
        get() = File(context.cacheDir, "subtitles").also { it.mkdirs() }

    suspend fun searchSubtitles(
        apiKey: String,
        userAgent: String,
        title: String,
        season: Int? = null,
        episode: Int? = null,
        languages: List<String> = listOf("en")
    ): Result<List<SubtitleSearchResult>> {
        return api.search(
            apiKey = apiKey,
            userAgent = userAgent,
            query = title,
            season = season,
            episode = episode,
            languages = languages
        )
    }

    suspend fun downloadAndCacheSubtitle(
        apiKey: String,
        userAgent: String,
        subtitle: SubtitleSearchResult,
        mediaId: String
    ): Result<CachedSubtitle> = withContext(Dispatchers.IO) {
        try {
            // Get download URL
            val downloadResult = retryForServerErrors {
                api.getDownloadUrl(apiKey, userAgent, subtitle.fileId)
            }
            if (downloadResult.isFailure) {
                return@withContext Result.failure(
                    downloadResult.exceptionOrNull() ?: Exception("Failed to get download URL")
                )
            }

            val downloadInfo = downloadResult.getOrThrow()

            // Download the subtitle content
            val contentResult = retryForServerErrors {
                api.downloadSubtitle(downloadInfo.downloadUrl, userAgent)
            }
            if (contentResult.isFailure) {
                return@withContext Result.failure(
                    contentResult.exceptionOrNull() ?: Exception("Failed to download subtitle")
                )
            }

            val content = contentResult.getOrThrow()

            // Save to cache
            val fileName = "${mediaId}_${subtitle.language}.srt"
            val file = File(subtitleDir, fileName)
            file.writeText(content)

            Timber.d("Subtitle cached: ${file.absolutePath}")

            Result.success(
                CachedSubtitle(
                    uri = Uri.fromFile(file),
                    language = subtitle.language,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to download and cache subtitle")
            Result.failure(e)
        }
    }

    private suspend fun <T> retryForServerErrors(
        attempts: Int = 3,
        initialDelayMs: Long = 500,
        block: suspend () -> Result<T>
    ): Result<T> {
        var delayMs = initialDelayMs
        repeat(attempts - 1) {
            val result = block()
            if (result.isSuccess) {
                return result
            }
            val error = result.exceptionOrNull()
            val code = (error as? OpenSubtitlesException)?.code
            if (code == null || code !in RETRYABLE_STATUS_CODES) {
                return result
            }
            delay(delayMs)
            delayMs *= 2
        }
        return block()
    }

    fun getCachedSubtitle(mediaId: String, language: String = "en"): CachedSubtitle? {
        val fileName = "${mediaId}_${language}.srt"
        val file = File(subtitleDir, fileName)
        return if (file.exists()) {
            CachedSubtitle(
                uri = Uri.fromFile(file),
                language = language,
                fileName = fileName
            )
        } else {
            null
        }
    }

    fun getCachedSubtitlesForMedia(mediaId: String): List<CachedSubtitle> {
        return subtitleDir.listFiles()
            ?.filter { it.name.startsWith("${mediaId}_") && it.name.endsWith(".srt") }
            ?.map { file ->
                val language = file.nameWithoutExtension.substringAfterLast("_")
                CachedSubtitle(
                    uri = Uri.fromFile(file),
                    language = language,
                    fileName = file.name
                )
            }
            ?: emptyList()
    }

    fun clearCache() {
        subtitleDir.listFiles()?.forEach { it.delete() }
    }

    fun clearCacheForMedia(mediaId: String) {
        subtitleDir.listFiles()
            ?.filter { it.name.startsWith("${mediaId}_") }
            ?.forEach { it.delete() }
    }

    private companion object {
        val RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503, 504)
    }
}

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
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

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
            val baseName = "${mediaId}_${subtitle.language}"
            val fallbackExt = downloadInfo.fileName.substringAfterLast('.', "srt")
                .lowercase()

            val (finalBytes, finalExt) = when {
                isZip(content) -> extractZipSubtitle(content, fallbackExt)
                isGzip(content) -> Pair(extractGzipSubtitle(content), fallbackExt)
                else -> Pair(content, fallbackExt)
            }

            val safeExt = finalExt.ifBlank { "srt" }
            val fileName = "$baseName.$safeExt"
            val file = File(subtitleDir, fileName)
            file.writeBytes(finalBytes)

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
            ?.filter { it.name.startsWith("${mediaId}_") }
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

    private fun isZip(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()
    }

    private fun isGzip(bytes: ByteArray): Boolean {
        return bytes.size >= 2 &&
            bytes[0] == 0x1F.toByte() &&
            bytes[1] == 0x8B.toByte()
    }

    private fun extractGzipSubtitle(bytes: ByteArray): ByteArray {
        GZIPInputStream(bytes.inputStream()).use { gzip ->
            return gzip.readBytes()
        }
    }

    private fun extractZipSubtitle(
        bytes: ByteArray,
        fallbackExt: String
    ): Pair<ByteArray, String> {
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            var firstFileBytes: ByteArray? = null
            var firstFileExt: String? = null
            while (entry != null) {
                if (!entry.isDirectory) {
                    val ext = entry.name.substringAfterLast('.', "").lowercase()
                    val data = zip.readBytes()
                    if (firstFileBytes == null) {
                        firstFileBytes = data
                        firstFileExt = ext
                    }
                    if (ext in setOf("srt", "vtt", "ass", "ssa", "ttml", "dfxp")) {
                        return Pair(data, ext)
                    }
                }
                entry = zip.nextEntry
            }
            val resolvedBytes = firstFileBytes ?: bytes
            val resolvedExt = firstFileExt?.ifBlank { fallbackExt } ?: fallbackExt
            return Pair(resolvedBytes, resolvedExt)
        }
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

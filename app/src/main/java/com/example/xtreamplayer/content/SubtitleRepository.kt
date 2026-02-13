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

private data class CachedSubtitleDescriptor(
    val mediaKey: String,
    val language: String
)

class SubtitleRepository(
    private val context: Context,
    private val api: OpenSubtitlesApi
) {
    @Volatile
    private var subtitleFilesSnapshot: List<File>? = null

    @Volatile
    private var subtitleDirLastModified: Long = -1L

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
            Timber.d("Downloaded subtitle content: ${content.size} bytes, first bytes: ${content.take(10).joinToString(" ") { String.format("%02X", it) }}")

            // OpenSubtitles sometimes returns HTTP 200 with an error message in the body
            // instead of a proper error code. Detect this by checking content size and content.
            if (content.size < 100) {
                val textContent = content.decodeToString()
                Timber.e("Subtitle content too small, likely an error: $textContent")
                if (textContent.contains("error", ignoreCase = true) ||
                    textContent.contains("limit", ignoreCase = true) ||
                    textContent.contains("expired", ignoreCase = true)) {
                    return@withContext Result.failure(
                        Exception("OpenSubtitles error: $textContent")
                    )
                }
                // Even if it doesn't contain "error", a subtitle file should be larger
                return@withContext Result.failure(
                    Exception("Downloaded content too small to be a valid subtitle (${content.size} bytes)")
                )
            }

            val safeMediaId = sanitizeFileComponent(mediaId, fallback = "media")
            val safeLanguage = sanitizeFileComponent(subtitle.language.lowercase(), fallback = "en")
            val baseName = "${safeMediaId}_${safeLanguage}"
            val rawFileName = downloadInfo.fileName
            val normalizedFileName =
                if (rawFileName.lowercase().endsWith(".gz")) {
                    rawFileName.dropLast(3)
                } else {
                    rawFileName
                }
            val extCandidate = normalizedFileName.substringAfterLast('.', "")
            val fallbackExt =
                if (normalizedFileName.contains('.') && extCandidate.isNotBlank()) {
                    extCandidate.lowercase()
                } else {
                    "srt"
                }

            Timber.d("Content detection: isZip=${isZip(content)}, isGzip=${isGzip(content)}")

            val (finalBytes, finalExt) = when {
                isZip(content) -> {
                    Timber.d("Extracting ZIP content")
                    extractZipSubtitle(content, fallbackExt)
                }
                isGzip(content) -> {
                    Timber.d("Extracting GZIP content")
                    val extracted = extractGzipSubtitle(content)
                    Timber.d("GZIP extracted: ${extracted.size} bytes")
                    Pair(extracted, fallbackExt)
                }
                else -> {
                    Timber.d("Using raw content (no compression detected)")
                    Pair(content, fallbackExt)
                }
            }

            Timber.d("Final subtitle content: ${finalBytes.size} bytes")

            val safeExt = sanitizeSubtitleExtension(finalExt)
            val fileName = "$baseName.$safeExt"
            val file = File(subtitleDir, fileName)
            file.writeBytes(finalBytes)
            invalidateSubtitleFileSnapshot()

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
        val safeMediaId = mediaCacheKey(mediaId)
        val safeLanguage = sanitizeFileComponent(language.lowercase(), fallback = "en")
        val fileName = "${safeMediaId}_${safeLanguage}.srt"
        val file = File(subtitleDir, fileName)
        return if (file.exists()) {
            CachedSubtitle(
                uri = Uri.fromFile(file),
                language = safeLanguage,
                fileName = fileName
            )
        } else {
            null
        }
    }

    fun getCachedSubtitlesForMedia(mediaId: String): List<CachedSubtitle> {
        val targetMediaKey = mediaCacheKey(mediaId)
        return listSubtitleFiles()
            .asSequence()
            .mapNotNull { file ->
                val descriptor = parseCachedSubtitleDescriptor(file) ?: return@mapNotNull null
                if (descriptor.mediaKey != targetMediaKey) return@mapNotNull null
                CachedSubtitle(
                    uri = Uri.fromFile(file),
                    language = descriptor.language,
                    fileName = file.name
                )
            }
            .toList()
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
        listSubtitleFiles().forEach { it.delete() }
        invalidateSubtitleFileSnapshot()
    }

    fun clearCacheAndCount(): Int {
        val files = listSubtitleFiles()
        var removed = 0
        files.forEach { file ->
            if (file.delete()) {
                removed += 1
            }
        }
        invalidateSubtitleFileSnapshot()
        return removed
    }

    fun clearCacheForMedia(mediaId: String) {
        val targetMediaKey = mediaCacheKey(mediaId)
        listSubtitleFiles()
            .filter { file ->
                parseCachedSubtitleDescriptor(file)?.mediaKey == targetMediaKey
            }
            .forEach { it.delete() }
        invalidateSubtitleFileSnapshot()
    }

    private fun listSubtitleFiles(): List<File> {
        val currentDir = subtitleDir
        val currentModified = currentDir.lastModified()
        val cached = subtitleFilesSnapshot
        if (cached != null && subtitleDirLastModified == currentModified) {
            return cached
        }

        val files = currentDir.listFiles()?.toList().orEmpty()
        subtitleFilesSnapshot = files
        subtitleDirLastModified = currentDir.lastModified()
        return files
    }

    private fun sanitizeFileComponent(raw: String, fallback: String): String {
        val normalized = raw
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("""\.\.+"""), "_")
            .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
            .trim('_', '.', ' ')
        return normalized.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun mediaCacheKey(mediaId: String): String {
        return sanitizeFileComponent(mediaId, fallback = "media")
    }

    private fun parseCachedSubtitleDescriptor(file: File): CachedSubtitleDescriptor? {
        val base = file.nameWithoutExtension
        val separator = base.lastIndexOf('_')
        if (separator <= 0 || separator >= base.lastIndex) return null
        val mediaKey = base.substring(0, separator)
        val language = base.substring(separator + 1)
        if (mediaKey.isBlank() || language.isBlank()) return null
        return CachedSubtitleDescriptor(mediaKey = mediaKey, language = language)
    }

    private fun sanitizeSubtitleExtension(rawExt: String): String {
        val cleaned = rawExt.lowercase().replace(Regex("""[^a-z0-9]"""), "")
        return when {
            cleaned in setOf("srt", "vtt", "ass", "ssa", "ttml", "dfxp") -> cleaned
            cleaned.isBlank() -> "srt"
            cleaned.length > 8 -> "srt"
            else -> cleaned
        }
    }

    private fun invalidateSubtitleFileSnapshot() {
        subtitleFilesSnapshot = null
        subtitleDirLastModified = -1L
    }

    private companion object {
        val RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503, 504)
    }
}

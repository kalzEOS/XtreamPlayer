package com.example.xtreamplayer.content

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.util.AtomicFile
import android.util.JsonReader
import android.util.JsonToken
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class ContentCache(context: Context) {
    private val cacheDir = File(context.filesDir, "content_cache").apply { mkdirs() }
    private companion object {
        private const val VOD_INFO_MAX_AGE_MS = 15L * 24 * 60 * 60 * 1000
        private const val SERIES_INFO_MAX_AGE_MS = 15L * 24 * 60 * 60 * 1000
        private const val MAX_CACHE_SIZE_BYTES = 256L * 1024L * 1024L
        private const val TRIM_TARGET_SIZE_BYTES = 220L * 1024L * 1024L
        private const val TRIM_INTERVAL_MS = 30_000L
    }
    @Volatile
    private var lastTrimTimestampMs: Long = 0L

    suspend fun readPage(
        section: Section,
        config: AuthConfig,
        page: Int,
        limit: Int
    ): ContentPage? {
        return withContext(Dispatchers.IO) {
            val file = fileFor(section, config, page, limit)
            if (!file.exists()) return@withContext null
            val text = runCatching { file.readText() }.getOrNull() ?: return@withContext null
            if (text.isBlank()) return@withContext null
            parsePage(text, limit)
        }
    }

    suspend fun readPage(
        cacheKey: String,
        config: AuthConfig,
        page: Int,
        limit: Int
    ): ContentPage? {
        return withContext(Dispatchers.IO) {
            val file = fileForKey(cacheKey, config, page, limit)
            if (!file.exists()) return@withContext null
            val text = runCatching { file.readText() }.getOrNull() ?: return@withContext null
            if (text.isBlank()) return@withContext null
            parsePage(text, limit)
        }
    }

    suspend fun writePage(
        section: Section,
        config: AuthConfig,
        page: Int,
        limit: Int,
        data: ContentPage
    ) {
        withContext(Dispatchers.IO) {
            val file = fileFor(section, config, page, limit)
            writeTextWithTrim(file, serializePage(data))
        }
    }

    suspend fun writePage(
        cacheKey: String,
        config: AuthConfig,
        page: Int,
        limit: Int,
        data: ContentPage
    ) {
        withContext(Dispatchers.IO) {
            val file = fileForKey(cacheKey, config, page, limit)
            writeTextWithTrim(file, serializePage(data))
        }
    }

    suspend fun readCategories(
        type: ContentType,
        config: AuthConfig
    ): List<CategoryItem>? {
        return withContext(Dispatchers.IO) {
            val file = categoriesFile(type, config)
            if (!file.exists()) return@withContext null
            val text = runCatching { file.readText() }.getOrNull() ?: return@withContext null
            if (text.isBlank()) return@withContext null
            runCatching {
                val array = JSONArray(text)
                val items = ArrayList<CategoryItem>(array.length())
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val id = obj.optString("id")
                    val name = obj.optString("name")
                    val typeName = obj.optString("type")
                    val contentType = runCatching { ContentType.valueOf(typeName) }
                        .getOrNull() ?: type
                    if (id.isNotBlank()) {
                        items.add(CategoryItem(id = id, name = name, type = contentType))
                    }
                }
                items
            }.getOrNull()
        }
    }

    suspend fun writeCategories(
        type: ContentType,
        config: AuthConfig,
        categories: List<CategoryItem>
    ) {
        withContext(Dispatchers.IO) {
            val file = categoriesFile(type, config)
            val array = JSONArray()
            categories.forEach { category ->
                val obj = JSONObject()
                obj.put("id", category.id)
                obj.put("name", category.name)
                obj.put("type", category.type.name)
                array.put(obj)
            }
            writeTextWithTrim(file, array.toString())
        }
    }

    suspend fun readVodInfo(
        vodId: String,
        config: AuthConfig
    ): MovieInfo? {
        return withContext(Dispatchers.IO) {
            val file = vodInfoFile(vodId, config)
            if (!file.exists()) return@withContext null
            val text = runCatching { file.readText() }.getOrNull() ?: return@withContext null
            if (text.isBlank()) return@withContext null
            runCatching {
                val obj = JSONObject(text)
                val timestamp = obj.optLong("cachedAt", 0L)
                if (timestamp == 0L || System.currentTimeMillis() - timestamp > VOD_INFO_MAX_AGE_MS) {
                    return@withContext null
                }
                MovieInfo(
                    director = obj.optString("director").ifBlank { null },
                    releaseDate = obj.optString("releaseDate").ifBlank { null },
                    duration = obj.optString("duration").ifBlank { null },
                    genre = obj.optString("genre").ifBlank { null },
                    cast = obj.optString("cast").ifBlank { null },
                    rating = obj.optString("rating").ifBlank { null },
                    description = obj.optString("description").ifBlank { null },
                    year = obj.optString("year").ifBlank { null },
                    videoCodec = obj.optString("videoCodec").ifBlank { null },
                    videoResolution = obj.optString("videoResolution").ifBlank { null },
                    videoHdr = obj.optString("videoHdr").ifBlank { null },
                    audioCodec = obj.optString("audioCodec").ifBlank { null },
                    audioChannels = obj.optString("audioChannels").ifBlank { null },
                    audioLanguages =
                        obj.optJSONArray("audioLanguages")?.let { array ->
                            List(array.length()) { index ->
                                array.optString(index)
                            }.filter { it.isNotBlank() }
                        } ?: emptyList()
                )
            }.getOrNull()
        }
    }

    suspend fun writeVodInfo(
        vodId: String,
        config: AuthConfig,
        info: MovieInfo
    ) {
        withContext(Dispatchers.IO) {
            val file = vodInfoFile(vodId, config)
            val obj = JSONObject()
            obj.put("director", info.director ?: "")
            obj.put("releaseDate", info.releaseDate ?: "")
            obj.put("duration", info.duration ?: "")
            obj.put("genre", info.genre ?: "")
            obj.put("cast", info.cast ?: "")
            obj.put("rating", info.rating ?: "")
            obj.put("description", info.description ?: "")
            obj.put("year", info.year ?: "")
            obj.put("videoCodec", info.videoCodec ?: "")
            obj.put("videoResolution", info.videoResolution ?: "")
            obj.put("videoHdr", info.videoHdr ?: "")
            obj.put("audioCodec", info.audioCodec ?: "")
            obj.put("audioChannels", info.audioChannels ?: "")
            val languageArray = JSONArray()
            info.audioLanguages.forEach { languageArray.put(it) }
            obj.put("audioLanguages", languageArray)
            obj.put("cachedAt", System.currentTimeMillis())
            writeTextWithTrim(file, obj.toString())
        }
    }

    suspend fun readSeriesInfo(
        seriesId: String,
        config: AuthConfig
    ): SeriesInfo? {
        return withContext(Dispatchers.IO) {
            val file = seriesInfoFile(seriesId, config)
            if (!file.exists()) return@withContext null
            val text = runCatching { file.readText() }.getOrNull() ?: return@withContext null
            if (text.isBlank()) return@withContext null
            runCatching {
                val obj = JSONObject(text)
                val timestamp = obj.optLong("cachedAt", 0L)
                if (timestamp == 0L || System.currentTimeMillis() - timestamp > SERIES_INFO_MAX_AGE_MS) {
                    return@withContext null
                }
                SeriesInfo(
                    director = obj.optString("director").ifBlank { null },
                    releaseDate = obj.optString("releaseDate").ifBlank { null },
                    duration = obj.optString("duration").ifBlank { null },
                    genre = obj.optString("genre").ifBlank { null },
                    cast = obj.optString("cast").ifBlank { null },
                    rating = obj.optString("rating").ifBlank { null },
                    description = obj.optString("description").ifBlank { null },
                    year = obj.optString("year").ifBlank { null }
                )
            }.getOrNull()
        }
    }

    suspend fun writeSeriesInfo(
        seriesId: String,
        config: AuthConfig,
        info: SeriesInfo
    ) {
        withContext(Dispatchers.IO) {
            val file = seriesInfoFile(seriesId, config)
            val obj = JSONObject()
            obj.put("director", info.director ?: "")
            obj.put("releaseDate", info.releaseDate ?: "")
            obj.put("duration", info.duration ?: "")
            obj.put("genre", info.genre ?: "")
            obj.put("cast", info.cast ?: "")
            obj.put("rating", info.rating ?: "")
            obj.put("description", info.description ?: "")
            obj.put("year", info.year ?: "")
            obj.put("cachedAt", System.currentTimeMillis())
            writeTextWithTrim(file, obj.toString())
        }
    }

    suspend fun readSeasonFull(
        seriesId: String,
        seasonLabel: String,
        config: AuthConfig
    ): List<ContentItem>? {
        return withContext(Dispatchers.IO) {
            val file = seasonFullFile(seriesId, seasonLabel, config)
            if (!file.exists()) return@withContext null
            parseIndexItems(file)
        }
    }

    suspend fun writeSeasonFull(
        seriesId: String,
        seasonLabel: String,
        config: AuthConfig,
        items: List<ContentItem>
    ) {
        withContext(Dispatchers.IO) {
            val file = seasonFullFile(seriesId, seasonLabel, config)
            val array = serializeItems(items)
            writeTextWithTrim(file, array.toString())
        }
    }

    suspend fun readSectionIndex(
        section: Section,
        config: AuthConfig
    ): List<ContentItem>? {
        val file = sectionIndexFile(section, config)
        return withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            parseIndexItems(file)
        }
    }

    suspend fun readSectionIndexStaging(
        section: Section,
        config: AuthConfig
    ): List<ContentItem>? {
        val file = sectionIndexStagingFile(section, config)
        return withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            parseIndexItems(file)
        }
    }

    suspend fun writeSectionIndex(
        section: Section,
        config: AuthConfig,
        items: List<ContentItem>
    ) {
        val file = sectionIndexFile(section, config)
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
            payload.put("items", serializeItems(items))
            writeTextWithTrim(file, payload.toString())
        }
    }

    suspend fun writeSectionIndexStaging(
        section: Section,
        config: AuthConfig,
        items: List<ContentItem>
    ) {
        val file = sectionIndexStagingFile(section, config)
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
            payload.put("items", serializeItems(items))
            writeTextWithTrim(file, payload.toString())
        }
    }

    suspend fun hasSectionIndex(section: Section, config: AuthConfig): Boolean {
        val file = sectionIndexFile(section, config)
        return withContext(Dispatchers.IO) { file.exists() }
    }

    suspend fun hasSectionIndexStaging(section: Section, config: AuthConfig): Boolean {
        val file = sectionIndexStagingFile(section, config)
        return withContext(Dispatchers.IO) { file.exists() }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            runCatching {
                val files = cacheDir.listFiles()
                files?.forEach { it.delete() }
            }
        }
    }

    suspend fun cacheSizeBytes(): Long {
        return withContext(Dispatchers.IO) { directorySizeBytes(cacheDir) }
    }

    private fun directorySizeBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.listFiles()?.sumOf { file -> directorySizeBytes(file) } ?: 0L
    }

    private fun writeTextWithTrim(file: File, payload: String) {
        runCatching {
            val atomicFile = AtomicFile(file)
            val encoded = payload.toByteArray(Charsets.UTF_8)
            var stream: java.io.FileOutputStream? = null
            try {
                stream = atomicFile.startWrite()
                stream.write(encoded)
                atomicFile.finishWrite(stream)
            } catch (error: Exception) {
                stream?.let { atomicFile.failWrite(it) }
                throw error
            }
            trimCacheIfNeeded()
        }
    }

    private fun trimCacheIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastTrimTimestampMs < TRIM_INTERVAL_MS) return
        lastTrimTimestampMs = now

        val files = cacheDir.listFiles()?.filter { it.isFile } ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= MAX_CACHE_SIZE_BYTES) return

        files.sortedBy { it.lastModified() }.forEach { file ->
            if (totalSize <= TRIM_TARGET_SIZE_BYTES) return
            val fileSize = file.length()
            runCatching { file.delete() }
                .onSuccess { totalSize -= fileSize }
        }
    }

    suspend fun clearFor(config: AuthConfig) {
        val accountHash = accountHash(config)
        withContext(Dispatchers.IO) {
            runCatching {
                val files = cacheDir.listFiles()
                files?.forEach { file ->
                    if (file.name.endsWith("_$accountHash.json")) {
                        file.delete()
                    }
                }
                refreshMarkerFile(config).delete()
            }
        }
    }

    suspend fun hasCacheFor(config: AuthConfig): Boolean {
        val accountHash = accountHash(config)
        return withContext(Dispatchers.IO) {
            val files = cacheDir.listFiles()
            files?.any { it.name.endsWith("_$accountHash.json") } == true
        }
    }

    suspend fun writeRefreshMarker(config: AuthConfig) {
        val marker = refreshMarkerFile(config)
        withContext(Dispatchers.IO) {
            writeTextWithTrim(marker, "refreshed")
        }
    }

    suspend fun hasRefreshMarker(config: AuthConfig): Boolean {
        val marker = refreshMarkerFile(config)
        return withContext(Dispatchers.IO) { marker.exists() }
    }

    suspend fun readCategoryThumbnail(
        type: ContentType,
        categoryId: String,
        config: AuthConfig
    ): String? {
        val file = categoryThumbnailFile(type, categoryId, config)
        return withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            val text = runCatching { file.readText() }.getOrNull()?.trim()
            if (text.isNullOrBlank()) null else text
        }
    }

    suspend fun writeCategoryThumbnail(
        type: ContentType,
        categoryId: String,
        config: AuthConfig,
        imageUrl: String?
    ) {
        val file = categoryThumbnailFile(type, categoryId, config)
        withContext(Dispatchers.IO) {
            writeTextWithTrim(file, imageUrl.orEmpty())
        }
    }

    private fun fileFor(section: Section, config: AuthConfig, page: Int, limit: Int): File {
        val key = accountHash(config)
        val name = "${section.name.lowercase()}_${page}_${limit}_$key.json"
        return File(cacheDir, name)
    }

    private fun fileForKey(cacheKey: String, config: AuthConfig, page: Int, limit: Int): File {
        val key = accountHash(config)
        val keyHash = hashKey(cacheKey)
        val name = "${keyHash}_${page}_${limit}_$key.json"
        return File(cacheDir, name)
    }

    private fun categoriesFile(type: ContentType, config: AuthConfig): File {
        val key = accountHash(config)
        val name = "categories_${type.name.lowercase()}_$key.json"
        return File(cacheDir, name)
    }

    private fun sectionIndexFile(section: Section, config: AuthConfig): File {
        val key = accountHash(config)
        val name = "index_${section.name.lowercase()}_$key.json"
        return File(cacheDir, name)
    }

    private fun sectionIndexStagingFile(section: Section, config: AuthConfig): File {
        val key = accountHash(config)
        val name = "index_staging_${section.name.lowercase()}_$key.json"
        return File(cacheDir, name)
    }

    private fun categoryThumbnailFile(
        type: ContentType,
        categoryId: String,
        config: AuthConfig
    ): File {
        val key = accountHash(config)
        val safeCategory = hashKey(categoryId)
        val name = "category_thumb_${type.name.lowercase()}_${safeCategory}_$key.json"
        return File(cacheDir, name)
    }

    private fun vodInfoFile(vodId: String, config: AuthConfig): File {
        val key = accountHash(config)
        val safeVod = hashKey(vodId)
        val name = "vod_info_${safeVod}_$key.json"
        return File(cacheDir, name)
    }

    private fun seriesInfoFile(seriesId: String, config: AuthConfig): File {
        val key = accountHash(config)
        val safeSeries = hashKey(seriesId)
        val name = "series_info_${safeSeries}_$key.json"
        return File(cacheDir, name)
    }

    private fun seasonFullFile(seriesId: String, seasonLabel: String, config: AuthConfig): File {
        val key = accountHash(config)
        val safeKey = hashKey("$seriesId|$seasonLabel")
        val name = "season_full_${safeKey}_$key.json"
        return File(cacheDir, name)
    }

    private fun refreshMarkerFile(config: AuthConfig): File {
        val key = accountHash(config)
        return File(cacheDir, "refresh_$key.marker")
    }

    private fun sectionCheckpointFile(section: Section, config: AuthConfig): File {
        val key = accountHash(config)
        return File(cacheDir, "checkpoint_${section.name.lowercase()}_$key.json")
    }

    /**
     * Write a sync checkpoint for a section to track progress
     */
    suspend fun writeSectionSyncCheckpoint(
        section: Section,
        config: AuthConfig,
        lastPage: Int,
        itemsIndexed: Int,
        isComplete: Boolean
    ) {
        val file = sectionCheckpointFile(section, config)
        withContext(Dispatchers.IO) {
            runCatching {
                val json = JSONObject()
                json.put("lastPageSynced", lastPage)
                json.put("itemsIndexed", itemsIndexed)
                json.put("isComplete", isComplete)
                json.put("timestamp", System.currentTimeMillis())
                writeTextWithTrim(file, json.toString())
            }
        }
    }

    /**
     * Read a sync checkpoint for a section
     */
    suspend fun readSectionSyncCheckpoint(
        section: Section,
        config: AuthConfig
    ): SectionSyncCheckpoint? {
        val file = sectionCheckpointFile(section, config)
        return withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            runCatching {
                val json = JSONObject(file.readText())
                SectionSyncCheckpoint(
                    lastPageSynced = json.getInt("lastPageSynced"),
                    itemsIndexed = json.getInt("itemsIndexed"),
                    isComplete = json.getBoolean("isComplete"),
                    timestamp = json.getLong("timestamp")
                )
            }.getOrNull()
        }
    }

    suspend fun clearSectionSyncCheckpoint(
        section: Section,
        config: AuthConfig
    ) {
        val file = sectionCheckpointFile(section, config)
        withContext(Dispatchers.IO) { runCatching { file.delete() } }
    }

    /**
     * Write a partial section index with checkpoint metadata
     * Uses transactional writes (temp files + atomic rename) to ensure consistency
     */
    suspend fun writeSectionIndexPartial(
        section: Section,
        config: AuthConfig,
        items: List<ContentItem>,
        lastPage: Int,
        isComplete: Boolean
    ) {
        val indexFile = sectionIndexFile(section, config)
        val checkpointFile = sectionCheckpointFile(section, config)

        val tempIndexFile = File(indexFile.parent, "temp_${indexFile.name}")
        val tempCheckpointFile = File(checkpointFile.parent, "temp_${checkpointFile.name}")

        withContext(Dispatchers.IO) {
            try {
                // Write index to temp file
                val indexPayload = JSONObject()
                indexPayload.put("items", serializeItems(items))
                tempIndexFile.writeText(indexPayload.toString())

                // Write checkpoint to temp file
                val checkpointPayload = JSONObject()
                checkpointPayload.put("lastPageSynced", lastPage)
                checkpointPayload.put("itemsIndexed", items.size)
                checkpointPayload.put("isComplete", isComplete)
                checkpointPayload.put("timestamp", System.currentTimeMillis())
                tempCheckpointFile.writeText(checkpointPayload.toString())

                // Atomic renames (POSIX guarantees atomicity)
                if (!tempIndexFile.renameTo(indexFile)) {
                    throw IOException("Failed to rename index temp file")
                }
                if (!tempCheckpointFile.renameTo(checkpointFile)) {
                    throw IOException("Failed to rename checkpoint temp file")
                }
                trimCacheIfNeeded()
            } catch (e: Exception) {
                // Clean up temp files on error
                runCatching { tempIndexFile.delete() }
                runCatching { tempCheckpointFile.delete() }
                throw e
            }
        }
    }

    /**
     * Update section index incrementally (used during background sync)
     */
    suspend fun updateSectionIndexIncremental(
        section: Section,
        config: AuthConfig,
        items: List<ContentItem>
    ) {
        writeSectionIndex(section, config, items)
    }

    /**
     * Update section index incrementally WITH checkpoint (transactional)
     * Used during background sync to ensure index and checkpoint stay in sync
     */
    suspend fun updateSectionIndexIncrementalWithCheckpoint(
        section: Section,
        config: AuthConfig,
        items: List<ContentItem>,
        lastPage: Int,
        isComplete: Boolean
    ) {
        val indexFile = sectionIndexFile(section, config)
        val checkpointFile = sectionCheckpointFile(section, config)

        val tempIndexFile = File(indexFile.parent, "temp_${indexFile.name}")
        val tempCheckpointFile = File(checkpointFile.parent, "temp_${checkpointFile.name}")

        withContext(Dispatchers.IO) {
            try {
                // Write index to temp file
                val indexPayload = JSONObject()
                indexPayload.put("items", serializeItems(items))
                tempIndexFile.writeText(indexPayload.toString())

                // Write checkpoint to temp file
                val checkpointPayload = JSONObject()
                checkpointPayload.put("lastPageSynced", lastPage)
                checkpointPayload.put("itemsIndexed", items.size)
                checkpointPayload.put("isComplete", isComplete)
                checkpointPayload.put("timestamp", System.currentTimeMillis())
                tempCheckpointFile.writeText(checkpointPayload.toString())

                // Atomic renames
                if (!tempIndexFile.renameTo(indexFile)) {
                    throw IOException("Failed to rename index temp file")
                }
                if (!tempCheckpointFile.renameTo(checkpointFile)) {
                    throw IOException("Failed to rename checkpoint temp file")
                }
                trimCacheIfNeeded()
            } catch (e: Exception) {
                runCatching { tempIndexFile.delete() }
                runCatching { tempCheckpointFile.delete() }
                throw e
            }
        }
    }

    /**
     * Update staging index incrementally WITH checkpoint (transactional)
     * Used during staging sync to ensure staging index and checkpoint stay in sync
     */
    suspend fun updateSectionIndexIncrementalStagingWithCheckpoint(
        section: Section,
        config: AuthConfig,
        items: List<ContentItem>,
        lastPage: Int,
        isComplete: Boolean
    ) {
        val indexFile = sectionIndexStagingFile(section, config)
        val checkpointFile = sectionCheckpointFile(section, config)

        val tempIndexFile = File(indexFile.parent, "temp_${indexFile.name}")
        val tempCheckpointFile = File(checkpointFile.parent, "temp_${checkpointFile.name}")

        withContext(Dispatchers.IO) {
            try {
                val indexPayload = JSONObject()
                indexPayload.put("items", serializeItems(items))
                tempIndexFile.writeText(indexPayload.toString())

                val checkpointPayload = JSONObject()
                checkpointPayload.put("lastPageSynced", lastPage)
                checkpointPayload.put("itemsIndexed", items.size)
                checkpointPayload.put("isComplete", isComplete)
                checkpointPayload.put("timestamp", System.currentTimeMillis())
                tempCheckpointFile.writeText(checkpointPayload.toString())

                if (!tempIndexFile.renameTo(indexFile)) {
                    throw IOException("Failed to rename staging index temp file")
                }
                if (!tempCheckpointFile.renameTo(checkpointFile)) {
                    throw IOException("Failed to rename checkpoint temp file")
                }
                trimCacheIfNeeded()
            } catch (e: Exception) {
                runCatching { tempIndexFile.delete() }
                runCatching { tempCheckpointFile.delete() }
                throw e
            }
        }
    }

    suspend fun updateSectionIndexIncrementalStaging(
        section: Section,
        config: AuthConfig,
        items: List<ContentItem>
    ) {
        writeSectionIndexStaging(section, config, items)
    }

    suspend fun commitSectionIndexStaging(
        section: Section,
        config: AuthConfig
    ) {
        val staging = sectionIndexStagingFile(section, config)
        val target = sectionIndexFile(section, config)
        withContext(Dispatchers.IO) {
            if (!staging.exists()) return@withContext
            runCatching {
                try {
                    Os.rename(staging.absolutePath, target.absolutePath)
                } catch (error: ErrnoException) {
                    throw IOException(
                        "Failed to commit staging index for section=$section",
                        error
                    )
                }
            }
        }
    }

    suspend fun clearSectionIndexStaging(
        section: Section,
        config: AuthConfig
    ) {
        val staging = sectionIndexStagingFile(section, config)
        withContext(Dispatchers.IO) { runCatching { staging.delete() } }
    }

    private fun serializePage(data: ContentPage): String {
        val payload = JSONObject()
        payload.put("endReached", data.endReached)
        payload.put("items", serializeItems(data.items))
        return payload.toString()
    }

    private fun serializeItems(items: List<ContentItem>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("subtitle", item.subtitle)
            obj.put("imageUrl", item.imageUrl)
            obj.put("section", item.section.name)
            obj.put("contentType", item.contentType.name)
            obj.put("streamId", item.streamId)
            obj.put("containerExtension", item.containerExtension)
            obj.put("description", item.description)
            obj.put("duration", item.duration)
            obj.put("rating", item.rating)
            obj.put("seasonLabel", item.seasonLabel)
            obj.put("episodeNumber", item.episodeNumber)
            obj.put("categoryId", item.categoryId)
            array.put(obj)
        }
        return array
    }

    private fun parsePage(text: String, limit: Int): ContentPage? {
        return runCatching {
            val obj = JSONObject(text)
            val endReached = obj.optBoolean("endReached", false)
            val array = obj.optJSONArray("items") ?: JSONArray()
            val items = parseItemsFromArray(array)
            ContentPage(items = items, endReached = endReached || items.size < limit)
        }.getOrNull()
    }

    private fun parseIndexItems(text: String): List<ContentItem>? {
        return runCatching {
            val trimmed = text.trim()
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONObject(trimmed).optJSONArray("items") ?: JSONArray()
            }
            parseItemsFromArray(array)
        }.getOrNull()
    }

    private fun parseIndexItems(file: File): List<ContentItem>? {
        return runCatching {
            if (file.length() == 0L) return@runCatching null
            file.inputStream().buffered().reader().use { streamReader ->
                JsonReader(streamReader).use { reader ->
                    when (reader.peek()) {
                        JsonToken.BEGIN_ARRAY -> parseItemsFromReaderArray(reader)
                        JsonToken.BEGIN_OBJECT -> parseItemsFromReaderObject(reader)
                        else -> {
                            reader.skipValue()
                            emptyList()
                        }
                    }
                }
            }
        }.getOrNull()
    }

    private fun parseItemsFromReaderObject(reader: JsonReader): List<ContentItem> {
        reader.beginObject()
        var parsedItems = emptyList<ContentItem>()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "items" -> {
                    parsedItems = if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                        parseItemsFromReaderArray(reader)
                    } else {
                        reader.skipValue()
                        emptyList()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return parsedItems
    }

    private fun parseItemsFromReaderArray(reader: JsonReader): List<ContentItem> {
        val items = mutableListOf<ContentItem>()
        reader.beginArray()
        while (reader.hasNext()) {
            parseContentItemFromReader(reader)?.let { items.add(it) }
        }
        reader.endArray()
        return items
    }

    private fun parseContentItemFromReader(reader: JsonReader): ContentItem? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }

        reader.beginObject()
        var id: String? = null
        var title: String? = null
        var subtitle: String? = null
        var imageUrl: String? = null
        var sectionName: String? = null
        var typeName: String? = null
        var streamId: String? = null
        var containerExtension: String? = null
        var description: String? = null
        var duration: String? = null
        var rating: String? = null
        var seasonLabel: String? = null
        var episodeNumber: String? = null
        var categoryId: String? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = readJsonString(reader)
                "title" -> title = readJsonString(reader)
                "subtitle" -> subtitle = readJsonString(reader)
                "imageUrl" -> imageUrl = readJsonString(reader)
                "section" -> sectionName = readJsonString(reader)
                "contentType" -> typeName = readJsonString(reader)
                "streamId" -> streamId = readJsonString(reader)
                "containerExtension" -> containerExtension = readJsonString(reader)
                "description" -> description = readJsonString(reader)
                "duration" -> duration = readJsonString(reader)
                "rating" -> rating = readJsonString(reader)
                "seasonLabel" -> seasonLabel = readJsonString(reader)
                "episodeNumber" -> episodeNumber = readJsonString(reader)
                "categoryId" -> categoryId = readJsonString(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val resolvedSection = runCatching { Section.valueOf(sectionName.orEmpty()) }
            .getOrNull() ?: Section.ALL
        val resolvedType = runCatching { ContentType.valueOf(typeName.orEmpty()) }
            .getOrNull() ?: ContentType.MOVIES
        val safeId = id.orEmpty()

        return ContentItem(
            id = safeId,
            title = title.orEmpty(),
            subtitle = subtitle.orEmpty(),
            imageUrl = imageUrl?.ifBlank { null },
            section = resolvedSection,
            contentType = resolvedType,
            streamId = streamId?.ifBlank { safeId } ?: safeId,
            containerExtension = containerExtension?.ifBlank { null },
            description = description?.ifBlank { null },
            duration = duration?.ifBlank { null },
            rating = rating?.ifBlank { null },
            seasonLabel = seasonLabel?.ifBlank { null },
            episodeNumber = episodeNumber?.ifBlank { null },
            categoryId = categoryId?.ifBlank { null }
        )
    }

    private fun readJsonString(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonToken.STRING -> reader.nextString()
            JsonToken.NUMBER -> reader.nextString()
            JsonToken.BOOLEAN -> reader.nextBoolean().toString()
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    private fun parseItemsFromArray(array: JSONArray): List<ContentItem> {
        val items = ArrayList<ContentItem>(array.length())
        for (index in 0 until array.length()) {
            val itemObj = array.optJSONObject(index) ?: continue
            val sectionName = itemObj.optString("section")
            val section = runCatching { Section.valueOf(sectionName) }
                .getOrNull() ?: Section.ALL
            val typeName = itemObj.optString("contentType")
            val contentType = runCatching { ContentType.valueOf(typeName) }
                .getOrNull() ?: ContentType.MOVIES
            items.add(
                ContentItem(
                    id = itemObj.optString("id"),
                    title = itemObj.optString("title"),
                    subtitle = itemObj.optString("subtitle"),
                    imageUrl = itemObj.optString("imageUrl").ifBlank { null },
                    section = section,
                    contentType = contentType,
                    streamId = itemObj.optString("streamId").ifBlank { itemObj.optString("id") },
                    containerExtension = itemObj.optString("containerExtension").ifBlank { null },
                    description = itemObj.optString("description").ifBlank { null },
                    duration = itemObj.optString("duration").ifBlank { null },
                    rating = itemObj.optString("rating").ifBlank { null },
                    seasonLabel = itemObj.optString("seasonLabel").ifBlank { null },
                    episodeNumber = itemObj.optString("episodeNumber").ifBlank { null },
                    categoryId = itemObj.optString("categoryId").ifBlank { null }
                )
            )
        }
        return items
    }

    private fun accountHash(config: AuthConfig): String {
        return hashKey("${config.baseUrl}|${config.username}|${config.listName}")
    }

    private fun hashKey(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Checkpoint data for section sync progress
 */
data class SectionSyncCheckpoint(
    /** Last page number successfully synced */
    val lastPageSynced: Int,

    /** Total items indexed so far */
    val itemsIndexed: Int,

    /** True if section sync is complete */
    val isComplete: Boolean,

    /** Timestamp when checkpoint was written */
    val timestamp: Long
)

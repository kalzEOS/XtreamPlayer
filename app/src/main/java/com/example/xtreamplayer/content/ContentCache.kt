package com.example.xtreamplayer.content

import android.content.Context
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class ContentCache(context: Context) {
    private val cacheDir = File(context.filesDir, "content_cache").apply { mkdirs() }

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
            runCatching { file.writeText(serializePage(data)) }
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
            runCatching { file.writeText(serializePage(data)) }
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
            runCatching { file.writeText(array.toString()) }
        }
    }

    suspend fun readSectionIndex(
        section: Section,
        config: AuthConfig
    ): List<ContentItem>? {
        val file = sectionIndexFile(section, config)
        return withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            val text = runCatching { file.readText() }.getOrNull() ?: return@withContext null
            if (text.isBlank()) return@withContext null
            parseIndexItems(text)
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
            runCatching { file.writeText(payload.toString()) }
        }
    }

    suspend fun hasSectionIndex(section: Section, config: AuthConfig): Boolean {
        val file = sectionIndexFile(section, config)
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
            runCatching { marker.writeText("refreshed") }
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
            runCatching { file.writeText(imageUrl.orEmpty()) }
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
                file.writeText(json.toString())
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

    /**
     * Write a partial section index with checkpoint metadata
     */
    suspend fun writeSectionIndexPartial(
        section: Section,
        config: AuthConfig,
        items: List<ContentItem>,
        lastPage: Int,
        isComplete: Boolean
    ) {
        writeSectionIndex(section, config, items)
        writeSectionSyncCheckpoint(section, config, lastPage, items.size, isComplete)
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
                    containerExtension = itemObj.optString("containerExtension").ifBlank { null }
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

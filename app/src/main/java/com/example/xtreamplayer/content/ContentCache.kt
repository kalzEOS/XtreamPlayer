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

    suspend fun writePage(
        section: Section,
        config: AuthConfig,
        page: Int,
        limit: Int,
        data: ContentPage
    ) {
        withContext(Dispatchers.IO) {
            val file = fileFor(section, config, page, limit)
            val payload = JSONObject()
            payload.put("endReached", data.endReached)
            val array = JSONArray()
            data.items.forEach { item ->
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
            payload.put("items", array)
            runCatching { file.writeText(payload.toString()) }
        }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            runCatching {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    private fun fileFor(section: Section, config: AuthConfig, page: Int, limit: Int): File {
        val key = hashKey("${config.baseUrl}|${config.username}|${config.listName}")
        val name = "${section.name.lowercase()}_${page}_${limit}_$key.json"
        return File(cacheDir, name)
    }

    private fun parsePage(text: String, limit: Int): ContentPage? {
        return runCatching {
            val obj = JSONObject(text)
            val endReached = obj.optBoolean("endReached", false)
            val array = obj.optJSONArray("items") ?: JSONArray()
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
            ContentPage(items = items, endReached = endReached || items.size < limit)
        }.getOrNull()
    }

    private fun hashKey(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

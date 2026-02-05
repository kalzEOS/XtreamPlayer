package com.example.xtreamplayer.content

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyDataStore by preferencesDataStore(name = "history")

class HistoryRepository(private val context: Context) {
    val historyEntries: Flow<List<HistoryEntry>> =
        context.historyDataStore.data
            .map { prefs ->
                val raw = prefs[Keys.HISTORY_ENTRIES] ?: "[]"
                parseEntries(raw)
            }
            .flowOn(Dispatchers.Default)

    suspend fun addToHistory(config: AuthConfig, item: ContentItem) {
        val key = contentKey(config, item)
        context.historyDataStore.edit { prefs ->
            val raw = prefs[Keys.HISTORY_ENTRIES] ?: "[]"
            val entries = parseEntries(raw).toMutableList()
            entries.removeAll { it.key == key }
            entries.add(0, HistoryEntry(key = key, item = item))
            val trimmed = entries.take(MAX_HISTORY)
            prefs[Keys.HISTORY_ENTRIES] = encodeEntries(trimmed)
        }
    }

    fun isEntryForConfig(entry: HistoryEntry, config: AuthConfig): Boolean {
        return entry.key.startsWith("${accountKey(config)}|")
    }

    private fun contentKey(config: AuthConfig, item: ContentItem): String {
        return "${accountKey(config)}|${item.contentType.name}|${item.id}"
    }

    private fun accountKey(config: AuthConfig): String {
        return "${config.baseUrl}|${config.username}"
    }

    private fun encodeEntries(entries: List<HistoryEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val item = entry.item
            val obj = JSONObject()
            obj.put("key", entry.key)
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
        return array.toString()
    }

    private fun parseEntries(raw: String): List<HistoryEntry> {
        return runCatching {
            val array = JSONArray(raw)
            val entries = ArrayList<HistoryEntry>(array.length())
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val key = obj.optString("key")
                if (key.isBlank()) continue
                val sectionName = obj.optString("section")
                val section = runCatching { Section.valueOf(sectionName) }
                    .getOrNull() ?: Section.ALL
                val typeName = obj.optString("contentType")
                val contentType = runCatching { ContentType.valueOf(typeName) }
                    .getOrNull() ?: ContentType.MOVIES
                val imageUrl = obj.optString("imageUrl")
                    .takeUnless { it.isBlank() || it == "null" }
                val containerExtension = obj.optString("containerExtension")
                    .takeUnless { it.isBlank() || it == "null" }
                val streamId = obj.optString("streamId")
                    .takeUnless { it.isBlank() || it == "null" }
                    ?: obj.optString("id")
                entries.add(
                    HistoryEntry(
                        key = key,
                        item = ContentItem(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            subtitle = obj.optString("subtitle"),
                            imageUrl = imageUrl,
                            section = section,
                            contentType = contentType,
                            streamId = streamId,
                            containerExtension = containerExtension
                        )
                    )
                )
            }
            entries
        }.getOrElse { emptyList() }
    }

    private object Keys {
        val HISTORY_ENTRIES = stringPreferencesKey("history_entries")
    }

    private companion object {
        const val MAX_HISTORY = 500
    }
}

data class HistoryEntry(
    val key: String,
    val item: ContentItem
)

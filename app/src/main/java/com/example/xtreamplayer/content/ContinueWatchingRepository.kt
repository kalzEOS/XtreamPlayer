package com.example.xtreamplayer.content

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.continueWatchingDataStore by preferencesDataStore(name = "continue_watching")

class ContinueWatchingRepository(private val context: Context) {
    val continueWatchingEntries: Flow<List<ContinueWatchingEntry>> =
        context.continueWatchingDataStore.data.map { prefs ->
            val raw = prefs[Keys.CONTINUE_WATCHING_ENTRIES] ?: "[]"
            parseEntries(raw)
        }

    suspend fun updateProgress(
        config: AuthConfig,
        item: ContentItem,
        positionMs: Long,
        durationMs: Long
    ) {
        val key = contentKey(config, item)
        val timestampMs = System.currentTimeMillis()

        context.continueWatchingDataStore.edit { prefs ->
            val raw = prefs[Keys.CONTINUE_WATCHING_ENTRIES] ?: "[]"
            val entries = parseAllEntries(raw).toMutableList()

            // Remove existing entry with same key
            entries.removeAll { it.key == key }

            // Add new entry at the front
            entries.add(
                0,
                ContinueWatchingEntry(
                    key = key,
                    item = item,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    timestampMs = timestampMs
                )
            )

            // Keep max entries
            val trimmed = entries.take(MAX_ENTRIES)
            prefs[Keys.CONTINUE_WATCHING_ENTRIES] = encodeEntries(trimmed)
        }
    }

    suspend fun removeEntry(config: AuthConfig, item: ContentItem) {
        val key = contentKey(config, item)
        context.continueWatchingDataStore.edit { prefs ->
            val raw = prefs[Keys.CONTINUE_WATCHING_ENTRIES] ?: "[]"
            val entries = parseAllEntries(raw).toMutableList()
            entries.removeAll { it.key == key }
            prefs[Keys.CONTINUE_WATCHING_ENTRIES] = encodeEntries(entries)
        }
    }

    fun isEntryForConfig(entry: ContinueWatchingEntry, config: AuthConfig): Boolean {
        return entry.key.startsWith("${accountKey(config)}|")
    }

    private fun contentKey(config: AuthConfig, item: ContentItem): String {
        return "${accountKey(config)}|${item.contentType.name}|${item.id}"
    }

    private fun accountKey(config: AuthConfig): String {
        return "${config.baseUrl}|${config.username}"
    }

    private fun encodeEntries(entries: List<ContinueWatchingEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val item = entry.item
            val obj = JSONObject()
            obj.put("key", entry.key)
            obj.put("positionMs", entry.positionMs)
            obj.put("durationMs", entry.durationMs)
            obj.put("timestampMs", entry.timestampMs)
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

    private fun parseAllEntries(raw: String): List<ContinueWatchingEntry> {
        return runCatching {
            val array = JSONArray(raw)
            val entries = ArrayList<ContinueWatchingEntry>(array.length())
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val key = obj.optString("key")
                if (key.isBlank()) continue

                val positionMs = obj.optLong("positionMs", 0)
                val durationMs = obj.optLong("durationMs", 0)
                val timestampMs = obj.optLong("timestampMs", 0)

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
                    ContinueWatchingEntry(
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
                        ),
                        positionMs = positionMs,
                        durationMs = durationMs,
                        timestampMs = timestampMs
                    )
                )
            }
            entries
        }.getOrElse { emptyList() }
    }

    private fun parseEntries(raw: String): List<ContinueWatchingEntry> {
        val allEntries = parseAllEntries(raw)

        // Filter by progress percentage (5% to 90%)
        val filtered = allEntries.filter { entry ->
            val progressPercent = if (entry.durationMs > 0) {
                (entry.positionMs * 100) / entry.durationMs
            } else {
                0
            }
            progressPercent in MIN_PROGRESS_PERCENT..MAX_PROGRESS_PERCENT
        }

        // Sort by timestamp descending (most recent first) and take top 10
        return filtered.sortedByDescending { it.timestampMs }.take(10)
    }

    private object Keys {
        val CONTINUE_WATCHING_ENTRIES = stringPreferencesKey("continue_watching_entries")
    }

    private companion object {
        const val MAX_ENTRIES = 50
        const val MIN_PROGRESS_PERCENT = 2
        const val MAX_PROGRESS_PERCENT = 95
    }
}

package com.example.xtreamplayer.content

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.resolveSubtitlePersistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import kotlin.math.abs

private val Context.continueWatchingDataStore by preferencesDataStore(name = "continue_watching")

class ContinueWatchingRepository(private val context: Context) {
    val continueWatchingEntries: Flow<List<ContinueWatchingEntry>> =
        context.continueWatchingDataStore.data
            .map { prefs ->
                val raw = prefs[Keys.CONTINUE_WATCHING_ENTRIES] ?: "[]"
                parseEntries(raw)
            }
            .flowOn(Dispatchers.Default)

    suspend fun updateProgress(
        config: AuthConfig,
        item: ContentItem,
        positionMs: Long,
        durationMs: Long,
        parentItem: ContentItem? = null,
        subtitleFileName: String? = null,
        subtitleLanguage: String? = null,
        subtitleLabel: String? = null,
        subtitleOffsetMs: Long = 0L
    ) {
        val key = contentKey(config, item)
        val keysToReplace = contentKeysForUpdate(config, item)
        val timestampMs = System.currentTimeMillis()

        context.continueWatchingDataStore.edit { prefs ->
            val raw = prefs[Keys.CONTINUE_WATCHING_ENTRIES] ?: "[]"
            val entries = parseAllEntries(raw).toMutableList()
            val existingEntry = entries.firstOrNull { it.key in keysToReplace }

            // Remove existing entry with same key
            entries.removeAll { it.key in keysToReplace }

            val resolvedSubtitlePersistence =
                resolveSubtitlePersistence(
                    existingEntry = existingEntry,
                    subtitleFileName = subtitleFileName,
                    subtitleLanguage = subtitleLanguage,
                    subtitleLabel = subtitleLabel,
                    subtitleOffsetMs = subtitleOffsetMs
                )

            val shouldSkipWrite =
                existingEntry != null &&
                    abs(existingEntry.positionMs - positionMs) < MIN_SAVE_DELTA_MS &&
                    existingEntry.durationMs == durationMs &&
                    existingEntry.parentItem?.id == parentItem?.id &&
                    existingEntry.subtitleFileName == resolvedSubtitlePersistence.subtitleFileName &&
                    existingEntry.subtitleLanguage == resolvedSubtitlePersistence.subtitleLanguage &&
                    existingEntry.subtitleLabel == resolvedSubtitlePersistence.subtitleLabel &&
                    existingEntry.subtitleOffsetMs == resolvedSubtitlePersistence.subtitleOffsetMs
            if (shouldSkipWrite) {
                return@edit
            }

            // Add new entry at the front
            entries.add(
                0,
                ContinueWatchingEntry(
                    key = key,
                    item = item,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    timestampMs = timestampMs,
                    parentItem = parentItem,
                    subtitleFileName = resolvedSubtitlePersistence.subtitleFileName,
                    subtitleLanguage = resolvedSubtitlePersistence.subtitleLanguage,
                    subtitleLabel = resolvedSubtitlePersistence.subtitleLabel,
                    subtitleOffsetMs = resolvedSubtitlePersistence.subtitleOffsetMs
                )
            )

            // Keep max entries
            val trimmed = entries.take(MAX_ENTRIES)
            prefs[Keys.CONTINUE_WATCHING_ENTRIES] = encodeEntries(trimmed)
        }
    }

    suspend fun removeEntry(config: AuthConfig, item: ContentItem) {
        val keysToRemove = contentKeysForUpdate(config, item)
        context.continueWatchingDataStore.edit { prefs ->
            val raw = prefs[Keys.CONTINUE_WATCHING_ENTRIES] ?: "[]"
            val entries = parseAllEntries(raw).toMutableList()
            entries.removeAll { it.key in keysToRemove }
            prefs[Keys.CONTINUE_WATCHING_ENTRIES] = encodeEntries(entries)
        }
    }

    suspend fun clearAll(config: AuthConfig) {
        val prefix = "${accountKey(config)}|"
        context.continueWatchingDataStore.edit { prefs ->
            val raw = prefs[Keys.CONTINUE_WATCHING_ENTRIES] ?: "[]"
            val entries = parseAllEntries(raw).filterNot { it.key.startsWith(prefix) }
            prefs[Keys.CONTINUE_WATCHING_ENTRIES] = encodeEntries(entries)
        }
    }

    fun isEntryForConfig(entry: ContinueWatchingEntry, config: AuthConfig): Boolean {
        return entry.key.startsWith("${accountKey(config)}|")
    }

    fun continueWatchingEntriesForConfig(config: AuthConfig): Flow<List<ContinueWatchingEntry>> {
        return continueWatchingEntries
            .map { entries -> entries.filter { isEntryForConfig(it, config) } }
            .distinctUntilChanged()
    }

    fun continueWatchingEntryForContent(
        config: AuthConfig,
        item: ContentItem
    ): Flow<ContinueWatchingEntry?> {
        return continueWatchingEntriesForConfig(config)
            .map { entries ->
                entries.firstOrNull { entry -> isSameContentIdentity(entry.item, item) }
            }
            .distinctUntilChanged()
    }

    suspend fun findContinueWatchingEntry(
        config: AuthConfig,
        item: ContentItem
    ): ContinueWatchingEntry? {
        return continueWatchingEntryForContent(config, item).first()
    }

    private fun contentKey(config: AuthConfig, item: ContentItem): String {
        return "${accountKey(config)}|${item.contentType.name}|${contentIdentity(item)}"
    }

    private fun contentKeysForUpdate(config: AuthConfig, item: ContentItem): Set<String> {
        val primary = contentKey(config, item)
        val legacy = legacyContentKey(config, item)
        return if (primary == legacy) {
            setOf(primary)
        } else {
            setOf(primary, legacy)
        }
    }

    private fun legacyContentKey(config: AuthConfig, item: ContentItem): String {
        return "${accountKey(config)}|${item.contentType.name}|${item.id}"
    }

    private fun contentIdentity(item: ContentItem): String {
        return item.streamId?.takeUnless { it.isBlank() } ?: item.id
    }

    private fun isSameContentIdentity(first: ContentItem, second: ContentItem): Boolean {
        if (first.contentType != second.contentType) return false
        return contentIdentity(first) == contentIdentity(second)
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
            entry.parentItem?.let { parent ->
                obj.put("parentId", parent.id)
                obj.put("parentTitle", parent.title)
                obj.put("parentSubtitle", parent.subtitle)
                obj.put("parentImageUrl", parent.imageUrl)
                obj.put("parentSection", parent.section.name)
                obj.put("parentContentType", parent.contentType.name)
                obj.put("parentStreamId", parent.streamId)
                obj.put("parentContainerExtension", parent.containerExtension)
            }
            entry.subtitleFileName?.let { obj.put("subtitleFileName", it) }
            entry.subtitleLanguage?.let { obj.put("subtitleLanguage", it) }
            entry.subtitleLabel?.let { obj.put("subtitleLabel", it) }
            if (entry.subtitleOffsetMs != 0L) {
                obj.put("subtitleOffsetMs", entry.subtitleOffsetMs)
            }
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
                val subtitleFileName = obj.optString("subtitleFileName")
                    .takeUnless { it.isBlank() || it == "null" }
                val subtitleLanguage = obj.optString("subtitleLanguage")
                    .takeUnless { it.isBlank() || it == "null" }
                val subtitleLabel = obj.optString("subtitleLabel")
                    .takeUnless { it.isBlank() || it == "null" }
                val subtitleOffsetMs = obj.optLong("subtitleOffsetMs", 0L)

                val parentItem =
                    obj.optString("parentId").takeIf { it.isNotBlank() }?.let {
                        val parentSectionName = obj.optString("parentSection")
                        val parentSection =
                            runCatching { Section.valueOf(parentSectionName) }
                                .getOrNull() ?: Section.SERIES
                        val parentTypeName = obj.optString("parentContentType")
                        val parentContentType =
                            runCatching { ContentType.valueOf(parentTypeName) }
                                .getOrNull() ?: ContentType.SERIES
                        val parentImageUrl = obj.optString("parentImageUrl")
                            .takeUnless { it.isBlank() || it == "null" }
                        val parentContainerExtension = obj.optString("parentContainerExtension")
                            .takeUnless { it.isBlank() || it == "null" }
                        val parentStreamId =
                            obj.optString("parentStreamId")
                                .takeUnless { it.isBlank() || it == "null" }
                                ?: it
                        ContentItem(
                            id = it,
                            title = obj.optString("parentTitle"),
                            subtitle = obj.optString("parentSubtitle"),
                            imageUrl = parentImageUrl,
                            section = parentSection,
                            contentType = parentContentType,
                            streamId = parentStreamId,
                            containerExtension = parentContainerExtension
                        )
                    }

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
                        timestampMs = timestampMs,
                        parentItem = parentItem,
                        subtitleFileName = subtitleFileName,
                        subtitleLanguage = subtitleLanguage,
                        subtitleLabel = subtitleLabel,
                        subtitleOffsetMs = subtitleOffsetMs
                    )
                )
            }
            entries
        }.getOrElse { error ->
            Timber.w(error, "Failed to parse Continue Watching entries; falling back to empty list")
            emptyList()
        }
    }

    private fun parseEntries(raw: String): List<ContinueWatchingEntry> {
        val allEntries = parseAllEntries(raw)

        // Build filtered list first
        val result = ArrayList<ContinueWatchingEntry>(10)
        allEntries.forEach { entry ->
            val progressPercent = if (entry.durationMs > 0) {
                (entry.positionMs * 100) / entry.durationMs
            } else {
                0
            }
            val minWatchMs = if (entry.durationMs > 0) {
                minOf(MIN_WATCH_MS, entry.durationMs / 10)
            } else {
                MIN_WATCH_MS
            }
            if (entry.positionMs >= minWatchMs && progressPercent <= MAX_PROGRESS_PERCENT) {
                result.add(entry)
            }
        }

        // Partial sort for top 15
        return if (result.size <= 15) {
            result.sortedByDescending { it.timestampMs }
        } else {
            result.asSequence()
                .sortedByDescending { it.timestampMs }
                .take(15)
                .toList()
        }
    }

    private object Keys {
        val CONTINUE_WATCHING_ENTRIES = stringPreferencesKey("continue_watching_entries")
    }

    private companion object {
        const val MAX_ENTRIES = 50
        const val MIN_WATCH_MS = 30_000L
        const val MAX_PROGRESS_PERCENT = 98
        const val MIN_SAVE_DELTA_MS = 30_000L
    }
}

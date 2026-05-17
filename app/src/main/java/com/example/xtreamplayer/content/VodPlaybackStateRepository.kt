package com.example.xtreamplayer.content

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.xtreamplayer.auth.AuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

private val Context.vodPlaybackStateDataStore by preferencesDataStore(name = "vod_playback_state")

class VodPlaybackStateRepository(private val context: Context) {
    val entries: Flow<List<VodPlaybackStateEntry>> =
        context.vodPlaybackStateDataStore.data
            .map { prefs ->
                val raw = prefs[Keys.VOD_PLAYBACK_STATE_ENTRIES] ?: "[]"
                parseEntries(raw)
            }
            .flowOn(Dispatchers.Default)

    fun entriesForConfig(config: AuthConfig): Flow<List<VodPlaybackStateEntry>> {
        return entries
            .map { entries -> entries.filter { isEntryForConfig(it, config) } }
            .distinctUntilChanged()
    }

    fun entryForMediaId(config: AuthConfig, mediaId: String): Flow<VodPlaybackStateEntry?> {
        return entriesForConfig(config)
            .map { entries -> entries.firstOrNull { it.mediaId == mediaId } }
            .distinctUntilChanged()
    }

    suspend fun updateProgress(
        config: AuthConfig,
        mediaId: String,
        title: String,
        positionMs: Long,
        durationMs: Long,
        subtitleFileName: String? = null,
        subtitleLanguage: String? = null,
        subtitleLabel: String? = null,
        subtitleOffsetMs: Long = 0L
    ) {
        val key = contentKey(config, mediaId)
        val timestampMs = System.currentTimeMillis()
        context.vodPlaybackStateDataStore.edit { prefs ->
            val raw = prefs[Keys.VOD_PLAYBACK_STATE_ENTRIES] ?: "[]"
            val entries = parseEntries(raw).toMutableList()
            val existing = entries.firstOrNull { it.key == key }
            if (
                shouldSkipVodPlaybackStateWrite(
                    existing = existing,
                    title = title,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    subtitleFileName = subtitleFileName,
                    subtitleLanguage = subtitleLanguage,
                    subtitleLabel = subtitleLabel,
                    subtitleOffsetMs = subtitleOffsetMs,
                    minSaveDeltaMs = MIN_SAVE_DELTA_MS
                )
            ) {
                return@edit
            }
            entries.removeAll { it.key == key }
            entries.add(
                0,
                VodPlaybackStateEntry(
                    key = key,
                    mediaId = mediaId,
                    title = title,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    timestampMs = timestampMs,
                    subtitleFileName = subtitleFileName,
                    subtitleLanguage = subtitleLanguage,
                    subtitleLabel = subtitleLabel,
                    subtitleOffsetMs = subtitleOffsetMs
                )
            )
            prefs[Keys.VOD_PLAYBACK_STATE_ENTRIES] = encodeEntries(entries.take(MAX_ENTRIES))
        }
    }

    suspend fun removeEntry(config: AuthConfig, mediaId: String) {
        val key = contentKey(config, mediaId)
        context.vodPlaybackStateDataStore.edit { prefs ->
            val raw = prefs[Keys.VOD_PLAYBACK_STATE_ENTRIES] ?: "[]"
            val entries = parseEntries(raw).toMutableList()
            entries.removeAll { it.key == key }
            prefs[Keys.VOD_PLAYBACK_STATE_ENTRIES] = encodeEntries(entries)
        }
    }

    private fun isEntryForConfig(entry: VodPlaybackStateEntry, config: AuthConfig): Boolean {
        return entry.key.startsWith("${accountKey(config)}|")
    }

    private fun contentKey(config: AuthConfig, mediaId: String): String {
        return "${accountKey(config)}|$mediaId"
    }

    private fun accountKey(config: AuthConfig): String {
        return "${config.baseUrl}|${config.username}"
    }

    private fun encodeEntries(entries: List<VodPlaybackStateEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("key", entry.key)
            obj.put("mediaId", entry.mediaId)
            obj.put("title", entry.title)
            obj.put("positionMs", entry.positionMs)
            obj.put("durationMs", entry.durationMs)
            obj.put("timestampMs", entry.timestampMs)
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

    private fun parseEntries(raw: String): List<VodPlaybackStateEntry> {
        return runCatching {
            val array = JSONArray(raw)
            val entries = ArrayList<VodPlaybackStateEntry>(array.length())
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val key = obj.optString("key")
                val mediaId = obj.optString("mediaId")
                if (key.isBlank() || mediaId.isBlank()) continue
                entries.add(
                    VodPlaybackStateEntry(
                        key = key,
                        mediaId = mediaId,
                        title = obj.optString("title"),
                        positionMs = obj.optLong("positionMs", 0L),
                        durationMs = obj.optLong("durationMs", 0L),
                        timestampMs = obj.optLong("timestampMs", 0L),
                        subtitleFileName = obj.optString("subtitleFileName")
                            .takeUnless { it.isBlank() || it == "null" },
                        subtitleLanguage = obj.optString("subtitleLanguage")
                            .takeUnless { it.isBlank() || it == "null" },
                        subtitleLabel = obj.optString("subtitleLabel")
                            .takeUnless { it.isBlank() || it == "null" },
                        subtitleOffsetMs = obj.optLong("subtitleOffsetMs", 0L)
                    )
                )
            }
            entries
        }.getOrElse { emptyList() }
    }

    private object Keys {
        val VOD_PLAYBACK_STATE_ENTRIES = stringPreferencesKey("vod_playback_state_entries")
    }

    private companion object {
        const val MAX_ENTRIES = 1000
        const val MIN_SAVE_DELTA_MS = 8_000L
    }
}

internal fun shouldSkipVodPlaybackStateWrite(
    existing: VodPlaybackStateEntry?,
    title: String,
    positionMs: Long,
    durationMs: Long,
    subtitleFileName: String?,
    subtitleLanguage: String?,
    subtitleLabel: String?,
    subtitleOffsetMs: Long,
    minSaveDeltaMs: Long
): Boolean {
    if (existing == null) {
        return false
    }
    return abs(existing.positionMs - positionMs) < minSaveDeltaMs &&
        existing.title == title &&
        existing.durationMs == durationMs &&
        existing.subtitleFileName == subtitleFileName &&
        existing.subtitleLanguage == subtitleLanguage &&
        existing.subtitleLabel == subtitleLabel &&
        existing.subtitleOffsetMs == subtitleOffsetMs
}

data class VodPlaybackStateEntry(
    val key: String,
    val mediaId: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val timestampMs: Long,
    val subtitleFileName: String? = null,
    val subtitleLanguage: String? = null,
    val subtitleLabel: String? = null,
    val subtitleOffsetMs: Long = 0L
)

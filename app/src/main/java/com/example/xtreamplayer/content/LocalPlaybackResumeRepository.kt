package com.example.xtreamplayer.content

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

private val Context.localPlaybackResumeDataStore by preferencesDataStore(name = "local_playback_resume")

class LocalPlaybackResumeRepository(private val context: Context) {
    val entries: Flow<List<LocalPlaybackResumeEntry>> =
        context.localPlaybackResumeDataStore.data
            .map { prefs ->
                val raw = prefs[Keys.LOCAL_PLAYBACK_RESUME_ENTRIES] ?: "[]"
                parseEntries(raw)
            }
            .flowOn(Dispatchers.Default)

    suspend fun updateProgress(
        mediaId: String,
        title: String,
        positionMs: Long,
        durationMs: Long
    ) {
        val timestampMs = System.currentTimeMillis()
        context.localPlaybackResumeDataStore.edit { prefs ->
            val raw = prefs[Keys.LOCAL_PLAYBACK_RESUME_ENTRIES] ?: "[]"
            val entries = parseEntries(raw).toMutableList()
            val existing = entries.firstOrNull { it.mediaId == mediaId }
            if (existing != null && abs(existing.positionMs - positionMs) < MIN_SAVE_DELTA_MS) {
                return@edit
            }
            entries.removeAll { it.mediaId == mediaId }
            entries.add(
                0,
                LocalPlaybackResumeEntry(
                    mediaId = mediaId,
                    title = title,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    timestampMs = timestampMs
                )
            )
            prefs[Keys.LOCAL_PLAYBACK_RESUME_ENTRIES] = encodeEntries(entries.take(MAX_ENTRIES))
        }
    }

    suspend fun removeEntry(mediaId: String) {
        context.localPlaybackResumeDataStore.edit { prefs ->
            val raw = prefs[Keys.LOCAL_PLAYBACK_RESUME_ENTRIES] ?: "[]"
            val entries = parseEntries(raw).toMutableList()
            entries.removeAll { it.mediaId == mediaId }
            prefs[Keys.LOCAL_PLAYBACK_RESUME_ENTRIES] = encodeEntries(entries)
        }
    }

    private fun encodeEntries(entries: List<LocalPlaybackResumeEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("mediaId", entry.mediaId)
            obj.put("title", entry.title)
            obj.put("positionMs", entry.positionMs)
            obj.put("durationMs", entry.durationMs)
            obj.put("timestampMs", entry.timestampMs)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseEntries(raw: String): List<LocalPlaybackResumeEntry> {
        return runCatching {
            val array = JSONArray(raw)
            val entries = ArrayList<LocalPlaybackResumeEntry>(array.length())
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val mediaId = obj.optString("mediaId")
                if (mediaId.isBlank()) continue
                entries.add(
                    LocalPlaybackResumeEntry(
                        mediaId = mediaId,
                        title = obj.optString("title"),
                        positionMs = obj.optLong("positionMs", 0L),
                        durationMs = obj.optLong("durationMs", 0L),
                        timestampMs = obj.optLong("timestampMs", 0L)
                    )
                )
            }
            entries
        }.getOrElse { emptyList() }
    }

    private object Keys {
        val LOCAL_PLAYBACK_RESUME_ENTRIES = stringPreferencesKey("local_playback_resume_entries")
    }

    private companion object {
        const val MAX_ENTRIES = 500
        const val MIN_SAVE_DELTA_MS = 8_000L
    }
}

data class LocalPlaybackResumeEntry(
    val mediaId: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val timestampMs: Long
)

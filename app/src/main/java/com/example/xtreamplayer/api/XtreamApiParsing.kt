package com.example.xtreamplayer.api

import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import com.example.xtreamplayer.content.LiveProgramInfo
import java.text.SimpleDateFormat
import java.util.Locale

private val TIMESTAMP_PATTERNS = listOf(
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd HH:mm",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd'T'HH:mm:ss.SSS",
    "yyyy-MM-dd'T'HH:mm:ssX",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
)

private val TIMESTAMP_FORMATTERS: Map<String, ThreadLocal<SimpleDateFormat>> =
    TIMESTAMP_PATTERNS.associateWith { pattern ->
        ThreadLocal.withInitial {
            SimpleDateFormat(pattern, Locale.US).apply { isLenient = true }
        }
    }

internal fun parseLiveProgram(obj: org.json.JSONObject): LiveProgramInfo? {
    val rawTitle = firstNonBlank(
        obj.optString("title"),
        obj.optString("name"),
        obj.optString("programme"),
        obj.optString("program_title")
    ) ?: return null
    val title = decodePossiblyBase64(rawTitle).trim()
    if (title.isBlank()) return null
    val startTimeMs = firstTimestamp(
        obj.opt("start_timestamp"),
        obj.opt("start"),
        obj.opt("start_datetime"),
        obj.opt("start_time"),
        obj.opt("from")
    )
    val endTimeMs = firstTimestamp(
        obj.opt("stop_timestamp"),
        obj.opt("end_timestamp"),
        obj.opt("end"),
        obj.opt("stop"),
        obj.opt("end_datetime"),
        obj.opt("stop_datetime"),
        obj.opt("end_time"),
        obj.opt("to")
    )
    return LiveProgramInfo(
        title = title,
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs
    )
}

internal fun firstTimestamp(vararg values: Any?): Long? {
    values.forEach { candidate ->
        parseTimestamp(candidate)?.let { return it }
    }
    return null
}

internal fun parseTimestamp(value: Any?): Long? {
    return when (value) {
        null -> null
        is Number -> normalizeEpoch(value.toLong())
        is String -> parseTimestamp(value)
        else -> null
    }
}

internal fun parseTimestamp(raw: String): Long? {
    val value = raw.trim()
    if (value.isBlank()) return null
    value.toLongOrNull()?.let { return normalizeEpoch(it) }
    for (pattern in TIMESTAMP_PATTERNS) {
        val formatter = TIMESTAMP_FORMATTERS[pattern]?.get() ?: continue
        val parsed = runCatching {
            formatter.parse(value)
        }.getOrNull()
        if (parsed != null) {
            return parsed.time
        }
    }
    return null
}

internal fun normalizeEpoch(raw: Long): Long {
    return if (raw in 1..9_999_999_999L) raw * 1000L else raw
}

internal fun decodePossiblyBase64(raw: String): String {
    val value = raw.trim()
    if (value.length < 8) return value
    if (!value.matches(Regex("^[A-Za-z0-9+/=_-]+$"))) {
        return value
    }
    val normalized = value.replace('-', '+').replace('_', '/')
    val padding = (4 - (normalized.length % 4)) % 4
    val padded = normalized + "=".repeat(padding)
    val decoded = runCatching {
        String(Base64.decode(padded, Base64.DEFAULT), Charsets.UTF_8).trim()
    }.getOrNull() ?: return value
    if (decoded.isBlank() || decoded.contains('\uFFFD')) {
        return value
    }
    val printableCount = decoded.count {
        it.isLetterOrDigit() || it.isWhitespace() || it in ".,:;!?-_/&'\"()[]"
    }
    return if (printableCount >= (decoded.length * 0.6f)) decoded else value
}

internal fun readString(reader: JsonReader): String? {
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

internal fun firstNonBlank(vararg values: String?): String? {
    for (value in values) {
        if (!value.isNullOrBlank()) {
            return value
        }
    }
    return null
}

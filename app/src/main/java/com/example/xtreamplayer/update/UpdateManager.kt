package com.example.xtreamplayer.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

// GitHub releases endpoint for update checks.
private const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/kalzEOS/XtreamPlayer/releases/latest"

data class UpdateRelease(
    val tagName: String,
    val versionName: String,
    val versionParts: List<Int>,
    val apkUrl: String
)

suspend fun fetchLatestRelease(client: OkHttpClient): UpdateRelease? {
    return withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(LATEST_RELEASE_URL)
        .header("User-Agent", "XtreamPlayer")
        .get()
        .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string()?.trim().orEmpty()
            if (body.isBlank()) return@withContext null
            val json = JSONObject(body)
            val tagName = json.optString("tag_name").trim()
            val name = json.optString("name").trim()
            val versionParts = parseVersionParts(tagName.ifBlank { name })
            if (versionParts.isEmpty()) return@withContext null
            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name").trim()
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    val url = asset.optString("browser_download_url").trim()
                    if (url.isNotBlank()) {
                        apkUrl = url
                        break
                    }
                }
            }
            val resolvedApkUrl = apkUrl ?: return@withContext null
            val versionName = versionParts.joinToString(".")
            UpdateRelease(
                tagName = tagName.ifBlank { name },
                versionName = versionName,
                versionParts = versionParts,
                apkUrl = resolvedApkUrl
            )
        }
    }
}

suspend fun downloadUpdateApk(
    context: Context,
    release: UpdateRelease,
    client: OkHttpClient
): File? {
    return withContext(Dispatchers.IO) {
        val request = Request.Builder().url(release.apkUrl).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            val target = File(context.cacheDir, "XtreamPlayerv${release.versionName}.apk")
            body.byteStream().use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            target
        }
    }
}

fun parseVersionParts(raw: String): List<Int> {
    val match = Regex("(\\d+(?:\\.\\d+)*)").find(raw) ?: return emptyList()
    return match.value.split(".").mapNotNull { it.toIntOrNull() }
}

fun compareVersions(current: List<Int>, latest: List<Int>): Int {
    val maxSize = maxOf(current.size, latest.size)
    for (index in 0 until maxSize) {
        val currentValue = current.getOrElse(index) { 0 }
        val latestValue = latest.getOrElse(index) { 0 }
        if (currentValue != latestValue) {
            return currentValue - latestValue
        }
    }
    return 0
}

package com.example.xtreamplayer.update

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
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
private const val UPDATE_APK_PREFIX = "XtreamPlayerv"
private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

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
): Uri? {
    return withContext(Dispatchers.IO) {
        val fileName = "${UPDATE_APK_PREFIX}${release.versionName}.apk"
        val request = Request.Builder().url(release.apkUrl).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                cleanupOldUpdateApksInDownloads(context, fileName)
                val values =
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, APK_MIME_TYPE)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                val targetUri =
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext null
                return@use try {
                    resolver.openOutputStream(targetUri)?.use { output ->
                        body.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    } ?: run {
                        resolver.delete(targetUri, null, null)
                        return@use null
                    }
                    val publishValues =
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                    resolver.update(targetUri, publishValues, null, null)
                    cleanupOldUpdateApksInCache(context, keepFileName = null)
                    targetUri
                } catch (_: Exception) {
                    runCatching { resolver.delete(targetUri, null, null) }
                    null
                }
            }

            val target = File(context.cacheDir, fileName)
            cleanupOldUpdateApksInCache(context, keepFileName = fileName)
            return@use try {
                body.byteStream().use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    target
                )
            } catch (_: Exception) {
                runCatching { target.delete() }
                null
            }
        }
    }
}

private fun cleanupOldUpdateApksInCache(context: Context, keepFileName: String?) {
    context.cacheDir.listFiles()?.forEach { file ->
        if (!file.isFile) return@forEach
        if (!isManagedUpdateApkName(file.name)) return@forEach
        if (keepFileName != null && file.name == keepFileName) return@forEach
        runCatching { file.delete() }
    }
}

private fun cleanupOldUpdateApksInDownloads(context: Context, keepDisplayName: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    val resolver = context.contentResolver
    val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.MediaColumns.DISPLAY_NAME)
    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("${UPDATE_APK_PREFIX}%.apk")
    resolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            if (name == keepDisplayName) continue
            val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
            runCatching { resolver.delete(uri, null, null) }
        }
    }
}

private fun isManagedUpdateApkName(name: String): Boolean {
    return name.startsWith(UPDATE_APK_PREFIX) && name.endsWith(".apk", ignoreCase = true)
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

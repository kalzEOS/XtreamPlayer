package com.example.xtreamplayer.api

import android.util.JsonReader
import android.util.JsonToken
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import com.example.xtreamplayer.content.CategoryItem
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.content.ContentPage
import com.example.xtreamplayer.content.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

class XtreamApi(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun authenticate(config: AuthConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildApiUrl(config, null, emptyMap())
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid service URL"))
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Login failed: ${response.code}")
                        )
                    }
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val userInfo = json.optJSONObject("user_info")
                    val status = userInfo?.optString("status")?.lowercase() ?: ""
                    if (status != "active") {
                        return@withContext Result.failure(
                            IllegalStateException("Account status: ${status.ifBlank { "unknown" }}")
                        )
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Authentication failed for ${config.baseUrl}")
                Result.failure(e)
            }
        }
    }

    suspend fun fetchSectionPage(
        section: Section,
        config: AuthConfig,
        page: Int,
        limit: Int
    ): Result<ContentPage> {
        if (section == Section.SETTINGS || section == Section.LOCAL_FILES || section == Section.FAVORITES) {
            return Result.success(ContentPage(items = emptyList(), endReached = true))
        }
        return withContext(Dispatchers.IO) {
            try {
                val action = actionForSection(section)
                val offset = page * limit
                val url = buildApiUrl(
                    config,
                    action,
                    mapOf(
                        "start" to offset.toString(),
                        "limit" to limit.toString()
                    )
                ) ?: return@withContext Result.failure(
                    IllegalArgumentException("Invalid service URL")
                )

                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Request failed: ${response.code}")
                        )
                    }
                    val body = response.body ?: return@withContext Result.failure(
                        IllegalStateException("Empty response")
                    )
                    body.charStream().use { stream ->
                        val reader = JsonReader(stream)
                        val pageData = parsePage(reader, section, offset, limit)
                        Result.success(pageData)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch section page: section=$section, page=$page")
                Result.failure(e)
            }
        }
    }

    suspend fun fetchSectionAll(
        section: Section,
        config: AuthConfig
    ): Result<List<ContentItem>> {
        if (section == Section.SETTINGS || section == Section.LOCAL_FILES || section == Section.FAVORITES) {
            return Result.success(emptyList())
        }
        return withContext(Dispatchers.IO) {
            try {
                val action = actionForSection(section)
                    ?: return@withContext Result.success(emptyList())
                val url = buildApiUrl(config, action, emptyMap())
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Invalid service URL")
                    )
                Timber.d("Bulk fetch starting for $section")
                val startTime = System.currentTimeMillis()
                // Use extended timeout for bulk fetches (5 minutes)
                val bulkClient = client.newBuilder()
                    .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                    .build()
                val request = Request.Builder().url(url).get().build()
                bulkClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.e("Bulk fetch failed for $section: ${response.code}")
                        return@withContext Result.failure(
                            IllegalStateException("Request failed: ${response.code}")
                        )
                    }
                    val body = response.body ?: return@withContext Result.failure(
                        IllegalStateException("Empty response")
                    )
                    body.charStream().use { stream ->
                        val reader = JsonReader(stream)
                        val items = parsePageAll(reader, section)
                        val elapsed = System.currentTimeMillis() - startTime
                        Timber.d("Bulk fetch completed for $section: ${items.size} items in ${elapsed}ms")
                        Result.success(items)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch all section items: section=$section")
                Result.failure(e)
            }
        }
    }

    suspend fun fetchSearchPage(
        section: Section,
        config: AuthConfig,
        query: String,
        page: Int,
        limit: Int
    ): Result<ContentPage> {
        if (
            section == Section.SETTINGS ||
            section == Section.CATEGORIES ||
            section == Section.ALL ||
            section == Section.LOCAL_FILES ||
            section == Section.FAVORITES
        ) {
            return Result.success(ContentPage(items = emptyList(), endReached = true))
        }
        return withContext(Dispatchers.IO) {
            try {
                val action = actionForSection(section)
                val offset = page * limit
                val url = buildApiUrl(
                    config,
                    action,
                    mapOf(
                        "start" to offset.toString(),
                        "limit" to limit.toString(),
                        "search" to query
                    )
                ) ?: return@withContext Result.failure(
                    IllegalArgumentException("Invalid service URL")
                )

                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Request failed: ${response.code}")
                        )
                    }
                    val body = response.body ?: return@withContext Result.failure(
                        IllegalStateException("Empty response")
                    )
                    body.charStream().use { stream ->
                        val reader = JsonReader(stream)
                        val pageData = parsePage(reader, section, offset, limit)
                        Result.success(pageData)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Search failed: section=$section, query=$query, page=$page")
                Result.failure(e)
            }
        }
    }

    suspend fun fetchCategories(
        type: ContentType,
        config: AuthConfig
    ): Result<List<CategoryItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val action = actionForCategory(type)
                val url = buildApiUrl(config, action, emptyMap())
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid service URL"))
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Request failed: ${response.code}")
                        )
                    }
                    val body = response.body ?: return@withContext Result.failure(
                        IllegalStateException("Empty response")
                    )
                    body.charStream().use { stream ->
                        val reader = JsonReader(stream)
                        val items = parseCategoryList(reader, type)
                        Result.success(items)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch categories: type=$type")
                Result.failure(e)
            }
        }
    }

    suspend fun fetchCategoryPage(
        type: ContentType,
        config: AuthConfig,
        categoryId: String,
        page: Int,
        limit: Int
    ): Result<ContentPage> {
        return withContext(Dispatchers.IO) {
            try {
                val action = actionForContent(type)
                val offset = page * limit
                val url = buildApiUrl(
                    config,
                    action,
                    mapOf(
                        "category_id" to categoryId,
                        "start" to offset.toString(),
                        "limit" to limit.toString()
                    )
                ) ?: return@withContext Result.failure(
                    IllegalArgumentException("Invalid service URL")
                )
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Request failed: ${response.code}")
                        )
                    }
                    val body = response.body ?: return@withContext Result.failure(
                        IllegalStateException("Empty response")
                    )
                    body.charStream().use { stream ->
                        val reader = JsonReader(stream)
                        val pageData = parseCategoryPage(reader, type, offset, limit)
                        Result.success(pageData)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "API request failed")
                Result.failure(e)
            }
        }
    }

    suspend fun fetchSeriesEpisodesPage(
        config: AuthConfig,
        seriesId: String,
        page: Int,
        limit: Int
    ): Result<ContentPage> {
        return withContext(Dispatchers.IO) {
            try {
                val offset = page * limit
                val url = buildApiUrl(
                    config,
                    "get_series_info",
                    mapOf("series_id" to seriesId)
                ) ?: return@withContext Result.failure(
                    IllegalArgumentException("Invalid service URL")
                )
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Request failed: ${response.code}")
                        )
                    }
                    val body = response.body ?: return@withContext Result.failure(
                        IllegalStateException("Empty response")
                    )
                    body.charStream().use { stream ->
                        val reader = JsonReader(stream)
                        val allItems = parseSeriesEpisodesAll(reader)
                        val slice = if (offset >= allItems.size) {
                            emptyList()
                        } else {
                            allItems.subList(offset, (offset + limit).coerceAtMost(allItems.size))
                        }
                        val endReached = offset + limit >= allItems.size
                        Result.success(ContentPage(items = slice, endReached = endReached))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "API request failed")
                Result.failure(e)
            }
        }
    }

    suspend fun fetchSeriesSeasonCount(
        config: AuthConfig,
        seriesId: String
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildApiUrl(
                    config,
                    "get_series_info",
                    mapOf("series_id" to seriesId)
                ) ?: return@withContext Result.failure(
                    IllegalArgumentException("Invalid service URL")
                )
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Request failed: ${response.code}")
                        )
                    }
                    val body = response.body ?: return@withContext Result.failure(
                        IllegalStateException("Empty response")
                    )
                    body.charStream().use { stream ->
                        val reader = JsonReader(stream)
                        val count = parseSeriesSeasonCount(reader)
                        Result.success(count)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "API request failed")
                Result.failure(e)
            }
        }
    }

    private fun parsePage(
        reader: JsonReader,
        section: Section,
        offset: Int,
        limit: Int
    ): ContentPage {
        return parseArrayPage(reader, offset, limit, mapperForSection(section))
    }

    private fun parsePageAll(
        reader: JsonReader,
        section: Section
    ): List<ContentItem> {
        return parseArrayAll(reader, mapperForSection(section))
    }

    private fun mapperForSection(section: Section): (JsonReader) -> ContentItem? {
        return when (section) {
            Section.MOVIES -> { r -> parseVodItem(r, Section.MOVIES) }
            Section.SERIES -> { r -> parseSeriesItem(r, Section.SERIES) }
            Section.LIVE -> { r -> parseLiveItem(r, Section.LIVE) }
            Section.CATEGORIES -> { r -> parseCategoryItem(r, Section.CATEGORIES) }
            Section.ALL -> { r -> parseVodItem(r, Section.MOVIES) }
            Section.CONTINUE_WATCHING -> { _ -> null }
            Section.FAVORITES -> { _ -> null }
            Section.LOCAL_FILES -> { _ -> null }
            Section.SETTINGS -> { _ -> null }
        }
    }

    private fun parseCategoryPage(
        reader: JsonReader,
        type: ContentType,
        offset: Int,
        limit: Int
    ): ContentPage {
        val mapper: (JsonReader) -> ContentItem? = when (type) {
            ContentType.LIVE -> { r -> parseLiveItem(r, Section.LIVE) }
            ContentType.MOVIES -> { r -> parseVodItem(r, Section.MOVIES) }
            ContentType.SERIES -> { r -> parseSeriesItem(r, Section.SERIES) }
        }
        return parseArrayPage(reader, offset, limit, mapper)
    }

    private fun parseCategoryList(reader: JsonReader, type: ContentType): List<CategoryItem> {
        val items = ArrayList<CategoryItem>()
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue()
            return emptyList()
        }
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue()
                continue
            }
            reader.beginObject()
            var id: String? = null
            var name: String? = null
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "category_id" -> id = readString(reader)
                    "category_name" -> name = readString(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            val safeId = id ?: continue
            val safeName = name?.ifBlank { "Category" } ?: "Category"
            items.add(CategoryItem(id = safeId, name = safeName, type = type))
        }
        reader.endArray()
        return items
    }

    private fun parseSeriesEpisodes(
        reader: JsonReader,
        offset: Int,
        limit: Int
    ): ContentPage {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return ContentPage(items = emptyList(), endReached = true)
        }
        reader.beginObject()
        val items = ArrayList<ContentItem>(limit)
        var endReached = true
        var index = 0
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name != "episodes") {
                reader.skipValue()
                continue
            }
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue()
                break
            }
            reader.beginObject()
            while (reader.hasNext()) {
                val seasonKey = reader.nextName()
                if (reader.peek() != JsonToken.BEGIN_ARRAY) {
                    reader.skipValue()
                    continue
                }
                reader.beginArray()
                while (reader.hasNext()) {
                    if (index < offset) {
                        reader.skipValue()
                        index++
                        continue
                    }
                    if (items.size >= limit) {
                        endReached = false
                        reader.skipValue()
                        while (reader.hasNext()) {
                            reader.skipValue()
                        }
                        break
                    }
                    val item = parseEpisodeItem(reader, seasonKey)
                    if (item != null) {
                        items.add(item)
                    }
                    index++
                }
                reader.endArray()
                if (!endReached && items.size >= limit) {
                    while (reader.hasNext()) {
                        reader.nextName()
                        reader.skipValue()
                    }
                    break
                }
            }
            reader.endObject()
        }
        reader.endObject()
        if (items.size == limit) {
            endReached = false
        }
        return ContentPage(items = items, endReached = endReached)
    }

    private data class EpisodeEntry(
        val item: ContentItem,
        val seasonNumber: Int,
        val episodeNumber: Int
    )

    private fun parseSeriesSeasonCount(reader: JsonReader): Int {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return 0
        }
        reader.beginObject()
        var seasonCount: Int? = null
        var episodeSeasonCount: Int? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "seasons" -> seasonCount = parseSeasonArrayCount(reader)
                "episodes" -> episodeSeasonCount = parseEpisodeSeasonCount(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return seasonCount ?: episodeSeasonCount ?: 0
    }

    private fun parseSeasonArrayCount(reader: JsonReader): Int {
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue()
            return 0
        }
        reader.beginArray()
        var count = 0
        while (reader.hasNext()) {
            reader.skipValue()
            count++
        }
        reader.endArray()
        return count
    }

    private fun parseEpisodeSeasonCount(reader: JsonReader): Int {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return 0
        }
        reader.beginObject()
        var count = 0
        while (reader.hasNext()) {
            reader.nextName()
            reader.skipValue()
            count++
        }
        reader.endObject()
        return count
    }

    private fun parseSeriesEpisodesAll(reader: JsonReader): List<ContentItem> {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return emptyList()
        }
        reader.beginObject()
        val items = ArrayList<EpisodeEntry>()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name != "episodes") {
                reader.skipValue()
                continue
            }
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue()
                break
            }
            reader.beginObject()
            while (reader.hasNext()) {
                val seasonKey = reader.nextName()
                if (reader.peek() != JsonToken.BEGIN_ARRAY) {
                    reader.skipValue()
                    continue
                }
                reader.beginArray()
                while (reader.hasNext()) {
                    val entry = parseEpisodeEntry(reader, seasonKey)
                    if (entry != null) {
                        items.add(entry)
                    }
                }
                reader.endArray()
            }
            reader.endObject()
        }
        reader.endObject()
        return items
            .sortedWith(
                compareBy<EpisodeEntry> { it.seasonNumber }
                    .thenBy { it.episodeNumber }
                    .thenBy { it.item.title }
            )
            .map { it.item }
    }

    private fun parseEpisodeItem(reader: JsonReader, seasonLabel: String): ContentItem? {
        return parseEpisodeEntry(reader, seasonLabel)?.item
    }

    private fun parseEpisodeEntry(reader: JsonReader, seasonLabel: String): EpisodeEntry? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        reader.beginObject()
        var id: String? = null
        var title: String? = null
        var container: String? = null
        var episodeNum: String? = null
        var image: String? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = readString(reader)
                "title" -> title = readString(reader)
                "container_extension" -> container = readString(reader)
                "episode_num" -> episodeNum = readString(reader)
                "info" -> image = parseEpisodeInfo(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val safeId = id ?: return null
        val safeTitle = title?.ifBlank {
            if (!episodeNum.isNullOrBlank()) "Episode $episodeNum" else "Episode"
        } ?: "Episode"
        val subtitle = buildString {
            append("S")
            append(seasonLabel)
            if (!episodeNum.isNullOrBlank()) {
                append(" · E")
                append(episodeNum)
            }
        }
        val seasonNumber = parseNumber(seasonLabel)
        val episodeNumber = parseNumber(episodeNum ?: "")
        val item = ContentItem(
            id = "ep-$safeId",
            title = safeTitle,
            subtitle = subtitle,
            imageUrl = image,
            section = Section.SERIES,
            contentType = ContentType.SERIES,
            streamId = safeId,
            containerExtension = container
        )
        return EpisodeEntry(
            item = item,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }

    private fun parseNumber(raw: String): Int {
        val digits = raw.filter { it.isDigit() }
        return digits.toIntOrNull() ?: Int.MAX_VALUE
    }

    private fun parseEpisodeInfo(reader: JsonReader): String? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        reader.beginObject()
        var image: String? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "movie_image" -> image = readString(reader)
                "cover_big" -> if (image.isNullOrBlank()) image = readString(reader) else reader.skipValue()
                "cover" -> if (image.isNullOrBlank()) image = readString(reader) else reader.skipValue()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return image
    }

    private fun parseArrayPage(
        reader: JsonReader,
        offset: Int,
        limit: Int,
        mapper: (JsonReader) -> ContentItem?
    ): ContentPage {
        val items = ArrayList<ContentItem>(limit)
        var endReached = true
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue()
            return ContentPage(items = emptyList(), endReached = true)
        }
        reader.beginArray()
        var index = 0
        while (reader.hasNext()) {
            if (index < offset) {
                reader.skipValue()
                index++
                continue
            }
            if (items.size >= limit) {
                endReached = false
                reader.skipValue()
                while (reader.hasNext()) {
                    reader.skipValue()
                }
                break
            }
            val item = mapper(reader)
            if (item != null) {
                items.add(item)
            }
            index++
        }
        reader.endArray()
        if (items.size == limit) {
            endReached = false
        }
        return ContentPage(items = items, endReached = endReached)
    }

    private fun parseArrayAll(
        reader: JsonReader,
        mapper: (JsonReader) -> ContentItem?
    ): List<ContentItem> {
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue()
            return emptyList()
        }
        reader.beginArray()

        // Pre-allocate with estimated capacity to avoid ArrayList resizing
        // Resize doubles capacity each time = memory spikes
        val items = ArrayList<ContentItem>(150000) // Estimate for large libraries
        var count = 0

        while (reader.hasNext()) {
            val item = mapper(reader)
            if (item != null) {
                items.add(item)
                count++

                // Log progress every 10k items
                if (count % 10000 == 0) {
                    Timber.d("Parsed $count items...")
                }
            }
        }
        reader.endArray()

        Timber.d("Parsing complete: $count total items")

        // Trim to actual size to free unused capacity
        items.trimToSize()
        return items
    }

    private fun parseVodItem(reader: JsonReader, section: Section): ContentItem? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        reader.beginObject()
        var id: String? = null
        var title: String? = null
        var cover: String? = null
        var icon: String? = null
        var movieImage: String? = null
        var container: String? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "stream_id" -> id = readString(reader)
                "name" -> title = readString(reader)
                "cover" -> cover = readString(reader)
                "stream_icon" -> icon = readString(reader)
                "movie_image" -> movieImage = readString(reader)
                "container_extension" -> container = readString(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val safeTitle = title?.ifBlank { "Movie" } ?: "Movie"
        val imageUrl = firstNonBlank(cover, icon, movieImage)
        val safeId = id ?: safeTitle
        return ContentItem(
            id = "vod-$safeId",
            title = safeTitle,
            subtitle = "Movie",
            imageUrl = imageUrl,
            section = section,
            contentType = ContentType.MOVIES,
            streamId = safeId,
            containerExtension = container
        )
    }

    private fun parseSeriesItem(reader: JsonReader, section: Section): ContentItem? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        reader.beginObject()
        var id: String? = null
        var title: String? = null
        var cover: String? = null
        var coverBig: String? = null
        var icon: String? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "series_id" -> id = readString(reader)
                "name" -> title = readString(reader)
                "cover" -> cover = readString(reader)
                "cover_big" -> coverBig = readString(reader)
                "stream_icon" -> icon = readString(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val safeTitle = title?.ifBlank { "Series" } ?: "Series"
        val imageUrl = firstNonBlank(cover, coverBig, icon)
        val safeId = id ?: safeTitle
        return ContentItem(
            id = "series-$safeId",
            title = safeTitle,
            subtitle = "Series",
            imageUrl = imageUrl,
            section = section,
            contentType = ContentType.SERIES,
            streamId = safeId,
            containerExtension = null
        )
    }

    private fun parseLiveItem(reader: JsonReader, section: Section): ContentItem? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        reader.beginObject()
        var id: String? = null
        var title: String? = null
        var icon: String? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "stream_id" -> id = readString(reader)
                "name" -> title = readString(reader)
                "stream_icon" -> icon = readString(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val safeTitle = title?.ifBlank { "Channel" } ?: "Channel"
        val safeId = id ?: safeTitle
        return ContentItem(
            id = "live-$safeId",
            title = safeTitle,
            subtitle = "Live",
            imageUrl = icon?.ifBlank { null },
            section = section,
            contentType = ContentType.LIVE,
            streamId = safeId,
            containerExtension = "ts"
        )
    }

    private fun parseCategoryItem(reader: JsonReader, section: Section): ContentItem? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        reader.beginObject()
        var id: String? = null
        var title: String? = null
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "category_id" -> id = readString(reader)
                "category_name" -> title = readString(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val safeTitle = title?.ifBlank { "Category" } ?: "Category"
        val safeId = id ?: safeTitle
        return ContentItem(
            id = "cat-$safeId",
            title = safeTitle,
            subtitle = "Category",
            imageUrl = null,
            section = section,
            contentType = ContentType.MOVIES,
            streamId = safeId,
            containerExtension = null
        )
    }

    private fun readString(reader: JsonReader): String? {
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

    private fun firstNonBlank(vararg values: String?): String? {
        for (value in values) {
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return null
    }

    private fun buildApiUrl(
        config: AuthConfig,
        action: String?,
        params: Map<String, String>
    ): String? {
        val normalized = normalizeBaseUrl(config.baseUrl)
        val httpUrl = normalized.toHttpUrlOrNull() ?: return null
        val builder = httpUrl.newBuilder()
            .encodedPath("/player_api.php")
            .addQueryParameter("username", config.username)
            .addQueryParameter("password", config.password)
        if (!action.isNullOrBlank()) {
            builder.addQueryParameter("action", action)
        }
        params.forEach { (key, value) ->
            builder.addQueryParameter(key, value)
        }
        return builder.build().toString()
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().removeSuffix("/")
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    private fun actionForSection(section: Section): String? {
        return when (section) {
            Section.ALL -> "get_vod_streams"
            Section.CONTINUE_WATCHING -> null
            Section.FAVORITES -> null
            Section.MOVIES -> "get_vod_streams"
            Section.SERIES -> "get_series"
            Section.LIVE -> "get_live_streams"
            Section.CATEGORIES -> "get_vod_categories"
            Section.LOCAL_FILES -> null
            Section.SETTINGS -> null
        }
    }

    private fun actionForCategory(type: ContentType): String {
        return when (type) {
            ContentType.LIVE -> "get_live_categories"
            ContentType.MOVIES -> "get_vod_categories"
            ContentType.SERIES -> "get_series_categories"
        }
    }

    private fun actionForContent(type: ContentType): String {
        return when (type) {
            ContentType.LIVE -> "get_live_streams"
            ContentType.MOVIES -> "get_vod_streams"
            ContentType.SERIES -> "get_series"
        }
    }
}

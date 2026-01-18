package com.example.xtreamplayer.content

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.xtreamplayer.Section
import com.example.xtreamplayer.auth.AuthConfig
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")

class FavoritesRepository(private val context: Context) {
    val favoriteContentKeys: Flow<Set<String>> =
        context.favoritesDataStore.data.map { prefs ->
            prefs[Keys.FAVORITE_CONTENT] ?: emptySet()
        }

    val favoriteCategoryKeys: Flow<Set<String>> =
        context.favoritesDataStore.data.map { prefs ->
            prefs[Keys.FAVORITE_CATEGORIES] ?: emptySet()
        }

    val favoriteContentEntries: Flow<List<FavoriteContentEntry>> =
        context.favoritesDataStore.data.map { prefs ->
            prefs[Keys.FAVORITE_CONTENT_META]
                ?.mapNotNull { parseContentEntry(it) }
                ?: emptyList()
        }

    val favoriteCategoryEntries: Flow<List<FavoriteCategoryEntry>> =
        context.favoritesDataStore.data.map { prefs ->
            prefs[Keys.FAVORITE_CATEGORY_META]
                ?.mapNotNull { parseCategoryEntry(it) }
                ?: emptyList()
        }

    fun isContentFavorite(keys: Set<String>, config: AuthConfig, item: ContentItem): Boolean {
        return keys.contains(contentKey(config, item))
    }

    fun isCategoryFavorite(keys: Set<String>, config: AuthConfig, category: CategoryItem): Boolean {
        return keys.contains(categoryKey(config, category))
    }

    fun filterKeysForConfig(keys: Set<String>, config: AuthConfig): Set<String> {
        val prefix = "${accountKey(config)}|"
        return keys.filter { it.startsWith(prefix) }.toSet()
    }

    fun isKeyForConfig(key: String, config: AuthConfig): Boolean {
        return key.startsWith("${accountKey(config)}|")
    }

    suspend fun toggleContentFavorite(config: AuthConfig, item: ContentItem): Boolean {
        val key = contentKey(config, item)
        var added = false
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_CONTENT]?.toMutableSet() ?: mutableSetOf()
            val meta = prefs[Keys.FAVORITE_CONTENT_META]?.toMutableSet() ?: mutableSetOf()
            added =
                if (current.remove(key)) {
                    false
                } else {
                    current.add(key)
                    true
                }
            removeMetaEntry(meta, key)
            if (added) {
                meta.add(encodeContentEntry(key, item))
            }
            prefs[Keys.FAVORITE_CONTENT] = current
            prefs[Keys.FAVORITE_CONTENT_META] = meta
        }
        return added
    }

    suspend fun toggleCategoryFavorite(config: AuthConfig, category: CategoryItem): Boolean {
        val key = categoryKey(config, category)
        var added = false
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_CATEGORIES]?.toMutableSet() ?: mutableSetOf()
            val meta = prefs[Keys.FAVORITE_CATEGORY_META]?.toMutableSet() ?: mutableSetOf()
            added =
                if (current.remove(key)) {
                    false
                } else {
                    current.add(key)
                    true
                }
            removeMetaEntry(meta, key)
            if (added) {
                meta.add(encodeCategoryEntry(key, category))
            }
            prefs[Keys.FAVORITE_CATEGORIES] = current
            prefs[Keys.FAVORITE_CATEGORY_META] = meta
        }
        return added
    }

    private fun contentKey(config: AuthConfig, item: ContentItem): String {
        return "${accountKey(config)}|${item.contentType.name}|${item.id}"
    }

    private fun categoryKey(config: AuthConfig, category: CategoryItem): String {
        return "${accountKey(config)}|${category.type.name}|${category.id}"
    }

    private fun accountKey(config: AuthConfig): String {
        return "${config.baseUrl}|${config.username}"
    }

    private fun encodeContentEntry(key: String, item: ContentItem): String {
        val obj = JSONObject()
        obj.put("key", key)
        obj.put("id", item.id)
        obj.put("title", item.title)
        obj.put("subtitle", item.subtitle)
        obj.put("imageUrl", item.imageUrl)
        obj.put("section", item.section.name)
        obj.put("contentType", item.contentType.name)
        obj.put("streamId", item.streamId)
        obj.put("containerExtension", item.containerExtension)
        return obj.toString()
    }

    private fun encodeCategoryEntry(key: String, category: CategoryItem): String {
        val obj = JSONObject()
        obj.put("key", key)
        obj.put("id", category.id)
        obj.put("name", category.name)
        obj.put("type", category.type.name)
        return obj.toString()
    }

    private fun parseContentEntry(raw: String): FavoriteContentEntry? {
        return runCatching {
            val obj = JSONObject(raw)
            val key = obj.optString("key")
            if (key.isBlank()) return@runCatching null
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
            FavoriteContentEntry(
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
        }.getOrNull()
    }

    private fun parseCategoryEntry(raw: String): FavoriteCategoryEntry? {
        return runCatching {
            val obj = JSONObject(raw)
            val key = obj.optString("key")
            if (key.isBlank()) return@runCatching null
            val typeName = obj.optString("type")
            val contentType = runCatching { ContentType.valueOf(typeName) }
                .getOrNull() ?: ContentType.MOVIES
            FavoriteCategoryEntry(
                key = key,
                category = CategoryItem(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    type = contentType
                )
            )
        }.getOrNull()
    }

    private fun removeMetaEntry(entries: MutableSet<String>, key: String) {
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val raw = iterator.next()
            val rawKey = runCatching { JSONObject(raw).optString("key") }.getOrNull()
            if (rawKey == key) {
                iterator.remove()
            }
        }
    }

    private object Keys {
        val FAVORITE_CONTENT = stringSetPreferencesKey("favorite_content")
        val FAVORITE_CATEGORIES = stringSetPreferencesKey("favorite_categories")
        val FAVORITE_CONTENT_META = stringSetPreferencesKey("favorite_content_meta")
        val FAVORITE_CATEGORY_META = stringSetPreferencesKey("favorite_category_meta")
    }
}

data class FavoriteContentEntry(
    val key: String,
    val item: ContentItem
)

data class FavoriteCategoryEntry(
    val key: String,
    val category: CategoryItem
)

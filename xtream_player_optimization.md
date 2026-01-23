# Xtream Player App Optimization - Codex Implementation Plan

## Problem Statement
Current app loads 130k+ movies, thousands of shows, and live channels on first launch, taking 10+ minutes. Need to implement lazy loading + smart caching for better UX while maintaining fast search.

## Critical Context for Implementation
**IMPORTANT**: The standard Xtream Codes API does NOT support pagination. It returns ALL content per category in a single response. All pagination in this plan refers to UI-side pagination of locally cached data, NOT API-level pagination. The API calls return full lists which must be stream-processed and stored incrementally.

---

## Dependencies to Add First

```kotlin
// build.gradle.kts (app level)
dependencies {
    // Room Database with FTS support
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Paging 3 for UI pagination
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1") // If using Compose
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Streaming JSON parsing (choose one based on existing setup)
    implementation("com.google.code.gson:gson:2.10.1") // For Gson streaming
    // OR
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // For kotlinx
    
    // OkHttp for streaming responses
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```

---

## Phase 1: Database Schema Setup

### 1.1 Create Content Entity

```kotlin
// data/local/entity/ContentEntity.kt
@Entity(
    tableName = "content",
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["content_type"]),
        Index(value = ["title_normalized"]),
        Index(value = ["last_accessed_at"]),
        Index(value = ["cached_at"])
    ]
)
data class ContentEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "stream_id")
    val streamId: Int,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "title_normalized")
    val titleNormalized: String, // Lowercase, trimmed, for faster search
    
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    
    @ColumnInfo(name = "content_type")
    val contentType: ContentType, // MOVIE, SERIES, LIVE
    
    @ColumnInfo(name = "cover_url")
    val coverUrl: String?,
    
    @ColumnInfo(name = "rating")
    val rating: Double?,
    
    @ColumnInfo(name = "year")
    val year: Int?,
    
    @ColumnInfo(name = "plot")
    val plot: String?,
    
    @ColumnInfo(name = "director")
    val director: String?,
    
    @ColumnInfo(name = "cast")
    val cast: String?,
    
    @ColumnInfo(name = "container_extension")
    val containerExtension: String?,
    
    @ColumnInfo(name = "stream_url")
    val streamUrl: String?,
    
    // Metadata for cache management
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long,
    
    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long,
    
    @ColumnInfo(name = "content_hash")
    val contentHash: String // MD5 hash of raw JSON for delta sync
)

enum class ContentType {
    MOVIE, SERIES, LIVE
}
```

### 1.2 Create FTS Virtual Table (Correct Implementation)

```kotlin
// data/local/entity/ContentFts.kt
// FTS4 table that shadows the content table for full-text search
@Entity(tableName = "content_fts")
@Fts4(contentEntity = ContentEntity::class)
data class ContentFts(
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "title_normalized")
    val titleNormalized: String,
    
    @ColumnInfo(name = "plot")
    val plot: String?,
    
    @ColumnInfo(name = "director")
    val director: String?,
    
    @ColumnInfo(name = "cast")
    val cast: String?
)
```

### 1.3 Create Category Entity

```kotlin
// data/local/entity/CategoryEntity.kt
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["content_type"]),
        Index(value = ["access_count"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "content_type")
    val contentType: ContentType,
    
    @ColumnInfo(name = "parent_id")
    val parentId: Int?,
    
    // For prioritizing popular categories in sync
    @ColumnInfo(name = "access_count")
    val accessCount: Int = 0,
    
    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long = 0,
    
    @ColumnInfo(name = "item_count")
    val itemCount: Int = 0, // Total items in this category
    
    @ColumnInfo(name = "synced_count")
    val syncedCount: Int = 0 // Items synced so far
)
```

### 1.4 Create Sync Progress Entity

```kotlin
// data/local/entity/SyncProgressEntity.kt
@Entity(tableName = "sync_progress")
data class SyncProgressEntity(
    @PrimaryKey
    val id: String, // Format: "{contentType}_{categoryId}" or "global_{contentType}"
    
    @ColumnInfo(name = "content_type")
    val contentType: ContentType,
    
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    
    @ColumnInfo(name = "total_items")
    val totalItems: Int,
    
    @ColumnInfo(name = "synced_items")
    val syncedItems: Int,
    
    @ColumnInfo(name = "last_sync_started_at")
    val lastSyncStartedAt: Long,
    
    @ColumnInfo(name = "last_sync_completed_at")
    val lastSyncCompletedAt: Long?,
    
    @ColumnInfo(name = "status")
    val status: SyncStatus,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String?
)

enum class SyncStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED, PAUSED
}
```

### 1.5 Create DAOs

```kotlin
// data/local/dao/ContentDao.kt
@Dao
interface ContentDao {
    
    // Insert/Update operations - use REPLACE for upsert behavior
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(content: List<ContentEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: ContentEntity)
    
    // Batch insert with transaction for performance
    @Transaction
    suspend fun insertBatch(content: List<ContentEntity>) {
        content.chunked(500).forEach { chunk ->
            insertAll(chunk)
        }
    }
    
    // Get content by category with Paging
    @Query("""
        SELECT * FROM content 
        WHERE category_id = :categoryId 
        ORDER BY title_normalized ASC
    """)
    fun getContentByCategory(categoryId: String): PagingSource<Int, ContentEntity>
    
    // Get content by type with Paging
    @Query("""
        SELECT * FROM content 
        WHERE content_type = :contentType 
        ORDER BY title_normalized ASC
    """)
    fun getContentByType(contentType: ContentType): PagingSource<Int, ContentEntity>
    
    // Get first N items for initial display (non-paging)
    @Query("""
        SELECT * FROM content 
        WHERE category_id = :categoryId 
        ORDER BY rating DESC, title_normalized ASC 
        LIMIT :limit
    """)
    suspend fun getTopContentByCategory(categoryId: String, limit: Int = 50): List<ContentEntity>
    
    // Get recently accessed content for home screen
    @Query("""
        SELECT * FROM content 
        WHERE last_accessed_at > 0 
        ORDER BY last_accessed_at DESC 
        LIMIT :limit
    """)
    suspend fun getRecentlyAccessed(limit: Int = 20): List<ContentEntity>
    
    // FTS Search - returns content matching search query
    @Query("""
        SELECT content.* FROM content
        JOIN content_fts ON content.rowid = content_fts.rowid
        WHERE content_fts MATCH :query
        ORDER BY 
            CASE WHEN content.title_normalized LIKE :exactPrefix THEN 0 ELSE 1 END,
            content.rating DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, exactPrefix: String, limit: Int = 50): List<ContentEntity>
    
    // Simple LIKE search fallback (for single characters or when FTS fails)
    @Query("""
        SELECT * FROM content 
        WHERE title_normalized LIKE :pattern 
        ORDER BY rating DESC, title_normalized ASC 
        LIMIT :limit
    """)
    suspend fun searchByPattern(pattern: String, limit: Int = 50): List<ContentEntity>
    
    // Update last accessed time
    @Query("UPDATE content SET last_accessed_at = :timestamp WHERE id = :contentId")
    suspend fun updateLastAccessed(contentId: String, timestamp: Long)
    
    // Get content count by category
    @Query("SELECT COUNT(*) FROM content WHERE category_id = :categoryId")
    suspend fun getCountByCategory(categoryId: String): Int
    
    // Get total content count by type
    @Query("SELECT COUNT(*) FROM content WHERE content_type = :contentType")
    suspend fun getCountByType(contentType: ContentType): Int
    
    // Check if content exists (for delta sync)
    @Query("SELECT content_hash FROM content WHERE id = :contentId")
    suspend fun getContentHash(contentId: String): String?
    
    // Delete old content not accessed in X days
    @Query("""
        DELETE FROM content 
        WHERE last_accessed_at < :threshold 
        AND cached_at < :threshold
    """)
    suspend fun deleteStaleContent(threshold: Long)
    
    // Get all content IDs for a category (for delta sync comparison)
    @Query("SELECT id FROM content WHERE category_id = :categoryId")
    suspend fun getContentIdsByCategory(categoryId: String): List<String>
    
    // Delete content by IDs (for removing items no longer in API)
    @Query("DELETE FROM content WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
    
    // Get single content item
    @Query("SELECT * FROM content WHERE id = :contentId")
    suspend fun getById(contentId: String): ContentEntity?
    
    // Flow for observing single item
    @Query("SELECT * FROM content WHERE id = :contentId")
    fun observeById(contentId: String): Flow<ContentEntity?>
}
```

```kotlin
// data/local/dao/CategoryDao.kt
@Dao
interface CategoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)
    
    @Query("SELECT * FROM categories WHERE content_type = :contentType ORDER BY name ASC")
    suspend fun getCategoriesByType(contentType: ContentType): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE content_type = :contentType ORDER BY name ASC")
    fun observeCategoriesByType(contentType: ContentType): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories ORDER BY access_count DESC, name ASC")
    suspend fun getCategoriesByPopularity(): List<CategoryEntity>
    
    @Query("""
        UPDATE categories 
        SET access_count = access_count + 1, last_accessed_at = :timestamp 
        WHERE id = :categoryId
    """)
    suspend fun incrementAccessCount(categoryId: String, timestamp: Long)
    
    @Query("UPDATE categories SET item_count = :count WHERE id = :categoryId")
    suspend fun updateItemCount(categoryId: String, count: Int)
    
    @Query("UPDATE categories SET synced_count = :count WHERE id = :categoryId")
    suspend fun updateSyncedCount(categoryId: String, count: Int)
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getById(categoryId: String): CategoryEntity?
}
```

```kotlin
// data/local/dao/SyncProgressDao.kt
@Dao
interface SyncProgressDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: SyncProgressEntity)
    
    @Query("SELECT * FROM sync_progress WHERE id = :id")
    suspend fun getById(id: String): SyncProgressEntity?
    
    @Query("SELECT * FROM sync_progress WHERE content_type = :contentType")
    suspend fun getByContentType(contentType: ContentType): List<SyncProgressEntity>
    
    @Query("SELECT * FROM sync_progress")
    fun observeAll(): Flow<List<SyncProgressEntity>>
    
    @Query("""
        UPDATE sync_progress 
        SET synced_items = :syncedItems, status = :status 
        WHERE id = :id
    """)
    suspend fun updateProgress(id: String, syncedItems: Int, status: SyncStatus)
    
    @Query("UPDATE sync_progress SET status = :status, error_message = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: SyncStatus, error: String? = null)
    
    @Query("""
        UPDATE sync_progress 
        SET last_sync_completed_at = :timestamp, status = :status 
        WHERE id = :id
    """)
    suspend fun markCompleted(id: String, timestamp: Long, status: SyncStatus = SyncStatus.COMPLETED)
    
    // Get overall sync percentage
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(synced_items) * 100.0 / NULLIF(SUM(total_items), 0) FROM sync_progress),
            0
        )
    """)
    suspend fun getOverallSyncPercentage(): Double
    
    // Check if initial sync is complete for a content type
    @Query("""
        SELECT COUNT(*) = 0 FROM sync_progress 
        WHERE content_type = :contentType AND status != 'COMPLETED'
    """)
    suspend fun isContentTypeSynced(contentType: ContentType): Boolean
}
```

### 1.6 Create Database

```kotlin
// data/local/AppDatabase.kt
@Database(
    entities = [
        ContentEntity::class,
        ContentFts::class,
        CategoryEntity::class,
        SyncProgressEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun contentDao(): ContentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun syncProgressDao(): SyncProgressDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xtream_player.db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // Better concurrent read/write
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Enable FTS rebuild triggers
                            db.execSQL("CREATE TRIGGER IF NOT EXISTS content_ai AFTER INSERT ON content BEGIN INSERT INTO content_fts(content_fts) VALUES('rebuild'); END;")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// data/local/Converters.kt
class Converters {
    @TypeConverter
    fun fromContentType(value: ContentType): String = value.name
    
    @TypeConverter
    fun toContentType(value: String): ContentType = ContentType.valueOf(value)
    
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name
    
    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
```

---

## Phase 2: Streaming JSON Parser for Large Responses

### 2.1 Create Streaming Parser

```kotlin
// data/remote/parser/StreamingContentParser.kt
/**
 * Parses large JSON arrays in a streaming fashion to avoid OOM errors.
 * Processes items in batches and writes to database incrementally.
 */
class StreamingContentParser(
    private val gson: Gson,
    private val contentDao: ContentDao,
    private val onProgress: suspend (processed: Int) -> Unit
) {
    
    companion object {
        private const val BATCH_SIZE = 500
    }
    
    /**
     * Parse streaming response and insert to database in batches.
     * Returns total number of items processed.
     */
    suspend fun parseAndStore(
        inputStream: InputStream,
        categoryId: String,
        contentType: ContentType
    ): Int = withContext(Dispatchers.IO) {
        var totalProcessed = 0
        val batch = mutableListOf<ContentEntity>()
        val now = System.currentTimeMillis()
        
        JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.beginArray()
            
            while (reader.hasNext()) {
                // Check for cancellation
                ensureActive()
                
                // Parse single item based on content type
                val entity = when (contentType) {
                    ContentType.MOVIE -> parseMovie(reader, categoryId, now)
                    ContentType.SERIES -> parseSeries(reader, categoryId, now)
                    ContentType.LIVE -> parseLiveStream(reader, categoryId, now)
                }
                
                entity?.let { batch.add(it) }
                
                // Insert batch when full
                if (batch.size >= BATCH_SIZE) {
                    contentDao.insertBatch(batch.toList())
                    totalProcessed += batch.size
                    onProgress(totalProcessed)
                    batch.clear()
                }
            }
            
            reader.endArray()
        }
        
        // Insert remaining items
        if (batch.isNotEmpty()) {
            contentDao.insertBatch(batch.toList())
            totalProcessed += batch.size
            onProgress(totalProcessed)
        }
        
        totalProcessed
    }
    
    private fun parseMovie(reader: JsonReader, categoryId: String, timestamp: Long): ContentEntity? {
        var streamId: Int? = null
        var name: String? = null
        var coverUrl: String? = null
        var rating: Double? = null
        var year: Int? = null
        var plot: String? = null
        var director: String? = null
        var cast: String? = null
        var containerExtension: String? = null
        
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "stream_id" -> streamId = reader.nextIntOrNull()
                "num" -> if (streamId == null) streamId = reader.nextIntOrNull()
                "name" -> name = reader.nextStringOrNull()
                "stream_icon", "cover" -> coverUrl = reader.nextStringOrNull()
                "rating" -> rating = reader.nextDoubleOrNull()
                "releasedate", "release_date" -> {
                    val date = reader.nextStringOrNull()
                    year = date?.take(4)?.toIntOrNull()
                }
                "plot", "description" -> plot = reader.nextStringOrNull()
                "director" -> director = reader.nextStringOrNull()
                "cast", "actors" -> cast = reader.nextStringOrNull()
                "container_extension" -> containerExtension = reader.nextStringOrNull()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        
        // Validate required fields
        if (streamId == null || name.isNullOrBlank()) return null
        
        val id = "movie_${streamId}"
        val rawData = "$streamId|$name|$coverUrl|$rating|$year|$plot|$director|$cast"
        
        return ContentEntity(
            id = id,
            streamId = streamId,
            title = name,
            titleNormalized = name.lowercase().trim(),
            categoryId = categoryId,
            contentType = ContentType.MOVIE,
            coverUrl = coverUrl,
            rating = rating,
            year = year,
            plot = plot,
            director = director,
            cast = cast,
            containerExtension = containerExtension,
            streamUrl = null,
            cachedAt = timestamp,
            lastAccessedAt = 0,
            contentHash = rawData.md5()
        )
    }
    
    private fun parseSeries(reader: JsonReader, categoryId: String, timestamp: Long): ContentEntity? {
        var seriesId: Int? = null
        var name: String? = null
        var coverUrl: String? = null
        var rating: Double? = null
        var year: Int? = null
        var plot: String? = null
        var director: String? = null
        var cast: String? = null
        
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "series_id" -> seriesId = reader.nextIntOrNull()
                "name" -> name = reader.nextStringOrNull()
                "cover" -> coverUrl = reader.nextStringOrNull()
                "rating" -> rating = reader.nextDoubleOrNull()
                "releaseDate", "release_date" -> {
                    val date = reader.nextStringOrNull()
                    year = date?.take(4)?.toIntOrNull()
                }
                "plot" -> plot = reader.nextStringOrNull()
                "director" -> director = reader.nextStringOrNull()
                "cast" -> cast = reader.nextStringOrNull()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        
        if (seriesId == null || name.isNullOrBlank()) return null
        
        val id = "series_${seriesId}"
        val rawData = "$seriesId|$name|$coverUrl|$rating|$year|$plot|$director|$cast"
        
        return ContentEntity(
            id = id,
            streamId = seriesId,
            title = name,
            titleNormalized = name.lowercase().trim(),
            categoryId = categoryId,
            contentType = ContentType.SERIES,
            coverUrl = coverUrl,
            rating = rating,
            year = year,
            plot = plot,
            director = director,
            cast = cast,
            containerExtension = null,
            streamUrl = null,
            cachedAt = timestamp,
            lastAccessedAt = 0,
            contentHash = rawData.md5()
        )
    }
    
    private fun parseLiveStream(reader: JsonReader, categoryId: String, timestamp: Long): ContentEntity? {
        var streamId: Int? = null
        var name: String? = null
        var iconUrl: String? = null
        var epgChannelId: String? = null
        
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "stream_id" -> streamId = reader.nextIntOrNull()
                "name" -> name = reader.nextStringOrNull()
                "stream_icon" -> iconUrl = reader.nextStringOrNull()
                "epg_channel_id" -> epgChannelId = reader.nextStringOrNull()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        
        if (streamId == null || name.isNullOrBlank()) return null
        
        val id = "live_${streamId}"
        val rawData = "$streamId|$name|$iconUrl|$epgChannelId"
        
        return ContentEntity(
            id = id,
            streamId = streamId,
            title = name,
            titleNormalized = name.lowercase().trim(),
            categoryId = categoryId,
            contentType = ContentType.LIVE,
            coverUrl = iconUrl,
            rating = null,
            year = null,
            plot = null,
            director = null,
            cast = null,
            containerExtension = null,
            streamUrl = null,
            cachedAt = timestamp,
            lastAccessedAt = 0,
            contentHash = rawData.md5()
        )
    }
    
    // Extension functions for safe JSON reading
    private fun JsonReader.nextStringOrNull(): String? {
        return if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextString()
        }
    }
    
    private fun JsonReader.nextIntOrNull(): Int? {
        return if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextInt()
        }
    }
    
    private fun JsonReader.nextDoubleOrNull(): Double? {
        return if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            try {
                nextDouble()
            } catch (e: Exception) {
                nextString().toDoubleOrNull()
            }
        }
    }
    
    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
    }
}
```

### 2.2 Create Streaming API Client

```kotlin
// data/remote/XtreamApiClient.kt
class XtreamApiClient(
    private val okHttpClient: OkHttpClient
) {
    
    /**
     * Fetch content with streaming response.
     * Returns InputStream for streaming parsing - caller must close.
     */
    suspend fun getContentStream(
        baseUrl: String,
        username: String,
        password: String,
        action: String,
        categoryId: String? = null
    ): InputStream = withContext(Dispatchers.IO) {
        val urlBuilder = StringBuilder(baseUrl)
            .append("/player_api.php")
            .append("?username=").append(username)
            .append("&password=").append(password)
            .append("&action=").append(action)
        
        categoryId?.let {
            urlBuilder.append("&category_id=").append(it)
        }
        
        val request = Request.Builder()
            .url(urlBuilder.toString())
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("API request failed: ${response.code}")
        }
        
        response.body?.byteStream() ?: throw IOException("Empty response body")
    }
    
    /**
     * Fetch categories (small response, can use regular parsing)
     */
    suspend fun getCategories(
        baseUrl: String,
        username: String,
        password: String,
        contentType: ContentType
    ): List<CategoryResponse> = withContext(Dispatchers.IO) {
        val action = when (contentType) {
            ContentType.MOVIE -> "get_vod_categories"
            ContentType.SERIES -> "get_series_categories"
            ContentType.LIVE -> "get_live_categories"
        }
        
        val url = "$baseUrl/player_api.php?username=$username&password=$password&action=$action"
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("API request failed: ${response.code}")
        }
        
        val body = response.body?.string() ?: throw IOException("Empty response")
        Gson().fromJson(body, Array<CategoryResponse>::class.java).toList()
    }
}

data class CategoryResponse(
    @SerializedName("category_id")
    val categoryId: String,
    @SerializedName("category_name")
    val categoryName: String,
    @SerializedName("parent_id")
    val parentId: Int?
)
```

---

## Phase 3: Sync Manager

### 3.1 Create Sync Manager

```kotlin
// data/sync/SyncManager.kt
class SyncManager(
    private val context: Context,
    private val apiClient: XtreamApiClient,
    private val database: AppDatabase,
    private val credentials: CredentialsProvider // Your existing credentials provider
) {
    
    private val contentDao = database.contentDao()
    private val categoryDao = database.categoryDao()
    private val syncProgressDao = database.syncProgressDao()
    private val gson = Gson()
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    /**
     * Initial sync - loads categories + first batch of content for immediate UI display.
     * Should complete in < 30 seconds.
     */
    suspend fun performInitialSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.Syncing(0, "Loading categories...")
            
            val creds = credentials.get() ?: return@withContext Result.failure(
                IllegalStateException("No credentials")
            )
            
            // Step 1: Load all categories (fast, small response)
            val allCategories = mutableListOf<CategoryEntity>()
            
            ContentType.values().forEach { contentType ->
                val categories = apiClient.getCategories(
                    creds.baseUrl, creds.username, creds.password, contentType
                )
                
                allCategories.addAll(categories.map { response ->
                    CategoryEntity(
                        id = "${contentType.name}_${response.categoryId}",
                        name = response.categoryName,
                        contentType = contentType,
                        parentId = response.parentId
                    )
                })
            }
            
            categoryDao.insertAll(allCategories)
            _syncState.value = SyncState.Syncing(10, "Categories loaded. Loading content preview...")
            
            // Step 2: Load first batch from top categories (parallel, limited)
            // Prioritize previously accessed categories, fallback to first N
            val prioritizedCategories = categoryDao.getCategoriesByPopularity()
                .take(10) // Load preview for top 10 categories only
            
            val jobs = prioritizedCategories.mapIndexed { index, category ->
                async {
                    try {
                        loadCategoryPreview(creds, category)
                        _syncState.value = SyncState.Syncing(
                            10 + ((index + 1) * 80 / prioritizedCategories.size),
                            "Loading ${category.name}..."
                        )
                    } catch (e: Exception) {
                        // Log but don't fail entire sync
                        Log.e("SyncManager", "Failed to load preview for ${category.name}", e)
                    }
                }
            }
            
            jobs.awaitAll()
            
            _syncState.value = SyncState.Syncing(100, "Initial sync complete")
            _syncState.value = SyncState.Idle
            
            // Schedule background full sync
            scheduleBackgroundSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }
    
    /**
     * Load preview of a single category (first ~100 items).
     * Uses streaming parser but stops early.
     */
    private suspend fun loadCategoryPreview(
        credentials: Credentials,
        category: CategoryEntity
    ) = withContext(Dispatchers.IO) {
        val action = when (category.contentType) {
            ContentType.MOVIE -> "get_vod_streams"
            ContentType.SERIES -> "get_series"
            ContentType.LIVE -> "get_live_streams"
        }
        
        val categoryId = category.id.substringAfter("_")
        
        val inputStream = apiClient.getContentStream(
            credentials.baseUrl,
            credentials.username,
            credentials.password,
            action,
            categoryId
        )
        
        // Use a limited parser that stops after N items
        val parser = LimitedStreamingParser(gson, contentDao, limit = 100)
        val count = parser.parseAndStore(inputStream, category.id, category.contentType)
        
        // Update category with item count (estimate based on preview)
        categoryDao.updateSyncedCount(category.id, count)
    }
    
    /**
     * Full sync of a single category (all items).
     * Used by background worker.
     */
    suspend fun syncCategory(
        categoryId: String,
        contentType: ContentType
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val creds = credentials.get() ?: return@withContext Result.failure(
                IllegalStateException("No credentials")
            )
            
            val action = when (contentType) {
                ContentType.MOVIE -> "get_vod_streams"
                ContentType.SERIES -> "get_series"
                ContentType.LIVE -> "get_live_streams"
            }
            
            val apiCategoryId = categoryId.substringAfter("_")
            val progressId = "sync_$categoryId"
            
            // Mark sync as in progress
            syncProgressDao.insert(SyncProgressEntity(
                id = progressId,
                contentType = contentType,
                categoryId = categoryId,
                totalItems = 0, // Will update after sync
                syncedItems = 0,
                lastSyncStartedAt = System.currentTimeMillis(),
                lastSyncCompletedAt = null,
                status = SyncStatus.IN_PROGRESS,
                errorMessage = null
            ))
            
            val inputStream = apiClient.getContentStream(
                creds.baseUrl,
                creds.username,
                creds.password,
                action,
                apiCategoryId
            )
            
            val parser = StreamingContentParser(gson, contentDao) { processed ->
                // Update progress periodically
                syncProgressDao.updateProgress(progressId, processed, SyncStatus.IN_PROGRESS)
            }
            
            val totalSynced = parser.parseAndStore(inputStream, categoryId, contentType)
            
            // Mark complete
            syncProgressDao.markCompleted(progressId, System.currentTimeMillis())
            categoryDao.updateItemCount(categoryId, totalSynced)
            categoryDao.updateSyncedCount(categoryId, totalSynced)
            
            Result.success(totalSynced)
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to sync category $categoryId", e)
            val progressId = "sync_$categoryId"
            syncProgressDao.updateStatus(progressId, SyncStatus.FAILED, e.message)
            Result.failure(e)
        }
    }
    
    /**
     * Schedule background sync using WorkManager.
     */
    fun scheduleBackgroundSync() {
        // Get all categories and schedule sync for each
        CoroutineScope(Dispatchers.IO).launch {
            val categories = categoryDao.getCategoriesByPopularity()
            
            categories.forEachIndexed { index, category ->
                val workRequest = OneTimeWorkRequestBuilder<ContentSyncWorker>()
                    .setInputData(workDataOf(
                        ContentSyncWorker.KEY_CATEGORY_ID to category.id,
                        ContentSyncWorker.KEY_CONTENT_TYPE to category.contentType.name
                    ))
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build())
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30, TimeUnit.SECONDS
                    )
                    .setInitialDelay((index * 2).toLong(), TimeUnit.SECONDS) // Stagger starts
                    .addTag("content_sync")
                    .build()
                
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "sync_${category.id}",
                        ExistingWorkPolicy.KEEP,
                        workRequest
                    )
            }
        }
    }
    
    /**
     * Cancel all background sync work.
     */
    fun cancelBackgroundSync() {
        WorkManager.getInstance(context).cancelAllWorkByTag("content_sync")
    }
    
    /**
     * Get sync progress as Flow.
     */
    fun observeSyncProgress(): Flow<List<SyncProgressEntity>> {
        return syncProgressDao.observeAll()
    }
    
    /**
     * Get overall sync percentage.
     */
    suspend fun getOverallProgress(): Double {
        return syncProgressDao.getOverallSyncPercentage()
    }
}

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val progress: Int, val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}
```

### 3.2 Create Limited Streaming Parser (for preview loading)

```kotlin
// data/remote/parser/LimitedStreamingParser.kt
/**
 * Like StreamingContentParser but stops after reaching a limit.
 * Used for initial preview loading.
 */
class LimitedStreamingParser(
    private val gson: Gson,
    private val contentDao: ContentDao,
    private val limit: Int = 100
) {
    
    suspend fun parseAndStore(
        inputStream: InputStream,
        categoryId: String,
        contentType: ContentType
    ): Int = withContext(Dispatchers.IO) {
        var totalProcessed = 0
        val batch = mutableListOf<ContentEntity>()
        val now = System.currentTimeMillis()
        
        try {
            JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.beginArray()
                
                while (reader.hasNext() && totalProcessed < limit) {
                    ensureActive()
                    
                    val entity = parseItem(reader, categoryId, contentType, now)
                    entity?.let {
                        batch.add(it)
                        totalProcessed++
                    }
                    
                    // Insert in smaller batches
                    if (batch.size >= 50) {
                        contentDao.insertBatch(batch.toList())
                        batch.clear()
                    }
                }
                
                // Don't read the rest - just close
            }
        } catch (e: Exception) {
            // Stream might close early, that's fine
            Log.d("LimitedParser", "Stream ended: ${e.message}")
        }
        
        // Insert remaining
        if (batch.isNotEmpty()) {
            contentDao.insertBatch(batch.toList())
        }
        
        totalProcessed
    }
    
    private fun parseItem(
        reader: JsonReader,
        categoryId: String,
        contentType: ContentType,
        timestamp: Long
    ): ContentEntity? {
        // Reuse parsing logic from StreamingContentParser
        // Copy the relevant parse methods here or extract to shared util
        return null // Implement same as StreamingContentParser
    }
}
```

### 3.3 Create Background Worker

```kotlin
// data/sync/ContentSyncWorker.kt
class ContentSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val KEY_CATEGORY_ID = "category_id"
        const val KEY_CONTENT_TYPE = "content_type"
    }
    
    override suspend fun doWork(): Result {
        val categoryId = inputData.getString(KEY_CATEGORY_ID) ?: return Result.failure()
        val contentTypeName = inputData.getString(KEY_CONTENT_TYPE) ?: return Result.failure()
        val contentType = ContentType.valueOf(contentTypeName)
        
        // Get SyncManager from DI or create instance
        val syncManager = getSyncManager()
        
        return try {
            if (isStopped) {
                return Result.retry()
            }
            
            val result = syncManager.syncCategory(categoryId, contentType)
            
            if (result.isSuccess) {
                Result.success()
            } else {
                // Retry on failure with backoff
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("ContentSyncWorker", "Sync failed for $categoryId", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    private fun getSyncManager(): SyncManager {
        // Implement based on your DI setup (Hilt, Koin, manual)
        // Example with manual creation:
        val database = AppDatabase.getInstance(applicationContext)
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val apiClient = XtreamApiClient(okHttpClient)
        val credentials = CredentialsProvider(applicationContext) // Your implementation
        
        return SyncManager(applicationContext, apiClient, database, credentials)
    }
}
```

---

## Phase 4: Search Implementation

### 4.1 Create Search Repository

```kotlin
// data/repository/SearchRepository.kt
class SearchRepository(
    private val contentDao: ContentDao
) {
    
    /**
     * Search content with FTS.
     * Handles query preprocessing and fallback.
     */
    suspend fun search(query: String, limit: Int = 50): List<ContentEntity> {
        val trimmedQuery = query.trim()
        
        if (trimmedQuery.isEmpty()) {
            return emptyList()
        }
        
        // For single characters or very short queries, use LIKE
        if (trimmedQuery.length < 2) {
            return contentDao.searchByPattern("${trimmedQuery.lowercase()}%", limit)
        }
        
        return try {
            // Prepare FTS query - add wildcard for prefix matching
            val ftsQuery = trimmedQuery
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }
            
            val exactPrefix = "${trimmedQuery.lowercase()}%"
            
            contentDao.search(ftsQuery, exactPrefix, limit)
        } catch (e: Exception) {
            // Fallback to LIKE search if FTS fails
            Log.w("SearchRepository", "FTS search failed, falling back to LIKE", e)
            contentDao.searchByPattern("%${trimmedQuery.lowercase()}%", limit)
        }
    }
    
    /**
     * Search with filter by content type.
     */
    suspend fun searchByType(
        query: String,
        contentType: ContentType,
        limit: Int = 50
    ): List<ContentEntity> {
        return search(query, limit * 2) // Fetch more, then filter
            .filter { it.contentType == contentType }
            .take(limit)
    }
}
```

### 4.2 Create Search ViewModel

```kotlin
// ui/search/SearchViewModel.kt
class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<ContentEntity>>(emptyList())
    val searchResults: StateFlow<List<ContentEntity>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private var searchJob: Job? = null
    
    init {
        // Debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after user stops typing
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                        return@collectLatest
                    }
                    
                    _isSearching.value = true
                    try {
                        val results = searchRepository.search(query)
                        _searchResults.value = results
                    } catch (e: Exception) {
                        Log.e("SearchViewModel", "Search failed", e)
                        _searchResults.value = emptyList()
                    } finally {
                        _isSearching.value = false
                    }
                }
        }
    }
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
}
```

---

## Phase 5: UI Pagination with Paging 3

### 5.1 Create Content Paging Source

```kotlin
// data/paging/ContentPagingSource.kt
/**
 * PagingSource that loads from local database.
 * Database is the single source of truth - API sync happens in background.
 */
class ContentPagingSource(
    private val contentDao: ContentDao,
    private val categoryId: String
) : PagingSource<Int, ContentEntity>() {
    
    // Room's PagingSource handles this automatically
    // Use contentDao.getContentByCategory(categoryId) which returns PagingSource
}

// Actually, Room generates the PagingSource for you, so just use:
// val pagingFlow = Pager(PagingConfig(pageSize = 50)) {
//     contentDao.getContentByCategory(categoryId)
// }.flow.cachedIn(viewModelScope)
```

### 5.2 Create Category ViewModel with Paging

```kotlin
// ui/category/CategoryViewModel.kt
class CategoryViewModel(
    private val contentDao: ContentDao,
    private val categoryDao: CategoryDao,
    private val syncManager: SyncManager
) : ViewModel() {
    
    private val _currentCategory = MutableStateFlow<CategoryEntity?>(null)
    val currentCategory: StateFlow<CategoryEntity?> = _currentCategory.asStateFlow()
    
    /**
     * Paged content flow - updates automatically as data is synced.
     */
    val pagedContent: Flow<PagingData<ContentEntity>> = _currentCategory
        .filterNotNull()
        .flatMapLatest { category ->
            Pager(
                config = PagingConfig(
                    pageSize = 50,
                    prefetchDistance = 20,
                    enablePlaceholders = false,
                    initialLoadSize = 100
                )
            ) {
                contentDao.getContentByCategory(category.id)
            }.flow
        }
        .cachedIn(viewModelScope)
    
    fun selectCategory(category: CategoryEntity) {
        _currentCategory.value = category
        
        // Track access for prioritization
        viewModelScope.launch {
            categoryDao.incrementAccessCount(category.id, System.currentTimeMillis())
        }
    }
    
    /**
     * Get top content for quick display before paging loads.
     */
    suspend fun getPreviewContent(categoryId: String): List<ContentEntity> {
        return contentDao.getTopContentByCategory(categoryId, 20)
    }
}
```

### 5.3 RecyclerView Adapter with Paging

```kotlin
// ui/category/ContentPagingAdapter.kt
class ContentPagingAdapter(
    private val onItemClick: (ContentEntity) -> Unit
) : PagingDataAdapter<ContentEntity, ContentPagingAdapter.ViewHolder>(DIFF_CALLBACK) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }
    
    inner class ViewHolder(
        private val binding: ItemContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                getItem(bindingAdapterPosition)?.let(onItemClick)
            }
        }
        
        fun bind(content: ContentEntity) {
            binding.title.text = content.title
            binding.year.text = content.year?.toString() ?: ""
            binding.rating.text = content.rating?.let { "%.1f".format(it) } ?: ""
            
            // Load image with Coil/Glide
            content.coverUrl?.let { url ->
                Glide.with(binding.cover)
                    .load(url)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error)
                    .into(binding.cover)
            }
        }
    }
    
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ContentEntity>() {
            override fun areItemsTheSame(old: ContentEntity, new: ContentEntity) = old.id == new.id
            override fun areContentsTheSame(old: ContentEntity, new: ContentEntity) = old == new
        }
    }
}
```

---

## Phase 6: Cache Management

### 6.1 Create Cache Manager

```kotlin
// data/cache/CacheManager.kt
class CacheManager(
    private val contentDao: ContentDao,
    private val context: Context
) {
    
    companion object {
        private const val STALE_THRESHOLD_DAYS = 7L
        private const val MAX_CACHE_SIZE_MB = 500L
    }
    
    /**
     * Clean stale content that hasn't been accessed in X days.
     */
    suspend fun cleanStaleContent() = withContext(Dispatchers.IO) {
        val thresholdMs = System.currentTimeMillis() - 
            TimeUnit.DAYS.toMillis(STALE_THRESHOLD_DAYS)
        contentDao.deleteStaleContent(thresholdMs)
    }
    
    /**
     * Check if content needs refresh.
     */
    suspend fun needsRefresh(contentId: String): Boolean {
        val content = contentDao.getById(contentId) ?: return true
        val ageMs = System.currentTimeMillis() - content.cachedAt
        return ageMs > TimeUnit.DAYS.toMillis(STALE_THRESHOLD_DAYS)
    }
    
    /**
     * Get database size in MB.
     */
    fun getDatabaseSizeMb(): Long {
        val dbFile = context.getDatabasePath("xtream_player.db")
        return if (dbFile.exists()) {
            dbFile.length() / (1024 * 1024)
        } else {
            0
        }
    }
    
    /**
     * Clear all cached content (keeps categories and sync progress).
     */
    suspend fun clearContentCache() = withContext(Dispatchers.IO) {
        // You'd need to add this method to ContentDao
        // contentDao.deleteAll()
    }
}
```

### 6.2 Schedule Periodic Cache Cleanup

```kotlin
// data/cache/CacheCleanupWorker.kt
class CacheCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)
        val cacheManager = CacheManager(database.contentDao(), applicationContext)
        
        return try {
            cacheManager.cleanStaleContent()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Schedule in Application or DI setup:
fun scheduleCacheCleanup(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
        1, TimeUnit.DAYS
    )
        .setConstraints(Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build())
        .build()
    
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(
            "cache_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
}
```

---

## Phase 7: App Startup & Initialization

### 7.1 Create App Initializer

```kotlin
// app/AppInitializer.kt
class AppInitializer(
    private val syncManager: SyncManager,
    private val cacheManager: CacheManager
) {
    
    /**
     * Call from Application.onCreate() or splash screen.
     */
    suspend fun initialize(): InitResult {
        return try {
            // Check if we have any cached data
            val hasCache = checkHasCache()
            
            if (!hasCache) {
                // First launch - perform initial sync
                val result = syncManager.performInitialSync()
                if (result.isFailure) {
                    return InitResult.Error(result.exceptionOrNull()?.message ?: "Sync failed")
                }
            } else {
                // Have cache - just schedule background refresh
                syncManager.scheduleBackgroundSync()
            }
            
            // Schedule periodic cleanup
            scheduleCacheCleanup()
            
            InitResult.Success
        } catch (e: Exception) {
            InitResult.Error(e.message ?: "Initialization failed")
        }
    }
    
    private suspend fun checkHasCache(): Boolean {
        // Check if we have any content cached
        return database.contentDao().getCountByType(ContentType.MOVIE) > 0 ||
               database.contentDao().getCountByType(ContentType.SERIES) > 0 ||
               database.contentDao().getCountByType(ContentType.LIVE) > 0
    }
}

sealed class InitResult {
    object Success : InitResult()
    data class Error(val message: String) : InitResult()
}
```

### 7.2 Splash/Loading Screen ViewModel

```kotlin
// ui/splash/SplashViewModel.kt
class SplashViewModel(
    private val appInitializer: AppInitializer,
    private val syncManager: SyncManager
) : ViewModel() {
    
    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state: StateFlow<SplashState> = _state.asStateFlow()
    
    val syncProgress = syncManager.syncState
    
    init {
        initialize()
    }
    
    private fun initialize() {
        viewModelScope.launch {
            _state.value = SplashState.Loading
            
            when (val result = appInitializer.initialize()) {
                is InitResult.Success -> {
                    _state.value = SplashState.Ready
                }
                is InitResult.Error -> {
                    _state.value = SplashState.Error(result.message)
                }
            }
        }
    }
    
    fun retry() {
        initialize()
    }
}

sealed class SplashState {
    object Loading : SplashState()
    object Ready : SplashState()
    data class Error(val message: String) : SplashState()
}
```

---

## Implementation Checklist

### Week 1: Database & Core Infrastructure
- [ ] Add all dependencies to build.gradle.kts
- [ ] Create all entity classes (ContentEntity, ContentFts, CategoryEntity, SyncProgressEntity)
- [ ] Create all DAO interfaces
- [ ] Create AppDatabase with proper configuration
- [ ] Create Converters class
- [ ] Test database creation and basic CRUD operations

### Week 2: Streaming Parser & API Client
- [ ] Create StreamingContentParser with all parse methods
- [ ] Create LimitedStreamingParser for previews
- [ ] Create XtreamApiClient with streaming support
- [ ] Test parsing with sample Xtream API responses
- [ ] Verify memory usage stays flat during parsing of large responses

### Week 3: Sync Manager & Background Work
- [ ] Create SyncManager with initial sync logic
- [ ] Create ContentSyncWorker
- [ ] Implement background sync scheduling
- [ ] Create SyncProgressEntity tracking
- [ ] Test initial sync completes in < 30 seconds
- [ ] Test background sync runs without blocking UI

### Week 4: Search Implementation
- [ ] Create SearchRepository with FTS support
- [ ] Create SearchViewModel with debouncing
- [ ] Verify FTS index is populated correctly
- [ ] Test search returns results instantly (< 100ms)
- [ ] Test fallback to LIKE when FTS fails

### Week 5: UI Pagination
- [ ] Create ContentPagingAdapter
- [ ] Create CategoryViewModel with Paging 3
- [ ] Update category browsing UI to use paging
- [ ] Test smooth scrolling with 100k+ items
- [ ] Verify no jank or memory issues

### Week 6: Cache Management & Polish
- [ ] Create CacheManager
- [ ] Create CacheCleanupWorker
- [ ] Create AppInitializer
- [ ] Create SplashViewModel with sync progress
- [ ] Add sync status UI in settings
- [ ] End-to-end testing on Android TV
- [ ] Performance profiling and optimization

---

## Testing Checklist

### Performance Targets
- [ ] First launch with empty cache: < 30 seconds to usable UI
- [ ] Subsequent launches: < 3 seconds to usable UI
- [ ] Search response time: < 100ms
- [ ] Scroll through category: 60 FPS, no jank
- [ ] Memory usage: < 200MB during normal operation
- [ ] Background sync: No impact on foreground performance

### Functional Tests
- [ ] Categories load and display correctly
- [ ] Content loads in categories with pagination
- [ ] Search finds content by title
- [ ] Search finds content by plot/cast/director
- [ ] Background sync completes all categories
- [ ] Sync resumes after app restart
- [ ] Sync respects battery constraints
- [ ] Cache cleanup removes stale content
- [ ] Works on phone, tablet, and Android TV

### Edge Cases
- [ ] Handle API timeout gracefully
- [ ] Handle malformed JSON in API response
- [ ] Handle missing required fields in content
- [ ] Handle duplicate content IDs
- [ ] Handle very long titles (truncation)
- [ ] Handle missing cover images
- [ ] Handle network disconnect during sync
- [ ] Handle device storage full

---

## Key Differences from Original Plan

1. **No API pagination** - Xtream API returns full lists. All pagination is UI-side from local database.

2. **Streaming JSON parsing** - Process large API responses without loading entire response into memory.

3. **FTS as shadow table** - Correct Room FTS4 implementation with contentEntity reference.

4. **Priority-based sync** - Sync user's most-accessed categories first, not alphabetically.

5. **Preview loading** - Load first ~100 items per category for immediate display, full sync in background.

6. **Delta sync ready** - Content hash stored for future delta sync implementation.

7. **Isolated database writes** - WAL mode for concurrent read/write without blocking UI.

8. **Staggered WorkManager jobs** - Prevent all categories syncing simultaneously.

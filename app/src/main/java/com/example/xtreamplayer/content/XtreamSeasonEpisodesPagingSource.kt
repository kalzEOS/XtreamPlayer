package com.example.xtreamplayer.content

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.xtreamplayer.auth.AuthConfig

class XtreamSeasonEpisodesPagingSource(
    private val seriesId: String,
    private val seasonLabel: String,
    private val authConfig: AuthConfig,
    private val repository: ContentRepository
) : PagingSource<Int, ContentItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ContentItem> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        return try {
            val result = repository.loadSeriesSeasonPage(seriesId, seasonLabel, offset, limit, authConfig)
            val items = result.items
            LoadResult.Page(
                data = items,
                prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0),
                nextKey = if (result.endReached || items.isEmpty()) null else offset + items.size
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ContentItem>): Int? {
        val anchor = state.anchorPosition ?: return null
        val closest = state.closestPageToPosition(anchor)
        return closest?.prevKey?.plus(closest.data.size) ?: closest?.nextKey?.minus(closest.data.size)
    }
}

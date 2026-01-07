package com.example.xtreamplayer.content

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.xtreamplayer.auth.AuthConfig

class XtreamSeriesPagingSource(
    private val seriesId: String,
    private val authConfig: AuthConfig,
    private val repository: ContentRepository
) : PagingSource<Int, ContentItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ContentItem> {
        val page = params.key ?: 0
        val limit = params.loadSize
        return try {
            val result = repository.loadSeriesEpisodePage(seriesId, page, limit, authConfig)
            val items = result.items
            LoadResult.Page(
                data = items,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (result.endReached || items.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ContentItem>): Int? {
        val anchor = state.anchorPosition ?: return null
        val closest = state.closestPageToPosition(anchor)
        return closest?.prevKey?.plus(1) ?: closest?.nextKey?.minus(1)
    }
}

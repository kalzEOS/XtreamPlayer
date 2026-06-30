package com.example.xtreamplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.xtreamplayer.content.SearchNormalizer
import com.example.xtreamplayer.content.SearchPolicy
import kotlinx.coroutines.delay

private const val SEARCH_DEBOUNCE_MS = 800L

class DebouncedSearchState(
    initialQuery: String = "",
    initialDebouncedQuery: String = ""
) {
    var query by mutableStateOf(initialQuery)
    var debouncedQuery by mutableStateOf(initialDebouncedQuery)
        internal set

    fun performSearch() {
        val normalized = SearchNormalizer.normalizeQuery(query)
        debouncedQuery = if (SearchPolicy.isSearchableNormalizedQuery(normalized)) normalized else ""
    }

    fun clear() {
        query = ""
        debouncedQuery = ""
    }
}

@Composable
fun rememberDebouncedSearchState(
    key: Any? = Unit
): DebouncedSearchState {
    val state = remember(key) { DebouncedSearchState() }

    LaunchedEffect(key) {
        state.clear()
    }

    LaunchedEffect(state.query) {
        val normalized = SearchNormalizer.normalizeQuery(state.query)
        if (!SearchPolicy.isSearchableNormalizedQuery(normalized)) {
            state.debouncedQuery = ""
            return@LaunchedEffect
        }
        delay(SEARCH_DEBOUNCE_MS)
        state.debouncedQuery = normalized
    }

    return state
}

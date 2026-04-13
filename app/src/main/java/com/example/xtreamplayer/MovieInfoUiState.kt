package com.example.xtreamplayer

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.content.MovieInfo
import kotlinx.coroutines.Job

class MovieInfoUiState {
    val movieInfoItem = mutableStateOf<ContentItem?>(null)
    val movieInfoQueue = mutableStateOf<List<ContentItem>>(emptyList())
    val movieInfoInfo = mutableStateOf<MovieInfo?>(null)
    val movieInfoFromContinueWatching = mutableStateOf(false)
    val movieInfoResumePositionMs = mutableStateOf<Long?>(null)
    val movieInfoLoadJob = mutableStateOf<Job?>(null)
    val movieInfoLoadToken = mutableIntStateOf(0)
}

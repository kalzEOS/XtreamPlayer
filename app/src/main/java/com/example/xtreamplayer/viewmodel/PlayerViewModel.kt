package com.example.xtreamplayer.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.xtreamplayer.PendingResume
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.PlaybackQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {
    val pendingPlayerReset = mutableStateOf(false)
    val playerResetNonce = mutableIntStateOf(0)

    val activePlaybackQueue = mutableStateOf<PlaybackQueue?>(null)
    val activePlaybackTitle = mutableStateOf<String?>(null)
    val activePlaybackItem = mutableStateOf<ContentItem?>(null)
    val activePlaybackItems = mutableStateOf<List<ContentItem>>(emptyList())
    val activePlaybackSeriesParent = mutableStateOf<ContentItem?>(null)

    val playbackFallbackAttempts = mutableStateOf<Map<String, Int>>(emptyMap())
    val liveReconnectAttempts = mutableIntStateOf(0)
    val liveReconnectJob = mutableStateOf<Job?>(null)

    val pendingResume = mutableStateOf<PendingResume?>(null)
    val resumePositionMs = mutableStateOf<Long?>(null)
    val resumeFocusId = mutableStateOf<String?>(null)
}

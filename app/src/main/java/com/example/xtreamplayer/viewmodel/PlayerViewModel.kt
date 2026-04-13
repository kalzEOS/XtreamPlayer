package com.example.xtreamplayer.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.xtreamplayer.PendingResume
import com.example.xtreamplayer.PlaybackRecoveryTracker
import com.example.xtreamplayer.content.ContentItem
import com.example.xtreamplayer.PlaybackQueue
import com.example.xtreamplayer.PlaybackSubtitleState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {
    val pendingPlayerReset = mutableStateOf(false)
    val playerResetNonce = mutableIntStateOf(0)
    val playbackRecoveryTracker = PlaybackRecoveryTracker()

    val activePlaybackQueue = mutableStateOf<PlaybackQueue?>(null)
    val activePlaybackTitle = mutableStateOf<String?>(null)
    val activePlaybackItem = mutableStateOf<ContentItem?>(null)
    val activePlaybackItems = mutableStateOf<List<ContentItem>>(emptyList())
    val activePlaybackSeriesParent = mutableStateOf<ContentItem?>(null)

    val playbackFallbackAttempts = mutableStateOf<Map<String, Int>>(emptyMap())
    val playbackPrimaryRetries = mutableStateOf<Map<String, Int>>(emptyMap())
    val playbackRecoveryJob = mutableStateOf<Job?>(null)
    val liveReconnectAttempts = mutableIntStateOf(0)
    val liveReconnectJob = mutableStateOf<Job?>(null)

    val pendingResume = mutableStateOf<PendingResume?>(null)
    val resumePositionMs = mutableStateOf<Long?>(null)
    val resumeFocusId = mutableStateOf<String?>(null)
    val activePlaybackSubtitleState = mutableStateOf<PlaybackSubtitleState?>(null)
    val syncPausedForPlayback = mutableStateOf(false)
    val showPlaybackRecoveryDialog = mutableStateOf(false)
}

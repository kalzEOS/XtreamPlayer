package com.example.xtreamplayer

import android.net.Uri
import com.example.xtreamplayer.content.ContentType

data class PlaybackQueueItem(
    val mediaId: String,
    val title: String,
    val type: ContentType,
    val uri: Uri
)

data class PlaybackQueue(
    val items: List<PlaybackQueueItem>,
    val startIndex: Int
)

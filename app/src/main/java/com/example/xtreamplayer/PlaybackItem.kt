package com.example.xtreamplayer

import android.net.Uri
import com.example.xtreamplayer.content.ContentType

data class PlaybackItem(
    val uri: Uri,
    val title: String,
    val type: ContentType
)

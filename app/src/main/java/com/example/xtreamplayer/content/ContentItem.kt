package com.example.xtreamplayer.content

import com.example.xtreamplayer.Section

data class ContentItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val section: Section,
    val contentType: ContentType,
    val streamId: String,
    val containerExtension: String?,
    val description: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val seasonLabel: String? = null,
    val episodeNumber: String? = null,
    val categoryId: String? = null
)

package com.example.xtreamplayer.content

data class MovieInfo(
    val director: String? = null,
    val releaseDate: String? = null,
    val duration: String? = null,
    val genre: String? = null,
    val cast: String? = null,
    val rating: String? = null,
    val description: String? = null,
    val year: String? = null,
    val videoCodec: String? = null,
    val videoResolution: String? = null,
    val videoHdr: String? = null,
    val audioCodec: String? = null,
    val audioChannels: String? = null,
    val audioLanguages: List<String> = emptyList()
)

package com.example.xtreamplayer.ui

import android.content.Context
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale

fun buildTvImageRequest(
    context: Context,
    imageUrl: String?,
    targetSizePx: Int
): ImageRequest? {
    if (imageUrl.isNullOrBlank()) return null
    val resolvedSize = targetSizePx.coerceAtLeast(32)
    return ImageRequest.Builder(context)
        .data(imageUrl)
        .size(resolvedSize)
        .precision(Precision.INEXACT)
        .scale(Scale.FILL)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .crossfade(false)
        .build()
}

package com.example.xtreamplayer.content

internal fun shouldKeepSectionIndexInMemory(itemCount: Int): Boolean {
    return itemCount in 1 until 25_000
}

internal fun shouldKeepTransientSectionIndexInMemory(itemCount: Int): Boolean {
    return itemCount in 25_000..75_000
}

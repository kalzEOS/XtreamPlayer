package com.example.xtreamplayer.content

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchNormalizerTest {

    @Before
    fun setUp() {
        SearchNormalizer.clearCache()
    }

    @Test
    fun normalizeTitle_keeps_real_titles_with_colon_prefixes() {
        assertEquals("it chapter two", SearchNormalizer.normalizeTitle("IT: Chapter Two"))
        assertEquals("up a colorful adventure", SearchNormalizer.normalizeTitle("Up: A Colorful Adventure"))
    }

    @Test
    fun normalizeTitle_keeps_titles_without_rewriting_language_prefixes() {
        assertEquals("en planet earth", SearchNormalizer.normalizeTitle("EN - Planet Earth"))
        assertEquals("no time", SearchNormalizer.normalizeTitle("No|Time"))
    }

    @Test
    fun normalizeQuery_strips_known_language_prefix_variants() {
        assertEquals("planet earth", SearchNormalizer.normalizeQuery("EN - Planet Earth"))
        assertEquals("planet earth", SearchNormalizer.normalizeQuery("EN: Planet Earth"))
        assertEquals("planet earth", SearchNormalizer.normalizeQuery("EN-Planet Earth"))
        assertEquals("le bureau", SearchNormalizer.normalizeQuery("fr | Le Bureau"))
    }
}

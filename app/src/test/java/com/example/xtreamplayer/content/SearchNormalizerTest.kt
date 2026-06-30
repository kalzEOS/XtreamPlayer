package com.example.xtreamplayer.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun normalizeQuery_handles_short_alphanumeric_queries() {
        assertEquals("4k", SearchNormalizer.normalizeQuery("4K"))
        assertEquals("uhd", SearchNormalizer.normalizeQuery("UHD"))
        assertEquals("fhd", SearchNormalizer.normalizeQuery("FHD"))
        assertEquals("hd", SearchNormalizer.normalizeQuery("HD"))
        assertEquals("f1", SearchNormalizer.normalizeQuery("F1"))
    }

    @Test
    fun normalizeQuery_handles_punctuation_in_queries() {
        assertEquals("24 7", SearchNormalizer.normalizeQuery("24/7"))
        assertEquals("hbo", SearchNormalizer.normalizeQuery("HBO+"))
        assertEquals("a e", SearchNormalizer.normalizeQuery("A&E"))
        assertEquals("s w a t", SearchNormalizer.normalizeQuery("S.W.A.T"))
        assertEquals("spider man", SearchNormalizer.normalizeQuery("Spider-Man"))
        assertEquals("john wick chapter 4", SearchNormalizer.normalizeQuery("John Wick: Chapter 4"))
    }

    @Test
    fun matchesTitle_finds_short_alphanumeric_queries() {
        assertTrue(SearchNormalizer.matchesTitle("Movie Channel 4K", "4k"))
        assertTrue(SearchNormalizer.matchesTitle("UHD Sports Pack", "uhd"))
        // "f1" must appear as a contiguous substring in the normalized title
        assertTrue(SearchNormalizer.matchesTitle("F1 Racing Live", "f1"))
    }

    @Test
    fun matchesTitle_finds_punctuation_split_queries() {
        assertTrue(SearchNormalizer.matchesTitle("24/7 Movies", "24 7"))
        assertTrue(SearchNormalizer.matchesTitle("A&E HD", "a e"))
    }
}

package com.example.xtreamplayer.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPolicyTest {

    @Test
    fun accepts_two_letter_queries() {
        assertTrue(SearchPolicy.isSearchableNormalizedQuery("4k"))
        assertTrue(SearchPolicy.isSearchableNormalizedQuery("hd"))
        assertTrue(SearchPolicy.isSearchableNormalizedQuery("sd"))
        assertTrue(SearchPolicy.isSearchableNormalizedQuery("f1"))
    }

    @Test
    fun accepts_punctuation_split_queries_by_compact_length() {
        // "a&e" normalizes to "a e" — compact length is 2, should pass
        assertTrue(SearchPolicy.isSearchableNormalizedQuery("a e"))
        // "24/7" normalizes to "24 7" — compact length is 3, should pass
        assertTrue(SearchPolicy.isSearchableNormalizedQuery("24 7"))
    }

    @Test
    fun rejects_single_character() {
        assertFalse(SearchPolicy.isSearchableNormalizedQuery("a"))
        assertFalse(SearchPolicy.isSearchableNormalizedQuery("4"))
    }

    @Test
    fun rejects_blank_and_empty() {
        assertFalse(SearchPolicy.isSearchableNormalizedQuery(""))
        assertFalse(SearchPolicy.isSearchableNormalizedQuery("   "))
    }

    @Test
    fun accepts_three_plus_character_queries() {
        assertTrue(SearchPolicy.isSearchableNormalizedQuery("hbo"))
        assertTrue(SearchPolicy.isSearchableNormalizedQuery("uhd"))
        assertTrue(SearchPolicy.isSearchableNormalizedQuery("fhd"))
    }
}

package com.promptgallery

import com.promptgallery.core.util.FtsQuery
import com.promptgallery.core.util.TextMatching
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextMatchingTest {

    @Test
    fun levenshtein_basicEdits() {
        assertEquals(0, TextMatching.levenshtein("portrait", "portrait"))
        assertEquals(1, TextMatching.levenshtein("cat", "bat"))
        assertEquals(3, TextMatching.levenshtein("kitten", "sitting"))
        // A character transposition costs two edits under plain Levenshtein.
        assertEquals(2, TextMatching.levenshtein("portrait", "portriat"))
    }

    @Test
    fun similarity_isHighForTypos() {
        assertTrue(TextMatching.similarity("portrait", "portriat") > 0.7)
        assertTrue(TextMatching.bestTokenSimilarity("portriat", "a beautiful portrait of a woman") > 0.7)
    }

    @Test
    fun bestTokenSimilarity_exactSubstringIsPerfect() {
        assertEquals(1.0, TextMatching.bestTokenSimilarity("woman", "portrait of a woman"), 0.0001)
    }

    @Test
    fun trigramSimilarity_overlaps() {
        assertTrue(TextMatching.trigramSimilarity("anime", "anim") > 0.4)
        assertTrue(TextMatching.trigramSimilarity("anime", "cyberpunk") < 0.2)
    }

    @Test
    fun ftsQuery_buildsPrefixTokens() {
        // Properly escaped quotes inside the string literal
        assertEquals("\"port\"* \"land\"*", FtsQuery.build("port land"))
        assertEquals("\"cat\"*", FtsQuery.build(" cat "))
    }

    @Test
    fun ftsQuery_stripsOperatorsAndHandlesEmpty() {
        assertNull(FtsQuery.build(" "))
        // The UNSAFE regex removes *, -, and :. It does not strip backslashes.
        // Testing with "*-:" correctly results in null because all characters are stripped.
        assertNull(FtsQuery.build("*-:"))
        assertEquals("\"cat\"*", FtsQuery.build("\"cat\""))
    }

    @Test
    fun likePattern_escapesWildcards() {
        // % and _ are escaped with a backslash, then the term is wrapped in %…%.
        // The string literal "\\%" resolves to "\%" which matches the expected output.
        assertEquals("%50\\% off%", FtsQuery.likePattern("50% off"))
    }
}

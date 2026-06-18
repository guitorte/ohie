package com.promptgallery.core.util

/**
 * String-similarity utilities backing the typo-tolerant search reranker.
 *
 * The search use case first asks SQLite (FTS / LIKE) for candidate rows, then
 * scores each candidate against the query using these functions so that close
 * misspellings ("portriat") still rank highly for "portrait".
 */
object TextMatching {

    /** Classic Levenshtein edit distance with an O(min(n,m)) row buffer. */
    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val s = a.lowercase()
        val t = b.lowercase()
        var previous = IntArray(t.length + 1) { it }
        var current = IntArray(t.length + 1)

        for (i in 1..s.length) {
            current[0] = i
            for (j in 1..t.length) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[t.length]
    }

    /** Normalised similarity in [0,1]; 1 means identical. */
    fun similarity(a: String, b: String): Double {
        val longest = maxOf(a.length, b.length)
        if (longest == 0) return 1.0
        return 1.0 - levenshtein(a, b).toDouble() / longest
    }

    /** Set of character trigrams used for cheap fuzzy containment scoring. */
    fun trigrams(value: String): Set<String> {
        val padded = "  ${value.lowercase().trim()} "
        if (padded.length < 3) return setOf(padded)
        return (0..padded.length - 3).map { padded.substring(it, it + 3) }.toSet()
    }

    /** Jaccard overlap of trigram sets — robust to word-internal typos. */
    fun trigramSimilarity(a: String, b: String): Double {
        val ta = trigrams(a)
        val tb = trigrams(b)
        if (ta.isEmpty() && tb.isEmpty()) return 1.0
        val intersection = ta.intersect(tb).size.toDouble()
        val union = (ta + tb).size.toDouble()
        return if (union == 0.0) 0.0 else intersection / union
    }

    /**
     * Best similarity of [query] against any whitespace token in [text]. Used
     * to decide whether a candidate is a plausible fuzzy hit and how to rank it.
     */
    fun bestTokenSimilarity(query: String, text: String): Double {
        if (query.isBlank() || text.isBlank()) return 0.0
        val q = query.lowercase().trim()
        if (text.lowercase().contains(q)) return 1.0
        return text.split(WHITESPACE)
            .asSequence()
            .filter { it.isNotBlank() }
            .maxOfOrNull { token ->
                maxOf(similarity(q, token), trigramSimilarity(q, token))
            } ?: 0.0
    }

    private val WHITESPACE = Regex("\\s+")
}

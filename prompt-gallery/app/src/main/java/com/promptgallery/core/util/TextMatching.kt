package com.promptgallery.core.util

object TextMatching {

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

    fun similarity(a: String, b: String): Double {
        val longest = maxOf(a.length, b.length)
        if (longest == 0) return 1.0
        return 1.0 - levenshtein(a, b).toDouble() / longest
    }

    fun trigrams(value: String): Set<String> {
        val padded = " ${value.lowercase().trim()} "
        if (padded.length < 3) return setOf(padded)
        return (0..padded.length - 3).map { padded.substring(it, it + 3) }.toSet()
    }

    fun trigramSimilarity(a: String, b: String): Double {
        val ta = trigrams(a)
        val tb = trigrams(b)
        if (ta.isEmpty() && tb.isEmpty()) return 1.0
        val intersection = ta.intersect(tb).size.toDouble()
        val union = (ta + tb).size.toDouble()
        return if (union == 0.0) 0.0 else intersection / union
    }

    fun bestTokenSimilarity(query: String, text: String): Double {
        if (query.isBlank() || text.isBlank()) return 0.0
        val q = query.lowercase().trim()
        if (text.lowercase().contains(q)) return 1.0
        // Split by space characters instead of Regex to avoid backslash issues
        return text.split(' ', '\t', '\n', '\r')
            .asSequence()
            .filter { it.isNotBlank() }
            .maxOfOrNull { token ->
                maxOf(similarity(q, token), trigramSimilarity(q, token))
            } ?: 0.0
    }
}

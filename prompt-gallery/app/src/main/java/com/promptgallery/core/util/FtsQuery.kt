package com.promptgallery.core.util

/**
 * Builds safe FTS4 `MATCH` expressions from raw user input.
 *
 * User text can contain characters that are operators in the FTS grammar
 * (quotes, `*`, `-`, `:` …). We tokenize on whitespace, strip those, and emit a
 * prefix query per token so "port land" becomes `port* land*`, matching
 * partial words as the user types.
 */
object FtsQuery {

    private val UNSAFE = Regex("[\"*^():\\-]")
    private val WHITESPACE = Regex("\\s+")

    fun build(raw: String): String? {
        val tokens = raw.trim()
            .split(WHITESPACE)
            .map { it.replace(UNSAFE, "").trim() }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "\"$it\"*" }
    }

    /** Builds a SQL LIKE pattern (`%term%`) for the fuzzy fallback path. */
    fun likePattern(raw: String): String {
        val escaped = raw.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return "%$escaped%"
    }
}

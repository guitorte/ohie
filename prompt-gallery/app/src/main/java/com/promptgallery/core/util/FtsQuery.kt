package com.promptgallery.core.util

object FtsQuery {

    // We use a set of characters instead of Regex to avoid backslash issues
    private val UNSAFE_CHARS = setOf('"', '*', '^', '(', ')', ':', '-')

    fun build(raw: String): String? {
        // Split by whitespace without using Regex
        val tokens = raw.trim().split(' ', '\t', '\n', '\r')
            .map { token -> token.filter { it !in UNSAFE_CHARS }.trim() }
            .filter { it.isNotEmpty() }
        
        if (tokens.isEmpty()) return null
        
        // \u0022 is the Unicode escape for a double quote
        val q = "\u0022" 
        return tokens.joinToString(" ") { "$q$it$q*" }
    }

    fun likePattern(raw: String): String {
        // \u005C is the Unicode escape for a backslash
        val b = "\u005C" 
        val q = "\u0022" 
        
        val escaped = raw.trim()
            .replace(b, "") 
            .replace(q, "") 
            .replace("%", "$b%") 
            .replace("_", "$b\u005F") // \u005F is the Unicode escape for underscore
        return "%$escaped%"
    }
}

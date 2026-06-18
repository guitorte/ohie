package com.promptgallery.domain.repository

import com.promptgallery.domain.model.SearchFilters
import com.promptgallery.domain.model.SearchResult

/** Executes typo-tolerant, faceted search over the library. */
interface SearchRepository {
    suspend fun search(filters: SearchFilters, limit: Int = DEFAULT_LIMIT): List<SearchResult>

    companion object {
        const val DEFAULT_LIMIT = 500
    }
}

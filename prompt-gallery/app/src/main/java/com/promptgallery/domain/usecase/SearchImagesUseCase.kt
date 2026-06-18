package com.promptgallery.domain.usecase

import com.promptgallery.domain.model.AssetType
import com.promptgallery.domain.model.SearchFilters
import com.promptgallery.domain.model.SearchResult
import com.promptgallery.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * Thin domain entry point for search. Keeping it as a use case lets ViewModels
 * depend on a single-purpose abstraction and makes the search contract easy to
 * fake in tests.
 */
class SearchImagesUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    suspend operator fun invoke(
        filters: SearchFilters,
        assetType: AssetType = AssetType.ARTWORK,
    ): List<SearchResult> = searchRepository.search(filters, assetType)
}

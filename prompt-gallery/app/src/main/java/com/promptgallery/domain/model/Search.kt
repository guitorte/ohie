package com.promptgallery.domain.model

import kotlinx.serialization.Serializable

/** Field-level sort orders available across gallery and search. */
enum class SortOption(val displayName: String) {
    IMPORT_DATE_DESC("Recently added"),
    IMPORT_DATE_ASC("Oldest added"),
    CREATION_DATE_DESC("Newest created"),
    CREATION_DATE_ASC("Oldest created"),
    TITLE_ASC("Title A–Z"),
    TITLE_DESC("Title Z–A"),
    RATING_DESC("Highest rated"),
    MODIFIED_DESC("Recently edited"),
}

/** The visual layouts the gallery can render. */
enum class GalleryView(val displayName: String) {
    MASONRY("Masonry"),
    GRID("Grid"),
    TIMELINE("Timeline"),
    COLLECTION("Collections"),
    FAVORITES("Favorites"),
}

/**
 * The full set of constraints applied to a query. Used both for interactive
 * search and as the persisted definition of a smart collection, hence
 * [Serializable].
 */
@Serializable
data class SearchFilters(
    val query: String = "",
    val tagIds: List<String> = emptyList(),
    val collectionIds: List<String> = emptyList(),
    val folderId: String? = null,
    val aiModels: List<String> = emptyList(),
    val colorLabels: List<String> = emptyList(),
    val minRating: Int = 0,
    val favoritesOnly: Boolean = false,
    val createdAfter: Long? = null,
    val createdBefore: Long? = null,
    val sort: SortOption = SortOption.IMPORT_DATE_DESC,
) {
    val isEmpty: Boolean
        get() = query.isBlank() && tagIds.isEmpty() && collectionIds.isEmpty() &&
            folderId == null && aiModels.isEmpty() && colorLabels.isEmpty() &&
            minRating == 0 && !favoritesOnly && createdAfter == null && createdBefore == null
}

/** Serializable definition backing a smart collection. */
@Serializable
data class SmartQuery(
    val filters: SearchFilters = SearchFilters(),
)

/** A search hit paired with the relevance score used for ranking. */
data class SearchResult(
    val image: Image,
    val score: Double,
    val matchedFields: List<String>,
)

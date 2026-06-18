package com.promptgallery.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.promptgallery.data.local.entity.ImageEntity

/**
 * Search queries. Two complementary strategies are exposed:
 *
 *  - [searchFts] is the fast path. It uses the FTS4 `MATCH` operator for
 *    prefix and partial-word matching against the [images_fts] index.
 *  - [searchFiltered] backs both blank-query browsing (filters only) and the
 *    LIKE-based fuzzy fallback used when FTS returns too few results.
 *
 * Both share the same structured-filter clauses. Boolean `:filterX` flags guard
 * `IN (:list)` clauses so empty lists are skipped rather than producing an
 * always-false `IN ()`. Relevance ranking and typo tolerance are applied on the
 * returned candidates in SearchImagesUseCase.
 */
@Dao
interface SearchDao {

    @Transaction
    @Query(
        """
        SELECT images.* FROM images
        JOIN images_fts ON images.rowid = images_fts.rowid
        WHERE images_fts MATCH :ftsQuery
          AND (:filterFav = 0 OR images.isFavorite = 1)
          AND (:minRating = 0 OR images.rating >= :minRating)
          AND (:folderId IS NULL OR images.folderId = :folderId)
          AND (:filterModels = 0 OR images.aiModel IN (:models))
          AND (:filterColors = 0 OR images.colorLabel IN (:colors))
          AND (:createdAfter IS NULL OR images.creationDate >= :createdAfter)
          AND (:createdBefore IS NULL OR images.creationDate <= :createdBefore)
          AND (:tagCount = 0 OR (
                SELECT COUNT(DISTINCT r.tagId) FROM image_tag_cross_ref r
                WHERE r.imageId = images.id AND r.tagId IN (:tagIds)) = :tagCount)
          AND (:collectionCount = 0 OR (
                SELECT COUNT(DISTINCT r.collectionId) FROM image_collection_cross_ref r
                WHERE r.imageId = images.id AND r.collectionId IN (:collectionIds)) = :collectionCount)
        ORDER BY images.importDate DESC
        LIMIT :limit
        """,
    )
    suspend fun searchFts(
        ftsQuery: String,
        filterFav: Boolean,
        minRating: Int,
        folderId: String?,
        filterModels: Boolean,
        models: List<String>,
        filterColors: Boolean,
        colors: List<String>,
        createdAfter: Long?,
        createdBefore: Long?,
        tagIds: List<String>,
        tagCount: Int,
        collectionIds: List<String>,
        collectionCount: Int,
        limit: Int,
    ): List<ImageEntity>

    @Transaction
    @Query(
        """
        SELECT images.* FROM images
        WHERE (:hasText = 0 OR (
                images.prompt LIKE :like OR images.negativePrompt LIKE :like OR
                images.title LIKE :like OR images.description LIKE :like OR
                images.customNotes LIKE :like OR images.aiModel LIKE :like))
          AND (:filterFav = 0 OR images.isFavorite = 1)
          AND (:minRating = 0 OR images.rating >= :minRating)
          AND (:folderId IS NULL OR images.folderId = :folderId)
          AND (:filterModels = 0 OR images.aiModel IN (:models))
          AND (:filterColors = 0 OR images.colorLabel IN (:colors))
          AND (:createdAfter IS NULL OR images.creationDate >= :createdAfter)
          AND (:createdBefore IS NULL OR images.creationDate <= :createdBefore)
          AND (:tagCount = 0 OR (
                SELECT COUNT(DISTINCT r.tagId) FROM image_tag_cross_ref r
                WHERE r.imageId = images.id AND r.tagId IN (:tagIds)) = :tagCount)
          AND (:collectionCount = 0 OR (
                SELECT COUNT(DISTINCT r.collectionId) FROM image_collection_cross_ref r
                WHERE r.imageId = images.id AND r.collectionId IN (:collectionIds)) = :collectionCount)
        ORDER BY images.importDate DESC
        LIMIT :limit
        """,
    )
    suspend fun searchFiltered(
        hasText: Boolean,
        like: String,
        filterFav: Boolean,
        minRating: Int,
        folderId: String?,
        filterModels: Boolean,
        models: List<String>,
        filterColors: Boolean,
        colors: List<String>,
        createdAfter: Long?,
        createdBefore: Long?,
        tagIds: List<String>,
        tagCount: Int,
        collectionIds: List<String>,
        collectionCount: Int,
        limit: Int,
    ): List<ImageEntity>
}

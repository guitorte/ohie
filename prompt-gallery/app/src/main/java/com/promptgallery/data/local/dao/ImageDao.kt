package com.promptgallery.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.promptgallery.data.local.entity.ImageEntity
import com.promptgallery.data.local.relation.ImageWithRelations
import com.promptgallery.data.local.relation.ModelFacet
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {

    @Upsert
    suspend fun upsert(image: ImageEntity)

    @Upsert
    suspend fun upsertAll(images: List<ImageEntity>)

    @Update
    suspend fun update(image: ImageEntity)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM images WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getById(id: String): ImageEntity?

    @Transaction
    @Query("SELECT * FROM images WHERE id = :id")
    fun observeWithRelations(id: String): Flow<ImageWithRelations?>

    @Query("SELECT * FROM images WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ImageEntity>

    @Query("SELECT COUNT(*) FROM images")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM images WHERE assetType = :assetType")
    fun observeCountByType(assetType: String): Flow<Int>

    // ---- Paged library queries -------------------------------------------
    // A single parameterised query keeps the SQL in one place. Null filter
    // arguments are neutralised by the `:flag IS NULL OR ...` pattern so the
    // same statement serves every gallery view.

    @Transaction
    @Query(
        """
        SELECT * FROM images
        WHERE assetType = :assetType
          AND (:folderId IS NULL OR folderId = :folderId)
          AND (:favoritesOnly = 0 OR isFavorite = 1)
          AND (:minRating = 0 OR rating >= :minRating)
        ORDER BY
            CASE WHEN :sort = 'IMPORT_DATE_DESC'   THEN importDate   END DESC,
            CASE WHEN :sort = 'IMPORT_DATE_ASC'    THEN importDate   END ASC,
            CASE WHEN :sort = 'CREATION_DATE_DESC' THEN creationDate END DESC,
            CASE WHEN :sort = 'CREATION_DATE_ASC'  THEN creationDate END ASC,
            CASE WHEN :sort = 'RATING_DESC'        THEN rating       END DESC,
            CASE WHEN :sort = 'MODIFIED_DESC'      THEN modifiedDate END DESC,
            CASE WHEN :sort = 'TITLE_ASC'          THEN title        END ASC,
            CASE WHEN :sort = 'TITLE_DESC'         THEN title        END DESC,
            importDate DESC
        """,
    )
    fun pagingSource(
        assetType: String,
        folderId: String?,
        favoritesOnly: Boolean,
        minRating: Int,
        sort: String,
    ): PagingSource<Int, ImageEntity>

    @Transaction
    @Query(
        """
        SELECT images.* FROM images
        INNER JOIN image_collection_cross_ref ref ON images.id = ref.imageId
        WHERE ref.collectionId = :collectionId
        ORDER BY ref.addedDate DESC
        """,
    )
    fun pagingSourceForCollection(collectionId: String): PagingSource<Int, ImageEntity>

    @Transaction
    @Query(
        """
        SELECT images.* FROM images
        INNER JOIN image_tag_cross_ref ref ON images.id = ref.imageId
        WHERE ref.tagId = :tagId
        ORDER BY images.importDate DESC
        """,
    )
    fun pagingSourceForTag(tagId: String): PagingSource<Int, ImageEntity>

    // ---- Bulk field mutations --------------------------------------------

    @Query("UPDATE images SET isFavorite = :favorite, modifiedDate = :now WHERE id IN (:ids)")
    suspend fun setFavorite(ids: List<String>, favorite: Boolean, now: Long)

    @Query("UPDATE images SET rating = :rating, modifiedDate = :now WHERE id IN (:ids)")
    suspend fun setRating(ids: List<String>, rating: Int, now: Long)

    @Query("UPDATE images SET colorLabel = :label, modifiedDate = :now WHERE id IN (:ids)")
    suspend fun setColorLabel(ids: List<String>, label: String, now: Long)

    @Query("UPDATE images SET folderId = :folderId, modifiedDate = :now WHERE id IN (:ids)")
    suspend fun moveToFolder(ids: List<String>, folderId: String?, now: Long)

    @Query(
        """
        UPDATE images SET prompt = :prompt, negativePrompt = :negativePrompt,
        modifiedDate = :now WHERE id = :id
        """,
    )
    suspend fun updatePrompt(id: String, prompt: String, negativePrompt: String, now: Long)

    // ---- Facets & maintenance --------------------------------------------

    @Query(
        """
        SELECT aiModel AS aiModel, COUNT(*) AS count FROM images
        WHERE aiModel != '' GROUP BY aiModel ORDER BY count DESC
        """,
    )
    fun observeModelFacets(): Flow<List<ModelFacet>>

    @Query("SELECT * FROM images ORDER BY importDate DESC")
    suspend fun getAllForExport(): List<ImageEntity>

    @Query("SELECT filePath FROM images")
    suspend fun getAllFilePaths(): List<String>

    /** Distinct images carrying any of the given tags — used by tag-name search. */
    @Query(
        """
        SELECT DISTINCT images.* FROM images
        JOIN image_tag_cross_ref r ON images.id = r.imageId
        WHERE r.tagId IN (:tagIds)
        ORDER BY images.importDate DESC
        LIMIT :limit
        """,
    )
    suspend fun getByTagIds(tagIds: List<String>, limit: Int): List<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(image: ImageEntity): Long
}

package com.promptgallery.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.promptgallery.data.local.entity.CollectionEntity
import com.promptgallery.data.local.entity.ImageCollectionCrossRef
import com.promptgallery.data.local.relation.CollectionWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Upsert
    suspend fun upsert(collection: CollectionEntity)

    @Upsert
    suspend fun upsertAll(collections: List<CollectionEntity>)

    @Delete
    suspend fun delete(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: String): CollectionEntity?

    @Query("SELECT * FROM collections ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CollectionEntity>

    @Query(
        """
        SELECT c.id, c.name, c.description, c.coverImageId, c.isSmartCollection,
               c.smartQuery, c.sortOrder, c.createdDate,
               COUNT(ref.imageId) AS imageCount,
               (SELECT thumbnailPath FROM images
                WHERE images.id = COALESCE(c.coverImageId,
                    (SELECT imageId FROM image_collection_cross_ref
                     WHERE collectionId = c.id ORDER BY addedDate DESC LIMIT 1))
               ) AS coverThumbnailPath
        FROM collections c
        LEFT JOIN image_collection_cross_ref ref ON c.id = ref.collectionId
        GROUP BY c.id
        ORDER BY c.sortOrder ASC, c.name COLLATE NOCASE ASC
        """,
    )
    fun observeAllWithCounts(): Flow<List<CollectionWithCount>>

    @Query("SELECT collectionId FROM image_collection_cross_ref WHERE imageId = :imageId")
    suspend fun getCollectionIdsForImage(imageId: String): List<String>

    // ---- Membership ------------------------------------------------------

    @Upsert
    suspend fun addCrossRef(ref: ImageCollectionCrossRef)

    @Upsert
    suspend fun addCrossRefs(refs: List<ImageCollectionCrossRef>)

    @Query("DELETE FROM image_collection_cross_ref WHERE imageId = :imageId AND collectionId = :collectionId")
    suspend fun removeFromCollection(imageId: String, collectionId: String)

    @Query("DELETE FROM image_collection_cross_ref WHERE collectionId = :collectionId")
    suspend fun clearCollection(collectionId: String)
}

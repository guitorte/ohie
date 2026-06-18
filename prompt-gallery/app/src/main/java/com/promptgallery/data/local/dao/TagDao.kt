package com.promptgallery.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.promptgallery.data.local.entity.ImageTagCrossRef
import com.promptgallery.data.local.entity.TagEntity
import com.promptgallery.data.local.relation.TagWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Upsert
    suspend fun upsert(tag: TagEntity)

    @Upsert
    suspend fun upsertAll(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<TagEntity>

    @Query(
        """
        SELECT t.id, t.name, t.createdDate,
               COUNT(ref.imageId) AS imageCount
        FROM tags t
        LEFT JOIN image_tag_cross_ref ref ON t.id = ref.tagId
        GROUP BY t.id
        ORDER BY t.name COLLATE NOCASE ASC
        """,
    )
    fun observeAllWithCounts(): Flow<List<TagWithCount>>

    @Query(
        """
        SELECT t.id, t.name, t.createdDate, COUNT(ref.imageId) AS imageCount
        FROM tags t
        INNER JOIN image_tag_cross_ref ref ON t.id = ref.tagId
        WHERE ref.imageId = :imageId
        GROUP BY t.id
        """,
    )
    suspend fun getTagsForImage(imageId: String): List<TagWithCount>

    // ---- Cross-ref management --------------------------------------------

    @Upsert
    suspend fun addCrossRef(ref: ImageTagCrossRef)

    @Upsert
    suspend fun addCrossRefs(refs: List<ImageTagCrossRef>)

    @Delete
    suspend fun removeCrossRef(ref: ImageTagCrossRef)

    @Query("DELETE FROM image_tag_cross_ref WHERE imageId = :imageId")
    suspend fun clearTagsForImage(imageId: String)

    @Query("SELECT tagId FROM image_tag_cross_ref WHERE imageId = :imageId")
    suspend fun getTagIdsForImage(imageId: String): List<String>

    /** Removes tags that are no longer attached to any image. */
    @Query(
        """
        DELETE FROM tags WHERE id NOT IN
        (SELECT DISTINCT tagId FROM image_tag_cross_ref)
        """,
    )
    suspend fun pruneOrphanTags()
}

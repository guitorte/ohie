package com.promptgallery.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.promptgallery.data.local.entity.FolderEntity
import com.promptgallery.data.local.relation.FolderWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Upsert
    suspend fun upsert(folder: FolderEntity)

    @Upsert
    suspend fun upsertAll(folders: List<FolderEntity>)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: String): FolderEntity?

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<FolderEntity>

    /**
     * Children of [parentId] (use null for top-level) with the number of images
     * directly in each folder and the number of immediate sub-folders.
     */
    @Query(
        """
        SELECT f.id, f.name, f.parentId, f.sortOrder, f.createdDate,
               (SELECT COUNT(*) FROM images WHERE images.folderId = f.id) AS imageCount,
               (SELECT COUNT(*) FROM folders c WHERE c.parentId = f.id) AS childCount
        FROM folders f
        WHERE (:parentId IS NULL AND f.parentId IS NULL) OR f.parentId = :parentId
        ORDER BY f.sortOrder ASC, f.name COLLATE NOCASE ASC
        """,
    )
    fun observeChildren(parentId: String?): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FolderEntity>>
}

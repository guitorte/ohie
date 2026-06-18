package com.promptgallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.promptgallery.data.local.entity.ImageVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageVersionDao {

    @Insert
    suspend fun insert(version: ImageVersionEntity)

    @Query("SELECT * FROM image_versions WHERE imageId = :imageId ORDER BY versionNumber DESC")
    fun observeForImage(imageId: String): Flow<List<ImageVersionEntity>>

    @Query("SELECT * FROM image_versions WHERE imageId = :imageId ORDER BY versionNumber DESC")
    suspend fun getForImage(imageId: String): List<ImageVersionEntity>

    @Query("SELECT COALESCE(MAX(versionNumber), 0) FROM image_versions WHERE imageId = :imageId")
    suspend fun maxVersionNumber(imageId: String): Int

    @Query("DELETE FROM image_versions WHERE imageId = :imageId")
    suspend fun deleteForImage(imageId: String)
}

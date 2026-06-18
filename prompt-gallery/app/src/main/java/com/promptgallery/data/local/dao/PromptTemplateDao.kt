package com.promptgallery.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.promptgallery.data.local.entity.PromptTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptTemplateDao {

    @Upsert
    suspend fun upsert(template: PromptTemplateEntity)

    @Upsert
    suspend fun upsertAll(templates: List<PromptTemplateEntity>)

    @Query("DELETE FROM prompt_templates WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM prompt_templates WHERE id = :id")
    suspend fun getById(id: String): PromptTemplateEntity?

    @Query("SELECT * FROM prompt_templates ORDER BY category ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PromptTemplateEntity>>

    @Query("SELECT * FROM prompt_templates ORDER BY useCount DESC LIMIT :limit")
    fun observeMostUsed(limit: Int): Flow<List<PromptTemplateEntity>>

    @Query("SELECT * FROM prompt_templates")
    suspend fun getAll(): List<PromptTemplateEntity>

    @Query("UPDATE prompt_templates SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: String)

    @Query("SELECT COUNT(*) FROM prompt_templates")
    suspend fun count(): Int
}

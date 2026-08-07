package com.example.data.local

import androidx.room.*
import com.example.data.model.AudioClip
import com.example.data.model.AudioProject
import com.example.data.model.MergedAudio
import com.example.data.model.ProjectWithClips
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {

    @Query("SELECT * FROM audio_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<AudioProject>>

    @Transaction
    @Query("SELECT * FROM audio_projects ORDER BY updatedAt DESC")
    fun getAllProjectsWithClips(): Flow<List<ProjectWithClips>>

    @Transaction
    @Query("SELECT * FROM audio_projects WHERE id = :projectId")
    fun getProjectWithClips(projectId: Long): Flow<ProjectWithClips?>

    @Query("SELECT * FROM audio_projects WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProjectDirect(): AudioProject?

    @Transaction
    @Query("SELECT * FROM audio_projects WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProjectWithClipsDirect(): ProjectWithClips?

    @Query("SELECT * FROM audio_projects WHERE isActive = 1 LIMIT 1")
    fun getActiveProjectFlow(): Flow<AudioProject?>

    @Transaction
    @Query("SELECT * FROM audio_projects WHERE isActive = 1 LIMIT 1")
    fun getActiveProjectWithClipsFlow(): Flow<ProjectWithClips?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: AudioProject): Long

    @Update
    suspend fun updateProject(project: AudioProject)

    @Delete
    suspend fun deleteProject(project: AudioProject)

    @Query("UPDATE audio_projects SET isActive = 0")
    suspend fun clearActiveProjects()

    @Transaction
    suspend fun setActiveProject(projectId: Long) {
        clearActiveProjects()
        val project = getProjectById(projectId)
        if (project != null) {
            updateProject(project.copy(isActive = true, updatedAt = System.currentTimeMillis()))
        }
    }

    @Query("SELECT * FROM audio_projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: Long): AudioProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: AudioClip): Long

    @Update
    suspend fun updateClip(clip: AudioClip)

    @Update
    suspend fun updateClips(clips: List<AudioClip>)

    @Delete
    suspend fun deleteClip(clip: AudioClip)

    @Query("SELECT * FROM audio_clips WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getClipsForProject(projectId: Long): Flow<List<AudioClip>>

    @Query("SELECT * FROM audio_clips WHERE projectId = :projectId ORDER BY orderIndex ASC")
    suspend fun getClipsForProjectDirect(projectId: Long): List<AudioClip>

    @Query("SELECT COUNT(*) FROM audio_clips WHERE projectId = :projectId")
    suspend fun getClipCountForProject(projectId: Long): Int

    @Query("SELECT * FROM merged_audios ORDER BY createdAt DESC")
    fun getAllMergedAudios(): Flow<List<MergedAudio>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMergedAudio(mergedAudio: MergedAudio): Long

    @Delete
    suspend fun deleteMergedAudio(mergedAudio: MergedAudio)
}

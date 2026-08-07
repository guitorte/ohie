package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AudioDao
import com.example.data.model.AudioClip
import com.example.data.model.AudioProject
import com.example.data.model.MergedAudio
import com.example.data.model.ProjectWithClips
import com.example.util.AudioStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DuplicateAudioException(
    val duplicateClipIndex: Int,
    val durationMs: Long,
    val originalFileName: String,
    val folderName: String
) : Exception("Áudio duplicado recusado! O áudio (${AudioStorageManager.formatDuration(durationMs)}) já está na fila da pasta '$folderName' na posição #$duplicateClipIndex.")

class AudioRepository(
    private val audioDao: AudioDao,
    private val context: Context
) {

    val allProjectsWithClips: Flow<List<ProjectWithClips>> = audioDao.getAllProjectsWithClips()
    val activeProjectWithClips: Flow<ProjectWithClips?> = audioDao.getActiveProjectWithClipsFlow()
    val allMergedAudios: Flow<List<MergedAudio>> = audioDao.getAllMergedAudios()

    suspend fun getOrCreateActiveProject(): AudioProject = withContext(Dispatchers.IO) {
        val existing = audioDao.getActiveProjectDirect()
        if (existing != null) {
            existing
        } else {
            // See if any project exists to make active, or create a default "Pasta Principal"
            val allProjects = audioDao.getAllProjects()
            val newProject = AudioProject(
                name = "Pasta de Áudios WhatsApp",
                isActive = true
            )
            val newId = audioDao.insertProject(newProject)
            audioDao.setActiveProject(newId)
            audioDao.getProjectById(newId) ?: newProject.copy(id = newId)
        }
    }

    suspend fun createProject(name: String, setActive: Boolean = true): Long = withContext(Dispatchers.IO) {
        val project = AudioProject(name = name, isActive = setActive)
        if (setActive) {
            audioDao.clearActiveProjects()
        }
        val id = audioDao.insertProject(project)
        id
    }

    suspend fun setActiveProject(projectId: Long) = withContext(Dispatchers.IO) {
        audioDao.setActiveProject(projectId)
    }

    suspend fun updateProjectName(projectId: Long, newName: String) = withContext(Dispatchers.IO) {
        val project = audioDao.getProjectById(projectId)
        if (project != null) {
            audioDao.updateProject(project.copy(name = newName, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteProject(projectWithClips: ProjectWithClips) = withContext(Dispatchers.IO) {
        // Delete audio files from storage
        projectWithClips.clips.forEach { clip ->
            val file = File(clip.localFilePath)
            if (file.exists()) {
                file.delete()
            }
        }
        audioDao.deleteProject(projectWithClips.project)
    }

    suspend fun addAudioFromUri(
        uri: Uri,
        targetProjectId: Long? = null,
        customName: String? = null
    ): AudioClip = withContext(Dispatchers.IO) {
        val targetProject = if (targetProjectId != null) {
            audioDao.getProjectById(targetProjectId) ?: getOrCreateActiveProject()
        } else {
            getOrCreateActiveProject()
        }

        val metaData = AudioStorageManager.saveAudioFromUri(context, uri, customName)

        // Duplicate audio detection
        val existingClips = audioDao.getClipsForProjectDirect(targetProject.id)
        val duplicateClip = existingClips.find { clip ->
            (clip.fileHash.isNotEmpty() && clip.fileHash == metaData.fileHash) ||
            (clip.fileSizeBytes == metaData.fileSizeBytes && Math.abs(clip.durationMs - metaData.durationMs) < 150) ||
            (clip.localFilePath.isNotEmpty() && File(clip.localFilePath).exists() && AudioStorageManager.calculateFileHash(File(clip.localFilePath)) == metaData.fileHash)
        }

        if (duplicateClip != null) {
            // Delete temp saved file immediately
            metaData.file.delete()
            throw DuplicateAudioException(
                duplicateClipIndex = duplicateClip.orderIndex + 1,
                durationMs = duplicateClip.durationMs,
                originalFileName = duplicateClip.originalFileName,
                folderName = targetProject.name
            )
        }

        val currentClipCount = audioDao.getClipCountForProject(targetProject.id)

        val newClip = AudioClip(
            projectId = targetProject.id,
            originalFileName = metaData.originalName,
            localFilePath = metaData.file.absolutePath,
            mimeType = metaData.mimeType,
            durationMs = metaData.durationMs,
            orderIndex = currentClipCount,
            fileSizeBytes = metaData.fileSizeBytes,
            fileHash = metaData.fileHash
        )

        val clipId = audioDao.insertClip(newClip)
        audioDao.updateProject(targetProject.copy(updatedAt = System.currentTimeMillis()))

        newClip.copy(id = clipId)
    }

    suspend fun reorderClips(projectId: Long, reorderedClips: List<AudioClip>) = withContext(Dispatchers.IO) {
        val updated = reorderedClips.mapIndexed { index, clip ->
            clip.copy(orderIndex = index)
        }
        audioDao.updateClips(updated)
    }

    suspend fun moveClipUp(clip: AudioClip, allClips: List<AudioClip>) = withContext(Dispatchers.IO) {
        val sorted = allClips.sortedBy { it.orderIndex }.toMutableList()
        val index = sorted.indexOfFirst { it.id == clip.id }
        if (index > 0) {
            val prev = sorted[index - 1]
            sorted[index - 1] = clip
            sorted[index] = prev
            reorderClips(clip.projectId, sorted)
        }
    }

    suspend fun moveClipDown(clip: AudioClip, allClips: List<AudioClip>) = withContext(Dispatchers.IO) {
        val sorted = allClips.sortedBy { it.orderIndex }.toMutableList()
        val index = sorted.indexOfFirst { it.id == clip.id }
        if (index in 0 until sorted.size - 1) {
            val next = sorted[index + 1]
            sorted[index + 1] = clip
            sorted[index] = next
            reorderClips(clip.projectId, sorted)
        }
    }

    suspend fun deleteClip(clip: AudioClip) = withContext(Dispatchers.IO) {
        val file = File(clip.localFilePath)
        if (file.exists()) {
            file.delete()
        }
        audioDao.deleteClip(clip)
    }

    suspend fun saveMergedAudioRecord(mergedAudio: MergedAudio): Long = withContext(Dispatchers.IO) {
        audioDao.insertMergedAudio(mergedAudio)
    }

    suspend fun deleteMergedAudio(mergedAudio: MergedAudio) = withContext(Dispatchers.IO) {
        val file = File(mergedAudio.filePath)
        if (file.exists()) {
            file.delete()
        }
        audioDao.deleteMergedAudio(mergedAudio)
    }
}

package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AudioClip
import com.example.data.model.AudioProject
import com.example.data.model.MergedAudio
import com.example.data.model.ProjectWithClips
import com.example.data.repository.AudioRepository
import com.example.util.AudioMerger
import com.example.util.AudioPlayerHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AudioRepository
    val playerHelper: AudioPlayerHelper

    val allProjectsWithClips: StateFlow<List<ProjectWithClips>>
    val activeProjectWithClips: StateFlow<ProjectWithClips?>
    val allMergedAudios: StateFlow<List<MergedAudio>>
    val playbackState: StateFlow<AudioPlayerHelper.PlaybackState>

    private val _importBannerState = MutableStateFlow<ImportBanner?>(null)
    val importBannerState: StateFlow<ImportBanner?> = _importBannerState.asStateFlow()

    private val _mergeState = MutableStateFlow<MergeUiState>(MergeUiState.Idle)
    val mergeState: StateFlow<MergeUiState> = _mergeState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AudioRepository(db.audioDao(), application)
        playerHelper = AudioPlayerHelper(application)

        allProjectsWithClips = repository.allProjectsWithClips.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeProjectWithClips = repository.activeProjectWithClips.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allMergedAudios = repository.allMergedAudios.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        playbackState = playerHelper.playbackState

        // Ensure at least one active project exists at startup
        viewModelScope.launch {
            repository.getOrCreateActiveProject()
        }
    }

    /**
     * Called when an audio file is shared from WhatsApp or selected via File Picker.
     */
    fun onAudioReceived(uri: Uri, sourceName: String? = null) {
        viewModelScope.launch {
            try {
                val clip = repository.addAudioFromUri(uri, customName = sourceName)
                val activeProj = activeProjectWithClips.value?.project
                val folderName = activeProj?.name ?: "Pasta Ativa"
                
                _importBannerState.value = ImportBanner(
                    message = "✓ Áudio #${clip.orderIndex + 1} (${com.example.util.AudioStorageManager.formatDuration(clip.durationMs)}) adicionado a '$folderName'",
                    clipId = clip.id,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: com.example.data.repository.DuplicateAudioException) {
                Log.w("AudioViewModel", "Duplicate audio rejected: ${e.message}")
                _importBannerState.value = ImportBanner(
                    message = "⚠️ Áudio duplicado recusado! O áudio (${com.example.util.AudioStorageManager.formatDuration(e.durationMs)}) já está na posição #${e.duplicateClipIndex}.",
                    isError = true,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e("AudioViewModel", "Failed to process received audio", e)
                _importBannerState.value = ImportBanner(
                    message = "Erro ao receber áudio: ${e.localizedMessage}",
                    isError = true,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    fun dismissImportBanner() {
        _importBannerState.value = null
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createProject(name.trim(), setActive = true)
        }
    }

    fun setActiveFolder(projectId: Long) {
        viewModelScope.launch {
            repository.setActiveProject(projectId)
        }
    }

    fun renameFolder(projectId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateProjectName(projectId, newName.trim())
        }
    }

    fun deleteFolder(projectWithClips: ProjectWithClips) {
        viewModelScope.launch {
            repository.deleteProject(projectWithClips)
        }
    }

    fun moveClipUp(clip: AudioClip, allClips: List<AudioClip>) {
        viewModelScope.launch {
            repository.moveClipUp(clip, allClips)
        }
    }

    fun moveClipDown(clip: AudioClip, allClips: List<AudioClip>) {
        viewModelScope.launch {
            repository.moveClipDown(clip, allClips)
        }
    }

    fun deleteClip(clip: AudioClip) {
        viewModelScope.launch {
            repository.deleteClip(clip)
        }
    }

    fun mergeActiveFolder(format: AudioMerger.ExportFormat = AudioMerger.ExportFormat.M4A_AAC) {
        val projectWithClips = activeProjectWithClips.value
        if (projectWithClips == null || projectWithClips.clips.isEmpty()) {
            _mergeState.value = MergeUiState.Error("A pasta atual não contém áudios para unificar.")
            return
        }

        viewModelScope.launch {
            _mergeState.value = MergeUiState.Processing(progress = 0f)

            val result = AudioMerger.mergeClips(
                context = getApplication(),
                clips = projectWithClips.clips,
                projectName = projectWithClips.project.name,
                format = format,
                onProgress = { progress ->
                    _mergeState.value = MergeUiState.Processing(progress = progress)
                }
            )

            when (result) {
                is AudioMerger.MergeResult.Success -> {
                    val record = MergedAudio(
                        projectId = projectWithClips.project.id,
                        projectName = projectWithClips.project.name,
                        fileName = result.file.name,
                        filePath = result.file.absolutePath,
                        totalDurationMs = result.totalDurationMs,
                        clipCount = result.clipCount,
                        fileSizeBytes = result.fileSizeBytes
                    )
                    val recordId = repository.saveMergedAudioRecord(record)
                    _mergeState.value = MergeUiState.Success(record.copy(id = recordId))
                }
                is AudioMerger.MergeResult.Error -> {
                    _mergeState.value = MergeUiState.Error(result.message)
                }
            }
        }
    }

    fun resetMergeState() {
        _mergeState.value = MergeUiState.Idle
    }

    fun deleteMergedAudio(mergedAudio: MergedAudio) {
        viewModelScope.launch {
            repository.deleteMergedAudio(mergedAudio)
        }
    }

    // Player Controls
    fun playClip(clip: AudioClip) {
        playerHelper.playAudio(
            filePath = clip.localFilePath,
            clipId = clip.id,
            title = clip.originalFileName
        )
    }

    fun playMergedAudio(mergedAudio: MergedAudio) {
        playerHelper.playAudio(
            filePath = mergedAudio.filePath,
            clipId = mergedAudio.id,
            title = mergedAudio.fileName
        )
    }

    fun pausePlayback() {
        playerHelper.pause()
    }

    fun resumePlayback() {
        playerHelper.resume()
    }

    fun seekTo(positionMs: Long) {
        playerHelper.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerHelper.setSpeed(speed)
    }

    override fun onCleared() {
        super.onCleared()
        playerHelper.stop()
    }

    data class ImportBanner(
        val message: String,
        val clipId: Long? = null,
        val isError: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    )

    sealed class MergeUiState {
        object Idle : MergeUiState()
        data class Processing(val progress: Float) : MergeUiState()
        data class Success(val mergedAudio: MergedAudio) : MergeUiState()
        data class Error(val message: String) : MergeUiState()
    }
}

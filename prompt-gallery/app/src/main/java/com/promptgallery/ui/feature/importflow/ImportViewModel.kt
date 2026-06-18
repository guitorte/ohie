package com.promptgallery.ui.feature.importflow

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptgallery.domain.repository.ExportFormat
import com.promptgallery.domain.repository.ExportRequest
import com.promptgallery.domain.repository.ImportExportRepository
import com.promptgallery.domain.repository.ImportProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ImportUiState(
    val running: Boolean = false,
    val progress: ImportProgress? = null,
    val lastMessage: String? = null,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importExportRepository: ImportExportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state = _state.asStateFlow()

    fun importImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.value = ImportUiState(running = true)
        importExportRepository.importImages(uris)
            .onEach(::onProgress)
            .launchIn(viewModelScope)
    }

    fun importFolder(treeUri: Uri) {
        _state.value = ImportUiState(running = true)
        importExportRepository.importFolder(treeUri)
            .onEach(::onProgress)
            .launchIn(viewModelScope)
    }

    fun restoreBackup(uri: Uri) {
        _state.value = ImportUiState(running = true)
        importExportRepository.restoreBackup(uri)
            .onEach(::onProgress)
            .launchIn(viewModelScope)
    }

    fun export(format: ExportFormat, destination: Uri, imageIds: List<String>? = null) {
        viewModelScope.launch {
            _state.value = ImportUiState(running = true)
            runCatching {
                importExportRepository.export(ExportRequest(format, imageIds), destination)
            }
            _state.value = ImportUiState(running = false, lastMessage = "Export complete")
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(lastMessage = null)
    }

    private fun onProgress(progress: ImportProgress) {
        _state.value = ImportUiState(
            running = !progress.done,
            progress = progress,
            lastMessage = if (progress.done) {
                "Imported ${progress.imported}, skipped ${progress.skipped}, failed ${progress.failed}"
            } else {
                null
            },
        )
    }
}

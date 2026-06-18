package com.promptgallery.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptgallery.domain.model.AppSettings
import com.promptgallery.domain.repository.ImportExportRepository
import com.promptgallery.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Activity-scoped state: the theme-relevant settings needed before drawing, and
 * handling of images shared into the app from other apps (ACTION_SEND).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val importExportRepository: ImportExportRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** Imports images shared into the app, surfacing a completion message. */
    fun importShared(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importExportRepository.importImages(uris).collect { progress ->
                if (progress.done) {
                    _messages.send("Imported ${progress.imported} shared image(s)")
                }
            }
        }
    }
}

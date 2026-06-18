package com.promptgallery.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptgallery.domain.model.AppSettings
import com.promptgallery.domain.model.GalleryView
import com.promptgallery.domain.model.SortOption
import com.promptgallery.domain.model.ThemeMode
import com.promptgallery.domain.repository.ImageRepository
import com.promptgallery.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val totalImages: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    imageRepository: ImageRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        imageRepository.observeTotalCount(),
    ) { settings, count -> SettingsUiState(settings, count) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setDefaultView(view: GalleryView) = viewModelScope.launch { settingsRepository.setDefaultGalleryView(view) }
    fun setColumns(columns: Int) = viewModelScope.launch { settingsRepository.setGridColumns(columns) }
    fun setSort(sort: SortOption) = viewModelScope.launch { settingsRepository.setDefaultSort(sort) }
    fun setEncryption(enabled: Boolean) = viewModelScope.launch { settingsRepository.setEncryptionEnabled(enabled) }
}

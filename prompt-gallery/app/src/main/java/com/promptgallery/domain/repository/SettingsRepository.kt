package com.promptgallery.domain.repository

import com.promptgallery.domain.model.AppSettings
import com.promptgallery.domain.model.GalleryView
import com.promptgallery.domain.model.SortOption
import com.promptgallery.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setDefaultGalleryView(view: GalleryView)
    suspend fun setGridColumns(columns: Int)
    suspend fun setDefaultSort(sort: SortOption)
    suspend fun setEncryptionEnabled(enabled: Boolean)
}

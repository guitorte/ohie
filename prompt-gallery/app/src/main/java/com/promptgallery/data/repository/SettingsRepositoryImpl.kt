package com.promptgallery.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.promptgallery.data.storage.SecurityManager
import com.promptgallery.domain.model.AppSettings
import com.promptgallery.domain.model.GalleryView
import com.promptgallery.domain.model.SortOption
import com.promptgallery.domain.model.ThemeMode
import com.promptgallery.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager,
) : SettingsRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val VIEW = stringPreferencesKey("gallery_view")
        val COLUMNS = intPreferencesKey("grid_columns")
        val SORT = stringPreferencesKey("default_sort")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC] ?: true,
            defaultGalleryView = prefs[Keys.VIEW]?.let { runCatching { GalleryView.valueOf(it) }.getOrNull() }
                ?: GalleryView.MASONRY,
            gridColumns = prefs[Keys.COLUMNS] ?: 3,
            defaultSort = prefs[Keys.SORT]?.let { runCatching { SortOption.valueOf(it) }.getOrNull() }
                ?: SortOption.IMPORT_DATE_DESC,
            encryptionEnabled = securityManager.isEncryptionEnabled,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC] = enabled }
    }

    override suspend fun setDefaultGalleryView(view: GalleryView) {
        context.dataStore.edit { it[Keys.VIEW] = view.name }
    }

    override suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[Keys.COLUMNS] = columns.coerceIn(2, 6) }
    }

    override suspend fun setDefaultSort(sort: SortOption) {
        context.dataStore.edit { it[Keys.SORT] = sort.name }
    }

    override suspend fun setEncryptionEnabled(enabled: Boolean) {
        // Applied on next launch; toggling re-keys the database via DatabaseModule.
        securityManager.isEncryptionEnabled = enabled
    }
}

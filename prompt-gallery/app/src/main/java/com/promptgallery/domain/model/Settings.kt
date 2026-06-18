package com.promptgallery.domain.model

enum class ThemeMode(val displayName: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** User-facing application preferences. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultGalleryView: GalleryView = GalleryView.MASONRY,
    val gridColumns: Int = 3,
    val defaultSort: SortOption = SortOption.IMPORT_DATE_DESC,
    val encryptionEnabled: Boolean = false,
)

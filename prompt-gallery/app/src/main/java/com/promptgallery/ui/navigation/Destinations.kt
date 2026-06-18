package com.promptgallery.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralised route definitions. Routes are plain strings (Navigation Compose)
 * with helper builders for parameterised destinations so call sites never
 * hand-assemble URLs.
 */
object Routes {
    const val GALLERY = "gallery"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val TEMPLATES = "templates"
    const val SETTINGS = "settings"

    const val DETAIL = "detail/{imageId}"
    fun detail(imageId: String) = "detail/$imageId"

    const val FOLDER = "folder/{folderId}"
    fun folder(folderId: String) = "folder/$folderId"

    const val COLLECTION = "collection/{collectionId}"
    fun collection(collectionId: String) = "collection/$collectionId"

    const val TAG = "tag/{tagId}"
    fun tag(tagId: String) = "tag/$tagId"

    object Args {
        const val IMAGE_ID = "imageId"
        const val FOLDER_ID = "folderId"
        const val COLLECTION_ID = "collectionId"
        const val TAG_ID = "tagId"
    }
}

/** Entries shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    GALLERY(Routes.GALLERY, "Gallery", Icons.Outlined.AutoAwesomeMosaic),
    SEARCH(Routes.SEARCH, "Search", Icons.Outlined.Search),
    LIBRARY(Routes.LIBRARY, "Library", Icons.Outlined.FolderOpen),
    TEMPLATES(Routes.TEMPLATES, "Templates", Icons.Outlined.Description),
}

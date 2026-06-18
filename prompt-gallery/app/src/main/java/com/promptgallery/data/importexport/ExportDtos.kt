package com.promptgallery.data.importexport

import kotlinx.serialization.Serializable

/**
 * Serializable transfer objects for the JSON and backup export formats. These
 * are intentionally decoupled from Room entities so the on-disk format is
 * stable across schema migrations.
 */
@Serializable
data class LibraryExportDto(
    val schemaVersion: Int = 2,
    val exportedAt: Long,
    val app: String = "Prompt Gallery",
    /** True when this package contains only assets changed since a prior backup. */
    val incremental: Boolean = false,
    val images: List<ImageExportDto>,
    val tags: List<TagExportDto> = emptyList(),
    val collections: List<CollectionExportDto> = emptyList(),
    val folders: List<FolderExportDto> = emptyList(),
    val templates: List<TemplateExportDto> = emptyList(),
    val references: List<ReferenceLinkDto> = emptyList(),
    val settings: SettingsExportDto? = null,
)

@Serializable
data class ImageExportDto(
    val id: String,
    val assetType: String = "ARTWORK",
    val fileName: String,
    val title: String,
    val description: String,
    val prompt: String,
    val negativePrompt: String,
    val aiModel: String,
    val aspectRatio: String,
    val width: Int,
    val height: Int,
    val seed: Long?,
    val sampler: String,
    val cfg: Float?,
    val steps: Int?,
    val isFavorite: Boolean,
    val rating: Int,
    val colorLabel: String,
    val customNotes: String,
    val sourceUrl: String,
    val creationDate: Long,
    val importDate: Long,
    val modifiedDate: Long,
    val tags: List<String> = emptyList(),
    val collectionIds: List<String> = emptyList(),
    val folderId: String? = null,
)

@Serializable
data class TagExportDto(val id: String, val name: String)

@Serializable
data class CollectionExportDto(
    val id: String,
    val name: String,
    val description: String,
    val isSmartCollection: Boolean,
    val smartQuery: String?,
)

@Serializable
data class FolderExportDto(val id: String, val name: String, val parentId: String?)

@Serializable
data class TemplateExportDto(
    val id: String,
    val name: String,
    val category: String,
    val promptText: String,
    val negativePromptText: String,
    val variablesJson: String,
    val description: String,
)

/** An artwork↔reference edge, preserved so backlinks survive a restore. */
@Serializable
data class ReferenceLinkDto(
    val artworkId: String,
    val referenceId: String,
    val addedDate: Long,
)

/** User preferences captured in a full backup. */
@Serializable
data class SettingsExportDto(
    val themeMode: String,
    val dynamicColor: Boolean,
    val defaultGalleryView: String,
    val gridColumns: Int,
    val defaultSort: String,
)

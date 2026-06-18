package com.promptgallery.domain.model

import kotlinx.serialization.Serializable

/** A free-form label. */
data class Tag(
    val id: String,
    val name: String,
    val createdDate: Long,
    /** Number of images carrying this tag, when loaded with counts. */
    val imageCount: Int = 0,
)

/** A curated or smart grouping of images. */
data class Collection(
    val id: String,
    val name: String,
    val description: String,
    val coverImageId: String?,
    val isSmartCollection: Boolean,
    val smartQuery: SmartQuery?,
    val sortOrder: Int,
    val createdDate: Long,
    val imageCount: Int = 0,
    val coverThumbnailPath: String? = null,
)

/** A node in the folder hierarchy. */
data class Folder(
    val id: String,
    val name: String,
    val parentId: String?,
    val sortOrder: Int,
    val createdDate: Long,
    val imageCount: Int = 0,
    val childCount: Int = 0,
)

/** A reusable prompt blueprint with fillable variables. */
data class PromptTemplate(
    val id: String,
    val name: String,
    val category: String,
    val promptText: String,
    val negativePromptText: String,
    val variables: List<TemplateVariable>,
    val description: String,
    val useCount: Int,
    val createdDate: Long,
)

/** A placeholder inside a template, e.g. `{subject}`. */
@Serializable
data class TemplateVariable(
    val key: String,
    val label: String,
    val defaultValue: String = "",
    val options: List<String> = emptyList(),
)

/** A historical snapshot of an image's prompt fields. */
data class ImageVersion(
    val id: String,
    val imageId: String,
    val prompt: String,
    val negativePrompt: String,
    val versionNumber: Int,
    val changeNote: String,
    val editedDate: Long,
)

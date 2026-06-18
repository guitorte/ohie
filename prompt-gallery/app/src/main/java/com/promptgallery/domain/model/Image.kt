package com.promptgallery.domain.model

/**
 * Domain representation of a stored image and its prompt metadata. This is the
 * model the UI and use cases work with; it is mapped to/from
 * [com.promptgallery.data.local.entity.ImageEntity] in the repository layer.
 */
data class Image(
    val id: String,
    val filePath: String,
    val fileName: String,
    val thumbnailPath: String,
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
    val colorLabel: ColorLabel,
    val folderId: String?,
    val customNotes: String,
    val sourceUrl: String,
    val creationDate: Long,
    val importDate: Long,
    val modifiedDate: Long,
    val fileSize: Long,
    val mimeType: String,
    val tags: List<Tag> = emptyList(),
    val collectionIds: List<String> = emptyList(),
) {
    /** Width / height ratio used for masonry layout; defaults to square. */
    val aspectRatioValue: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f
}

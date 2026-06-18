package com.promptgallery.data.local.relation

/** Lightweight projections returned by aggregate DAO queries. */

data class TagWithCount(
    val id: String,
    val name: String,
    val createdDate: Long,
    val imageCount: Int,
)

data class CollectionWithCount(
    val id: String,
    val name: String,
    val description: String,
    val coverImageId: String?,
    val isSmartCollection: Boolean,
    val smartQuery: String?,
    val sortOrder: Int,
    val createdDate: Long,
    val imageCount: Int,
    val coverThumbnailPath: String?,
)

data class FolderWithCount(
    val id: String,
    val name: String,
    val parentId: String?,
    val sortOrder: Int,
    val createdDate: Long,
    val imageCount: Int,
    val childCount: Int,
)

/** A single distinct AI model name with how many images use it. */
data class ModelFacet(
    val aiModel: String,
    val count: Int,
)

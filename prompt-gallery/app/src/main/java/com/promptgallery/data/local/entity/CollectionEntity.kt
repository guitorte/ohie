package com.promptgallery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-curated grouping of images. A collection may be "smart": instead of
 * an explicit membership list it stores a [smartQuery] that is evaluated
 * dynamically (e.g. all photorealistic Flux portraits rated >= 4).
 */
@Entity(
    tableName = "collections",
    indices = [Index("isSmartCollection"), Index("sortOrder")],
)
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    /** Image used as the collection cover; null falls back to the newest member. */
    val coverImageId: String? = null,
    val isSmartCollection: Boolean = false,
    /** Serialized [com.promptgallery.domain.model.SmartQuery] when smart. */
    val smartQuery: String? = null,
    val sortOrder: Int = 0,
    val createdDate: Long,
)

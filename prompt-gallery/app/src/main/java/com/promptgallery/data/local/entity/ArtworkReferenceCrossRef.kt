package com.promptgallery.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Self-referential many-to-many link between an artwork and a reference image.
 * Both endpoints are rows in [ImageEntity] (distinguished by `assetType`).
 *
 * Rows where `artworkId = X` are the references attached to artwork X; rows where
 * `referenceId = Y` are the artworks that use reference Y (Obsidian-style
 * backlinks / usage history).
 */
@Entity(
    tableName = "artwork_reference_cross_ref",
    primaryKeys = ["artworkId", "referenceId"],
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["artworkId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["referenceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("artworkId"), Index("referenceId")],
)
data class ArtworkReferenceCrossRef(
    val artworkId: String,
    val referenceId: String,
    val addedDate: Long,
)

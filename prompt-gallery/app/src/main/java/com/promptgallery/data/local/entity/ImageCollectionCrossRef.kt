package com.promptgallery.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Join table for the many-to-many relation between images and collections. */
@Entity(
    tableName = "image_collection_cross_ref",
    primaryKeys = ["imageId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["imageId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collectionId"), Index("imageId")],
)
data class ImageCollectionCrossRef(
    val imageId: String,
    val collectionId: String,
    val addedDate: Long,
)

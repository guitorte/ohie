package com.promptgallery.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Join table implementing the many-to-many relation between images and tags. */
@Entity(
    tableName = "image_tag_cross_ref",
    primaryKeys = ["imageId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["imageId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId"), Index("imageId")],
)
data class ImageTagCrossRef(
    val imageId: String,
    val tagId: String,
)

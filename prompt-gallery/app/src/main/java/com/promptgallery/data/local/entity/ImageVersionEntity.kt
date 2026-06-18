package com.promptgallery.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A historical snapshot of an image's prompt fields. Every time the prompt or
 * negative prompt is edited, the previous value is recorded here so the user
 * can review and restore earlier iterations.
 */
@Entity(
    tableName = "image_versions",
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["imageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("imageId"), Index(value = ["imageId", "versionNumber"])],
)
data class ImageVersionEntity(
    @PrimaryKey
    val id: String,
    val imageId: String,
    val prompt: String,
    val negativePrompt: String,
    val versionNumber: Int,
    val changeNote: String = "",
    val editedDate: Long,
)

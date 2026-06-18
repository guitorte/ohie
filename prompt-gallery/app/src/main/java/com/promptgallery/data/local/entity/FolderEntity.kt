package com.promptgallery.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A hierarchical folder. Nesting is modelled with a self-referencing
 * [parentId]; a null parent denotes a top-level folder. Deleting a parent
 * cascades to its descendants (images are re-parented to null via their own FK).
 */
@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parentId"), Index("sortOrder")],
)
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val createdDate: Long,
)

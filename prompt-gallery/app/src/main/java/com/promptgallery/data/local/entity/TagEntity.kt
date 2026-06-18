package com.promptgallery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A free-form label that can be attached to many images. */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdDate: Long,
)

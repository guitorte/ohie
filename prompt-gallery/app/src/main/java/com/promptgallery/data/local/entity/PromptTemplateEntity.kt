package com.promptgallery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A reusable prompt blueprint. [variablesJson] holds the serialized list of
 * placeholder variables (e.g. {subject}, {style}) so the template can be
 * filled in before being copied for reuse.
 */
@Entity(
    tableName = "prompt_templates",
    indices = [Index("category"), Index("useCount")],
)
data class PromptTemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String = "",
    val promptText: String,
    val negativePromptText: String = "",
    /** Serialized list of [com.promptgallery.domain.model.TemplateVariable]. */
    val variablesJson: String = "[]",
    val description: String = "",
    val useCount: Int = 0,
    val createdDate: Long,
)

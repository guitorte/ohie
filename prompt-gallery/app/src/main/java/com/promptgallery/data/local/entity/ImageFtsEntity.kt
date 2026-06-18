package com.promptgallery.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text-search shadow table over the textual columns of [ImageEntity].
 *
 * Declaring [ImageEntity] as the content entity means Room keeps this FTS index
 * synchronized with the base table and stores no duplicate content, while
 * exposing fast `MATCH` queries for prefix and partial-word search. Typo
 * tolerance is layered on top in the search use case via trigram/Levenshtein
 * reranking (see SearchImagesUseCase).
 */
@Fts4(contentEntity = ImageEntity::class)
@Entity(tableName = "images_fts")
data class ImageFtsEntity(
    val title: String,
    val description: String,
    val prompt: String,
    val negativePrompt: String,
    val customNotes: String,
)

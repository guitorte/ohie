package com.promptgallery.domain.model

/**
 * Discriminates the two kinds of asset stored in the single `images` table.
 * Both share the full feature set (tags, collections, notes, search, import);
 * the type only changes which library surface they appear in and how they relate.
 */
enum class AssetType {
    ARTWORK,
    REFERENCE;

    companion object {
        fun fromName(value: String?): AssetType =
            entries.firstOrNull { it.name == value } ?: ARTWORK
    }
}

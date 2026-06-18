package com.promptgallery.domain.repository

import androidx.paging.PagingData
import com.promptgallery.domain.model.AssetType
import com.promptgallery.domain.model.ColorLabel
import com.promptgallery.domain.model.Image
import com.promptgallery.domain.model.ImageVersion
import com.promptgallery.domain.model.SortOption
import kotlinx.coroutines.flow.Flow

/** Read/write access to the image library. */
interface ImageRepository {

    fun pagedLibrary(
        folderId: String?,
        favoritesOnly: Boolean,
        minRating: Int,
        sort: SortOption,
        assetType: AssetType = AssetType.ARTWORK,
    ): Flow<PagingData<Image>>

    fun pagedCollection(collectionId: String): Flow<PagingData<Image>>

    fun pagedTag(tagId: String): Flow<PagingData<Image>>

    fun observeImage(id: String): Flow<Image?>

    suspend fun getImage(id: String): Image?

    fun observeTotalCount(): Flow<Int>

    fun observeCount(assetType: AssetType): Flow<Int>

    fun observeModels(): Flow<List<String>>

    /** Inserts or updates an image record (binary files handled separately). */
    suspend fun save(image: Image)

    suspend fun delete(ids: List<String>)

    // ---- Single-field edits with version history -------------------------

    suspend fun updatePrompt(id: String, prompt: String, negativePrompt: String, changeNote: String)

    suspend fun setFavorite(ids: List<String>, favorite: Boolean)

    suspend fun setRating(ids: List<String>, rating: Int)

    suspend fun setColorLabel(ids: List<String>, label: ColorLabel)

    suspend fun moveToFolder(ids: List<String>, folderId: String?)

    fun observeVersions(imageId: String): Flow<List<ImageVersion>>

    suspend fun restoreVersion(versionId: String, imageId: String)

    // ---- Reference relationships (Obsidian-style backlinks) --------------

    /** Reference images attached to the given artwork. */
    fun observeReferences(artworkId: String): Flow<List<Image>>

    /** Artworks that use the given reference (usage history / backlinks). */
    fun observeBacklinks(referenceId: String): Flow<List<Image>>

    /** How many artworks currently use the given reference. */
    fun observeUsageCount(referenceId: String): Flow<Int>

    suspend fun attachReferences(artworkId: String, referenceIds: List<String>)

    suspend fun detachReference(artworkId: String, referenceId: String)

    suspend fun referenceIdsFor(artworkId: String): List<String>
}

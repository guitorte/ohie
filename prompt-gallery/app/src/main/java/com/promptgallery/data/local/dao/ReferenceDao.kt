package com.promptgallery.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.promptgallery.data.local.entity.ArtworkReferenceCrossRef
import com.promptgallery.data.local.entity.ImageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Manages the self-referential artwork↔reference relationship and the
 * backlink/usage queries it enables.
 */
@Dao
interface ReferenceDao {

    @Upsert
    suspend fun link(ref: ArtworkReferenceCrossRef)

    @Upsert
    suspend fun linkAll(refs: List<ArtworkReferenceCrossRef>)

    @Query("DELETE FROM artwork_reference_cross_ref WHERE artworkId = :artworkId AND referenceId = :referenceId")
    suspend fun unlink(artworkId: String, referenceId: String)

    /** Reference images attached to an artwork. */
    @Transaction
    @Query(
        """
        SELECT images.* FROM images
        JOIN artwork_reference_cross_ref r ON images.id = r.referenceId
        WHERE r.artworkId = :artworkId
        ORDER BY r.addedDate DESC
        """,
    )
    fun observeReferencesForArtwork(artworkId: String): Flow<List<ImageEntity>>

    /** Artworks that use a reference (Obsidian-style backlinks / usage history). */
    @Transaction
    @Query(
        """
        SELECT images.* FROM images
        JOIN artwork_reference_cross_ref r ON images.id = r.artworkId
        WHERE r.referenceId = :referenceId
        ORDER BY r.addedDate DESC
        """,
    )
    fun observeArtworksForReference(referenceId: String): Flow<List<ImageEntity>>

    @Query("SELECT COUNT(*) FROM artwork_reference_cross_ref WHERE referenceId = :referenceId")
    fun observeUsageCount(referenceId: String): Flow<Int>

    @Query("SELECT referenceId FROM artwork_reference_cross_ref WHERE artworkId = :artworkId")
    suspend fun referenceIdsFor(artworkId: String): List<String>

    @Query("SELECT * FROM artwork_reference_cross_ref")
    suspend fun getAllForExport(): List<ArtworkReferenceCrossRef>
}

package com.promptgallery.domain.repository

import android.net.Uri
import com.promptgallery.domain.model.Image
import kotlinx.coroutines.flow.Flow

/** Progress emitted while a long-running import runs. */
data class ImportProgress(
    val processed: Int,
    val total: Int,
    val currentName: String,
    val imported: Int,
    val skipped: Int,
    val failed: Int,
    val done: Boolean,
)

enum class ExportFormat { ZIP, JSON, CSV, MARKDOWN, BACKUP }

data class ExportRequest(
    val format: ExportFormat,
    val imageIds: List<String>?,
    val includeImages: Boolean = true,
)

interface ImportExportRepository {

    /**
     * Imports the given content [uris] (single images or whole document trees).
     * Copies pixels into app storage, generates thumbnails, extracts embedded
     * generation metadata, and writes [Image] records. Emits progress.
     */
    fun importImages(uris: List<Uri>): Flow<ImportProgress>

    /** Imports every image found by walking a SAF document-tree uri. */
    fun importFolder(treeUri: Uri): Flow<ImportProgress>

    /** Builds an export artifact and returns the destination [Uri] to share. */
    suspend fun export(request: ExportRequest, destination: Uri)

    /** Restores a previously exported backup archive. */
    fun restoreBackup(backupUri: Uri): Flow<ImportProgress>
}

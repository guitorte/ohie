package com.promptgallery.data.importexport

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.promptgallery.core.util.Ids
import com.promptgallery.core.util.IoDispatcher
import com.promptgallery.data.local.dao.CollectionDao
import com.promptgallery.data.local.dao.FolderDao
import com.promptgallery.data.local.dao.ImageDao
import com.promptgallery.data.local.dao.PromptTemplateDao
import com.promptgallery.data.local.dao.TagDao
import com.promptgallery.data.local.entity.ImageCollectionCrossRef
import com.promptgallery.data.local.entity.ImageEntity
import com.promptgallery.data.local.entity.ImageTagCrossRef
import com.promptgallery.data.local.entity.TagEntity
import com.promptgallery.data.storage.FileStorage
import com.promptgallery.data.storage.MetadataExtractor
import com.promptgallery.domain.repository.ExportFormat
import com.promptgallery.domain.repository.ExportRequest
import com.promptgallery.domain.repository.ImportExportRepository
import com.promptgallery.domain.repository.ImportProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ImportExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageDao: ImageDao,
    private val tagDao: TagDao,
    private val collectionDao: CollectionDao,
    private val folderDao: FolderDao,
    private val templateDao: PromptTemplateDao,
    private val fileStorage: FileStorage,
    private val metadataExtractor: MetadataExtractor,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ImportExportRepository {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    // ---- Import ----------------------------------------------------------

    override fun importImages(uris: List<Uri>): Flow<ImportProgress> =
        importInternal(uris).flowOn(dispatcher)

    override fun importFolder(treeUri: Uri): Flow<ImportProgress> = flow {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
        val images = mutableListOf<Uri>()
        if (tree != null) collectImages(tree, images)
        emitAll(importInternal(images))
    }.flowOn(dispatcher)

    private fun importInternal(uris: List<Uri>): Flow<ImportProgress> = flow {
        val total = uris.size
        var imported = 0
        var skipped = 0
        var failed = 0
        if (total == 0) {
            emit(ImportProgress(0, 0, "", 0, 0, 0, done = true))
            return@flow
        }
        uris.forEachIndexed { index, uri ->
            val name = uri.lastPathSegment ?: "image_$index"
            emit(ImportProgress(index, total, name, imported, skipped, failed, done = false))
            try {
                val id = Ids.newId()
                val stored = fileStorage.storeFromUri(uri, id)
                val meta = metadataExtractor.extract(uri)
                val now = System.currentTimeMillis()
                val width = meta.width ?: stored.width
                val height = meta.height ?: stored.height
                val entity = ImageEntity(
                    id = id,
                    filePath = stored.filePath,
                    fileName = stored.fileName,
                    thumbnailPath = stored.thumbnailPath,
                    title = stored.fileName.substringBeforeLast('.'),
                    prompt = meta.prompt,
                    negativePrompt = meta.negativePrompt,
                    aiModel = meta.aiModel,
                    aspectRatio = aspectRatioLabel(width, height),
                    width = width,
                    height = height,
                    seed = meta.seed,
                    sampler = meta.sampler,
                    cfg = meta.cfg,
                    steps = meta.steps,
                    creationDate = now,
                    importDate = now,
                    modifiedDate = now,
                    fileSize = stored.fileSize,
                    mimeType = stored.mimeType,
                )
                imageDao.upsert(entity)
                imported++
            } catch (e: Exception) {
                failed++
            }
        }
        emit(ImportProgress(total, total, "", imported, skipped, failed, done = true))
    }

    private fun collectImages(dir: DocumentFile, out: MutableList<Uri>) {
        dir.listFiles().forEach { file ->
            when {
                file.isDirectory -> collectImages(file, out)
                file.isFile && (file.type?.startsWith("image/") == true) -> out += file.uri
            }
        }
    }

    // ---- Export ----------------------------------------------------------

    override suspend fun export(request: ExportRequest, destination: Uri) = withContext(dispatcher) {
        val entities = if (request.imageIds == null) {
            imageDao.getAllForExport()
        } else {
            imageDao.getByIds(request.imageIds)
        }
        when (request.format) {
            ExportFormat.JSON -> writeText(destination, json.encodeToString(buildLibraryDto(entities)))
            ExportFormat.CSV -> writeText(destination, buildCsv(entities))
            ExportFormat.MARKDOWN -> writeText(destination, buildMarkdown(entities))
            ExportFormat.ZIP -> writeZip(destination, entities, includeImages = request.includeImages)
            ExportFormat.BACKUP -> writeZip(destination, entities, includeImages = true, backup = true)
        }
    }

    private suspend fun buildLibraryDto(entities: List<ImageEntity>): LibraryExportDto {
        val tagsById = tagDao.getAll().associateBy { it.id }
        return LibraryExportDto(
            exportedAt = System.currentTimeMillis(),
            images = entities.map { e ->
                val tagIds = tagDao.getTagIdsForImage(e.id)
                ImageExportDto(
                    id = e.id,
                    fileName = e.fileName,
                    title = e.title,
                    description = e.description,
                    prompt = e.prompt,
                    negativePrompt = e.negativePrompt,
                    aiModel = e.aiModel,
                    aspectRatio = e.aspectRatio,
                    width = e.width,
                    height = e.height,
                    seed = e.seed,
                    sampler = e.sampler,
                    cfg = e.cfg,
                    steps = e.steps,
                    isFavorite = e.isFavorite,
                    rating = e.rating,
                    colorLabel = e.colorLabel,
                    customNotes = e.customNotes,
                    sourceUrl = e.sourceUrl,
                    creationDate = e.creationDate,
                    importDate = e.importDate,
                    modifiedDate = e.modifiedDate,
                    tags = tagIds.mapNotNull { tagsById[it]?.name },
                    collectionIds = collectionDao.getCollectionIdsForImage(e.id),
                    folderId = e.folderId,
                )
            },
            tags = tagDao.getAll().map { TagExportDto(it.id, it.name) },
            collections = collectionDao.getAll().map {
                CollectionExportDto(it.id, it.name, it.description, it.isSmartCollection, it.smartQuery)
            },
            folders = folderDao.getAll().map { FolderExportDto(it.id, it.name, it.parentId) },
            templates = templateDao.getAll().map {
                TemplateExportDto(
                    it.id, it.name, it.category, it.promptText,
                    it.negativePromptText, it.variablesJson, it.description,
                )
            },
        )
    }

    private fun buildCsv(entities: List<ImageEntity>): String = buildString {
        val headers = listOf(
            "id", "fileName", "title", "prompt", "negativePrompt", "aiModel",
            "aspectRatio", "seed", "sampler", "cfg", "steps", "rating",
            "favorite", "colorLabel", "creationDate",
        )
        appendLine(headers.joinToString(",") { csvCell(it) })
        entities.forEach { e ->
            appendLine(
                listOf(
                    e.id, e.fileName, e.title, e.prompt, e.negativePrompt, e.aiModel,
                    e.aspectRatio, e.seed?.toString().orEmpty(), e.sampler,
                    e.cfg?.toString().orEmpty(), e.steps?.toString().orEmpty(),
                    e.rating.toString(), e.isFavorite.toString(), e.colorLabel,
                    e.creationDate.toString(),
                ).joinToString(",") { csvCell(it) },
            )
        }
    }

    private fun buildMarkdown(entities: List<ImageEntity>): String = buildString {
        appendLine("# Prompt Gallery export")
        appendLine()
        appendLine("Exported ${entities.size} image(s).")
        appendLine()
        entities.forEach { e ->
            appendLine("## ${e.title.ifBlank { e.fileName }}")
            appendLine()
            appendLine("- **File:** `${e.fileName}`")
            if (e.aiModel.isNotBlank()) appendLine("- **Model:** ${e.aiModel}")
            if (e.seed != null) appendLine("- **Seed:** ${e.seed}")
            if (e.sampler.isNotBlank()) appendLine("- **Sampler:** ${e.sampler}")
            if (e.cfg != null) appendLine("- **CFG:** ${e.cfg}")
            if (e.steps != null) appendLine("- **Steps:** ${e.steps}")
            if (e.rating > 0) appendLine("- **Rating:** ${"★".repeat(e.rating)}")
            appendLine()
            appendLine("**Prompt**")
            appendLine()
            appendLine("```")
            appendLine(e.prompt)
            appendLine("```")
            if (e.negativePrompt.isNotBlank()) {
                appendLine()
                appendLine("**Negative prompt**")
                appendLine()
                appendLine("```")
                appendLine(e.negativePrompt)
                appendLine("```")
            }
            appendLine()
            appendLine("---")
            appendLine()
        }
    }

    private suspend fun writeZip(
        destination: Uri,
        entities: List<ImageEntity>,
        includeImages: Boolean,
        backup: Boolean = false,
    ) {
        val output = context.contentResolver.openOutputStream(destination)
            ?: error("Cannot open output for $destination")
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            // Library manifest.
            zip.putNextEntry(ZipEntry("library.json"))
            zip.write(json.encodeToString(buildLibraryDto(entities)).toByteArray())
            zip.closeEntry()

            // Human-readable prompts alongside the data.
            zip.putNextEntry(ZipEntry("prompts.md"))
            zip.write(buildMarkdown(entities).toByteArray())
            zip.closeEntry()

            if (includeImages) {
                entities.forEach { e ->
                    val file = File(e.filePath)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry("images/${e.id}_${e.fileName}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            if (backup) {
                // Marker so restore can validate the archive type.
                zip.putNextEntry(ZipEntry("BACKUP"))
                zip.write("prompt-gallery-backup-v1".toByteArray())
                zip.closeEntry()
            }
        }
    }

    private fun writeText(destination: Uri, text: String) {
        context.contentResolver.openOutputStream(destination)?.use { out ->
            OutputStreamWriter(out, Charsets.UTF_8).use { it.write(text) }
        } ?: error("Cannot open output for $destination")
    }

    // ---- Restore ---------------------------------------------------------

    override fun restoreBackup(backupUri: Uri): Flow<ImportProgress> = flow {
        // Stage the archive into cache so we can read it twice (manifest + images).
        val staged = File(fileStorage.exportsDir, "restore_${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(backupUri)?.use { input ->
            staged.outputStream().use { input.copyTo(it) }
        } ?: error("Cannot open backup $backupUri")

        val manifest = readManifest(staged) ?: run {
            emit(ImportProgress(0, 0, "", 0, 0, 1, done = true))
            staged.delete()
            return@flow
        }

        // Recreate tags first so cross-refs resolve.
        val tagIdByName = HashMap<String, String>()
        manifest.tags.forEach { dto ->
            val existing = tagDao.findByName(dto.name)
            val id = existing?.id ?: dto.id
            if (existing == null) tagDao.upsert(TagEntity(id, dto.name, System.currentTimeMillis()))
            tagIdByName[dto.name] = id
        }

        val total = manifest.images.size
        var imported = 0
        var failed = 0
        val imagesById = manifest.images.associateBy { it.id }

        ZipInputStream(staged.inputStream().buffered()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val current = entry
                if (current.name.startsWith("images/")) {
                    val originalId = current.name.removePrefix("images/").substringBefore('_')
                    val dto = imagesById[originalId]
                    if (dto != null) {
                        try {
                            val newId = Ids.newId()
                            val ext = dto.fileName.substringAfterLast('.', "png")
                            val target = fileStorage.imageFile(newId, ext)
                            target.outputStream().use { zip.copyTo(it) }
                            val (w, h) = fileStorage.decodeDimensions(target)
                            fileStorage.writeThumbnail(target, newId)
                            val now = System.currentTimeMillis()
                            imageDao.upsert(
                                ImageEntity(
                                    id = newId,
                                    filePath = target.absolutePath,
                                    fileName = dto.fileName,
                                    thumbnailPath = fileStorage.thumbnailFile(newId).absolutePath,
                                    title = dto.title,
                                    description = dto.description,
                                    prompt = dto.prompt,
                                    negativePrompt = dto.negativePrompt,
                                    aiModel = dto.aiModel,
                                    aspectRatio = dto.aspectRatio.ifBlank { aspectRatioLabel(w, h) },
                                    width = if (dto.width > 0) dto.width else w,
                                    height = if (dto.height > 0) dto.height else h,
                                    seed = dto.seed,
                                    sampler = dto.sampler,
                                    cfg = dto.cfg,
                                    steps = dto.steps,
                                    isFavorite = dto.isFavorite,
                                    rating = dto.rating,
                                    colorLabel = dto.colorLabel,
                                    customNotes = dto.customNotes,
                                    sourceUrl = dto.sourceUrl,
                                    creationDate = dto.creationDate,
                                    importDate = now,
                                    modifiedDate = now,
                                    fileSize = target.length(),
                                ),
                            )
                            dto.tags.forEach { tagName ->
                                tagIdByName[tagName]?.let { tagDao.addCrossRef(ImageTagCrossRef(newId, it)) }
                            }
                            imported++
                            emit(ImportProgress(imported, total, dto.fileName, imported, 0, failed, false))
                        } catch (e: Exception) {
                            failed++
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        staged.delete()
        emit(ImportProgress(total, total, "", imported, 0, failed, done = true))
    }.flowOn(dispatcher)

    private fun readManifest(zipFile: File): LibraryExportDto? = runCatching {
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "library.json") {
                    val text = zip.readBytes().toString(Charsets.UTF_8)
                    return@use json.decodeFromString<LibraryExportDto>(text)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            null
        }
    }.getOrNull()

    // ---- Helpers ---------------------------------------------------------

    private fun csvCell(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }

    private fun aspectRatioLabel(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return ""
        val g = gcd(width, height)
        return "${width / g}:${height / g}"
    }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}

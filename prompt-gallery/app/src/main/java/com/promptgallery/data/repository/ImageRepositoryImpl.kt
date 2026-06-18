package com.promptgallery.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.promptgallery.core.util.Ids
import com.promptgallery.core.util.IoDispatcher
import com.promptgallery.data.local.dao.CollectionDao
import com.promptgallery.data.local.dao.ImageDao
import com.promptgallery.data.local.dao.ImageVersionDao
import com.promptgallery.data.local.dao.TagDao
import com.promptgallery.data.local.entity.ImageVersionEntity
import com.promptgallery.data.storage.FileStorage
import com.promptgallery.domain.model.ColorLabel
import com.promptgallery.domain.model.Image
import com.promptgallery.domain.model.ImageVersion
import com.promptgallery.domain.model.SortOption
import com.promptgallery.domain.repository.ImageRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val imageDao: ImageDao,
    private val tagDao: TagDao,
    private val collectionDao: CollectionDao,
    private val versionDao: ImageVersionDao,
    private val fileStorage: FileStorage,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ImageRepository {

    private fun pagingConfig() = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = PAGE_SIZE,
        initialLoadSize = PAGE_SIZE * 2,
        enablePlaceholders = true,
        maxSize = PAGE_SIZE * 8,
    )

    override fun pagedLibrary(
        folderId: String?,
        favoritesOnly: Boolean,
        minRating: Int,
        sort: SortOption,
    ): Flow<PagingData<Image>> = Pager(pagingConfig()) {
        imageDao.pagingSource(folderId, favoritesOnly, minRating, sort.name)
    }.flow.map { paging -> paging.map { it.toDomain() } }

    override fun pagedCollection(collectionId: String): Flow<PagingData<Image>> =
        Pager(pagingConfig()) { imageDao.pagingSourceForCollection(collectionId) }
            .flow.map { paging -> paging.map { it.toDomain() } }

    override fun pagedTag(tagId: String): Flow<PagingData<Image>> =
        Pager(pagingConfig()) { imageDao.pagingSourceForTag(tagId) }
            .flow.map { paging -> paging.map { it.toDomain() } }

    override fun observeImage(id: String): Flow<Image?> =
        imageDao.observeWithRelations(id).map { it?.toDomain() }

    override suspend fun getImage(id: String): Image? = withContext(dispatcher) {
        val entity = imageDao.getById(id) ?: return@withContext null
        val tags = tagDao.getTagsForImage(id).map { it.toDomain() }
        val collectionIds = collectionDao.getCollectionIdsForImage(id)
        entity.toDomain(tags, collectionIds)
    }

    override fun observeTotalCount(): Flow<Int> = imageDao.observeCount()

    override fun observeModels(): Flow<List<String>> =
        imageDao.observeModelFacets().map { facets -> facets.map { it.aiModel } }

    override suspend fun save(image: Image) = withContext(dispatcher) {
        imageDao.upsert(image.toEntity())
    }

    override suspend fun delete(ids: List<String>) = withContext(dispatcher) {
        if (ids.isEmpty()) return@withContext
        val paths = imageDao.getByIds(ids).flatMap { listOf(it.filePath, it.thumbnailPath) }
        imageDao.deleteByIds(ids) // cascades remove cross-refs and versions
        fileStorage.deleteFiles(paths)
    }

    override suspend fun updatePrompt(
        id: String,
        prompt: String,
        negativePrompt: String,
        changeNote: String,
    ) = withContext(dispatcher) {
        val current = imageDao.getById(id) ?: return@withContext
        if (current.prompt == prompt && current.negativePrompt == negativePrompt) return@withContext
        // Snapshot the previous values before overwriting.
        val nextVersion = versionDao.maxVersionNumber(id) + 1
        versionDao.insert(
            ImageVersionEntity(
                id = Ids.newId(),
                imageId = id,
                prompt = current.prompt,
                negativePrompt = current.negativePrompt,
                versionNumber = nextVersion,
                changeNote = changeNote,
                editedDate = now(),
            ),
        )
        imageDao.updatePrompt(id, prompt, negativePrompt, now())
    }

    override suspend fun setFavorite(ids: List<String>, favorite: Boolean) = withContext(dispatcher) {
        imageDao.setFavorite(ids, favorite, now())
    }

    override suspend fun setRating(ids: List<String>, rating: Int) = withContext(dispatcher) {
        imageDao.setRating(ids, rating.coerceIn(0, 5), now())
    }

    override suspend fun setColorLabel(ids: List<String>, label: ColorLabel) = withContext(dispatcher) {
        imageDao.setColorLabel(ids, label.name, now())
    }

    override suspend fun moveToFolder(ids: List<String>, folderId: String?) = withContext(dispatcher) {
        imageDao.moveToFolder(ids, folderId, now())
    }

    override fun observeVersions(imageId: String): Flow<List<ImageVersion>> =
        versionDao.observeForImage(imageId).map { list -> list.map { it.toDomain() } }

    override suspend fun restoreVersion(versionId: String, imageId: String) = withContext(dispatcher) {
        val version = versionDao.getForImage(imageId).firstOrNull { it.id == versionId }
            ?: return@withContext
        // Restoring is itself an edit, so it records a new version of the current state.
        updatePrompt(
            id = imageId,
            prompt = version.prompt,
            negativePrompt = version.negativePrompt,
            changeNote = "Restored v${version.versionNumber}",
        )
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        const val PAGE_SIZE = 60
    }
}

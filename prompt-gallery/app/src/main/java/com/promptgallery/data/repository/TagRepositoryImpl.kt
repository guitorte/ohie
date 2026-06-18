package com.promptgallery.data.repository

import com.promptgallery.core.util.Ids
import com.promptgallery.core.util.IoDispatcher
import com.promptgallery.data.local.dao.TagDao
import com.promptgallery.data.local.entity.ImageTagCrossRef
import com.promptgallery.data.local.entity.TagEntity
import com.promptgallery.domain.model.Tag
import com.promptgallery.domain.repository.TagRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> =
        tagDao.observeAllWithCounts().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Tag> = withContext(dispatcher) {
        tagDao.getAll().map { it.toDomain() }
    }

    override suspend fun observeTagsForImage(imageId: String): List<Tag> = withContext(dispatcher) {
        tagDao.getTagsForImage(imageId).map { it.toDomain() }
    }

    override suspend fun getOrCreate(name: String): Tag = withContext(dispatcher) {
        val trimmed = name.trim()
        tagDao.findByName(trimmed)?.toDomain() ?: run {
            val entity = TagEntity(Ids.newId(), trimmed, System.currentTimeMillis())
            tagDao.upsert(entity)
            entity.toDomain()
        }
    }

    override suspend fun rename(id: String, name: String) = withContext(dispatcher) {
        val existing = tagDao.getAll().firstOrNull { it.id == id } ?: return@withContext
        tagDao.upsert(existing.copy(name = name.trim()))
    }

    override suspend fun delete(id: String) = withContext(dispatcher) {
        tagDao.deleteById(id)
    }

    override suspend fun setTagsForImage(imageId: String, tagIds: List<String>) = withContext(dispatcher) {
        tagDao.clearTagsForImage(imageId)
        tagDao.addCrossRefs(tagIds.map { ImageTagCrossRef(imageId, it) })
    }

    override suspend fun addTagToImages(tagId: String, imageIds: List<String>) = withContext(dispatcher) {
        tagDao.addCrossRefs(imageIds.map { ImageTagCrossRef(it, tagId) })
    }

    override suspend fun removeTagFromImage(tagId: String, imageId: String) = withContext(dispatcher) {
        tagDao.removeCrossRef(ImageTagCrossRef(imageId, tagId))
    }
}

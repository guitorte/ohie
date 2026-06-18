package com.promptgallery.data.repository

import com.promptgallery.core.util.Ids
import com.promptgallery.core.util.IoDispatcher
import com.promptgallery.data.local.dao.CollectionDao
import com.promptgallery.data.local.entity.CollectionEntity
import com.promptgallery.data.local.entity.ImageCollectionCrossRef
import com.promptgallery.domain.model.Collection
import com.promptgallery.domain.model.SmartQuery
import com.promptgallery.domain.repository.CollectionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : CollectionRepository {

    override fun observeAll(): Flow<List<Collection>> =
        collectionDao.observeAllWithCounts().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Collection? = withContext(dispatcher) {
        collectionDao.getById(id)?.toDomain()
    }

    override suspend fun create(name: String, description: String): Collection = withContext(dispatcher) {
        val entity = CollectionEntity(
            id = Ids.newId(),
            name = name.trim(),
            description = description,
            createdDate = System.currentTimeMillis(),
        )
        collectionDao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun createSmart(
        name: String,
        query: SmartQuery,
        description: String,
    ): Collection = withContext(dispatcher) {
        val entity = CollectionEntity(
            id = Ids.newId(),
            name = name.trim(),
            description = description,
            isSmartCollection = true,
            smartQuery = mapperJson.encodeToString(SmartQuery.serializer(), query),
            createdDate = System.currentTimeMillis(),
        )
        collectionDao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun update(collection: Collection) = withContext(dispatcher) {
        collectionDao.upsert(collection.toEntity())
    }

    override suspend fun delete(id: String) = withContext(dispatcher) {
        collectionDao.deleteById(id)
    }

    override suspend fun addImages(collectionId: String, imageIds: List<String>) = withContext(dispatcher) {
        val now = System.currentTimeMillis()
        collectionDao.addCrossRefs(imageIds.map { ImageCollectionCrossRef(it, collectionId, now) })
    }

    override suspend fun removeImage(collectionId: String, imageId: String) = withContext(dispatcher) {
        collectionDao.removeFromCollection(imageId, collectionId)
    }

    override suspend fun setCollectionsForImage(
        imageId: String,
        collectionIds: List<String>,
    ) = withContext(dispatcher) {
        val current = collectionDao.getCollectionIdsForImage(imageId).toSet()
        val target = collectionIds.toSet()
        val now = System.currentTimeMillis()
        (target - current).forEach { collectionDao.addCrossRef(ImageCollectionCrossRef(imageId, it, now)) }
        (current - target).forEach { collectionDao.removeFromCollection(imageId, it) }
    }
}

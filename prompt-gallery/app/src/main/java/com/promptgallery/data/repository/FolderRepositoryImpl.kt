package com.promptgallery.data.repository

import com.promptgallery.core.util.Ids
import com.promptgallery.core.util.IoDispatcher
import com.promptgallery.data.local.dao.FolderDao
import com.promptgallery.data.local.entity.FolderEntity
import com.promptgallery.domain.model.Folder
import com.promptgallery.domain.repository.FolderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : FolderRepository {

    override fun observeChildren(parentId: String?): Flow<List<Folder>> =
        folderDao.observeChildren(parentId).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Folder>> =
        folderDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Folder? = withContext(dispatcher) {
        folderDao.getById(id)?.toDomain()
    }

    override suspend fun create(name: String, parentId: String?): Folder = withContext(dispatcher) {
        val entity = FolderEntity(
            id = Ids.newId(),
            name = name.trim(),
            parentId = parentId,
            createdDate = System.currentTimeMillis(),
        )
        folderDao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun rename(id: String, name: String) = withContext(dispatcher) {
        val existing = folderDao.getById(id) ?: return@withContext
        folderDao.upsert(existing.copy(name = name.trim()))
    }

    override suspend fun move(id: String, newParentId: String?) = withContext(dispatcher) {
        val existing = folderDao.getById(id) ?: return@withContext
        // Guard against making a folder its own ancestor.
        if (newParentId != null && wouldCreateCycle(id, newParentId)) return@withContext
        folderDao.upsert(existing.copy(parentId = newParentId))
    }

    override suspend fun delete(id: String) = withContext(dispatcher) {
        folderDao.deleteById(id)
    }

    override suspend fun breadcrumb(folderId: String): List<Folder> = withContext(dispatcher) {
        val chain = ArrayDeque<Folder>()
        var current = folderDao.getById(folderId)?.toDomain()
        val guard = HashSet<String>()
        while (current != null && guard.add(current.id)) {
            chain.addFirst(current)
            current = current.parentId?.let { folderDao.getById(it)?.toDomain() }
        }
        chain.toList()
    }

    private suspend fun wouldCreateCycle(movingId: String, targetParentId: String): Boolean {
        var cursor: String? = targetParentId
        val guard = HashSet<String>()
        while (cursor != null && guard.add(cursor)) {
            if (cursor == movingId) return true
            cursor = folderDao.getById(cursor)?.parentId
        }
        return false
    }
}

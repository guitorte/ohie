package com.promptgallery.domain.repository

import com.promptgallery.domain.model.Collection
import com.promptgallery.domain.model.Folder
import com.promptgallery.domain.model.PromptTemplate
import com.promptgallery.domain.model.SmartQuery
import com.promptgallery.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeAll(): Flow<List<Tag>>
    suspend fun getAll(): List<Tag>
    suspend fun observeTagsForImage(imageId: String): List<Tag>

    /** Finds an existing tag by name (case-insensitive) or creates one. */
    suspend fun getOrCreate(name: String): Tag
    suspend fun rename(id: String, name: String)
    suspend fun delete(id: String)

    suspend fun setTagsForImage(imageId: String, tagIds: List<String>)
    suspend fun addTagToImages(tagId: String, imageIds: List<String>)
    suspend fun removeTagFromImage(tagId: String, imageId: String)
}

interface CollectionRepository {
    fun observeAll(): Flow<List<Collection>>
    suspend fun getById(id: String): Collection?
    suspend fun create(name: String, description: String = ""): Collection
    suspend fun createSmart(name: String, query: SmartQuery, description: String = ""): Collection
    suspend fun update(collection: Collection)
    suspend fun delete(id: String)

    suspend fun addImages(collectionId: String, imageIds: List<String>)
    suspend fun removeImage(collectionId: String, imageId: String)
    suspend fun setCollectionsForImage(imageId: String, collectionIds: List<String>)
}

interface FolderRepository {
    fun observeChildren(parentId: String?): Flow<List<Folder>>
    fun observeAll(): Flow<List<Folder>>
    suspend fun getById(id: String): Folder?
    suspend fun create(name: String, parentId: String?): Folder
    suspend fun rename(id: String, name: String)
    suspend fun move(id: String, newParentId: String?)
    suspend fun delete(id: String)
    /** Full ancestor chain from root to the given folder, for breadcrumbs. */
    suspend fun breadcrumb(folderId: String): List<Folder>
}

interface TemplateRepository {
    fun observeAll(): Flow<List<PromptTemplate>>
    fun observeMostUsed(limit: Int): Flow<List<PromptTemplate>>
    suspend fun getById(id: String): PromptTemplate?
    suspend fun save(template: PromptTemplate)
    suspend fun delete(id: String)
    suspend fun markUsed(id: String)
    /** Seeds the starter template library on first launch. */
    suspend fun ensureSeeded()
}

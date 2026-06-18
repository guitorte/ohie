package com.promptgallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.promptgallery.data.local.dao.CollectionDao
import com.promptgallery.data.local.dao.FolderDao
import com.promptgallery.data.local.dao.ImageDao
import com.promptgallery.data.local.dao.ImageVersionDao
import com.promptgallery.data.local.dao.PromptTemplateDao
import com.promptgallery.data.local.dao.SearchDao
import com.promptgallery.data.local.dao.TagDao
import com.promptgallery.data.local.entity.CollectionEntity
import com.promptgallery.data.local.entity.FolderEntity
import com.promptgallery.data.local.entity.ImageCollectionCrossRef
import com.promptgallery.data.local.entity.ImageEntity
import com.promptgallery.data.local.entity.ImageFtsEntity
import com.promptgallery.data.local.entity.ImageTagCrossRef
import com.promptgallery.data.local.entity.ImageVersionEntity
import com.promptgallery.data.local.entity.PromptTemplateEntity
import com.promptgallery.data.local.entity.TagEntity

@Database(
    entities = [
        ImageEntity::class,
        ImageFtsEntity::class,
        TagEntity::class,
        ImageTagCrossRef::class,
        CollectionEntity::class,
        ImageCollectionCrossRef::class,
        FolderEntity::class,
        PromptTemplateEntity::class,
        ImageVersionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PromptGalleryDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun tagDao(): TagDao
    abstract fun collectionDao(): CollectionDao
    abstract fun folderDao(): FolderDao
    abstract fun promptTemplateDao(): PromptTemplateDao
    abstract fun imageVersionDao(): ImageVersionDao
    abstract fun searchDao(): SearchDao

    companion object {
        const val DATABASE_NAME = "prompt_gallery.db"
    }
}

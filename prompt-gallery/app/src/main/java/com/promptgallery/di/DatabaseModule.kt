package com.promptgallery.di

import android.content.Context
import androidx.room.Room
import com.promptgallery.data.local.Migrations
import com.promptgallery.data.local.PromptGalleryDatabase
import com.promptgallery.data.local.dao.CollectionDao
import com.promptgallery.data.local.dao.FolderDao
import com.promptgallery.data.local.dao.ImageDao
import com.promptgallery.data.local.dao.ImageVersionDao
import com.promptgallery.data.local.dao.PromptTemplateDao
import com.promptgallery.data.local.dao.ReferenceDao
import com.promptgallery.data.local.dao.SearchDao
import com.promptgallery.data.local.dao.TagDao
import com.promptgallery.data.storage.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager,
    ): PromptGalleryDatabase {
        val builder = Room.databaseBuilder(
            context,
            PromptGalleryDatabase::class.java,
            PromptGalleryDatabase.DATABASE_NAME,
        )

        // When the user has opted in, open the database through SQLCipher using
        // the Keystore-protected passphrase. Otherwise use the default factory.
        if (securityManager.isEncryptionEnabled) {
            System.loadLibrary("sqlcipher")
            builder.openHelperFactory(SupportOpenHelperFactory(securityManager.databasePassphrase()))
        }

        return builder
            .addMigrations(*Migrations.ALL)
            .setQueryExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides fun provideImageDao(db: PromptGalleryDatabase): ImageDao = db.imageDao()
    @Provides fun provideTagDao(db: PromptGalleryDatabase): TagDao = db.tagDao()
    @Provides fun provideCollectionDao(db: PromptGalleryDatabase): CollectionDao = db.collectionDao()
    @Provides fun provideFolderDao(db: PromptGalleryDatabase): FolderDao = db.folderDao()
    @Provides fun provideTemplateDao(db: PromptGalleryDatabase): PromptTemplateDao = db.promptTemplateDao()
    @Provides fun provideVersionDao(db: PromptGalleryDatabase): ImageVersionDao = db.imageVersionDao()
    @Provides fun provideSearchDao(db: PromptGalleryDatabase): SearchDao = db.searchDao()
    @Provides fun provideReferenceDao(db: PromptGalleryDatabase): ReferenceDao = db.referenceDao()
}

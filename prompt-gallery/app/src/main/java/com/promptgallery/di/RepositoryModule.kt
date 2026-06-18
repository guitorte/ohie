package com.promptgallery.di

import com.promptgallery.data.importexport.ImportExportRepositoryImpl
import com.promptgallery.data.repository.CollectionRepositoryImpl
import com.promptgallery.data.repository.FolderRepositoryImpl
import com.promptgallery.data.repository.ImageRepositoryImpl
import com.promptgallery.data.repository.SearchRepositoryImpl
import com.promptgallery.data.repository.SettingsRepositoryImpl
import com.promptgallery.data.repository.TagRepositoryImpl
import com.promptgallery.data.repository.TemplateRepositoryImpl
import com.promptgallery.domain.repository.CollectionRepository
import com.promptgallery.domain.repository.FolderRepository
import com.promptgallery.domain.repository.ImageRepository
import com.promptgallery.domain.repository.ImportExportRepository
import com.promptgallery.domain.repository.SearchRepository
import com.promptgallery.domain.repository.SettingsRepository
import com.promptgallery.domain.repository.TagRepository
import com.promptgallery.domain.repository.TemplateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindImageRepository(impl: ImageRepositoryImpl): ImageRepository

    @Binds @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository

    @Binds @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository

    @Binds @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds @Singleton
    abstract fun bindImportExportRepository(impl: ImportExportRepositoryImpl): ImportExportRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

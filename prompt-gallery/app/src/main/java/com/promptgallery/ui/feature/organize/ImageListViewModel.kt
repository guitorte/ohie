package com.promptgallery.ui.feature.organize

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.promptgallery.domain.model.Image
import com.promptgallery.domain.model.SortOption
import com.promptgallery.domain.repository.CollectionRepository
import com.promptgallery.domain.repository.FolderRepository
import com.promptgallery.domain.repository.ImageRepository
import com.promptgallery.domain.repository.TagRepository
import com.promptgallery.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The source the list is scoped to. */
sealed interface ImageListSource {
    data class FolderSource(val id: String) : ImageListSource
    data class CollectionSource(val id: String) : ImageListSource
    data class TagSource(val id: String) : ImageListSource
}

@HiltViewModel
class ImageListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val imageRepository: ImageRepository,
    folderRepository: FolderRepository,
    collectionRepository: CollectionRepository,
    tagRepository: TagRepository,
) : ViewModel() {

    private val source: ImageListSource = when {
        savedStateHandle.contains(Routes.Args.FOLDER_ID) ->
            ImageListSource.FolderSource(savedStateHandle[Routes.Args.FOLDER_ID]!!)
        savedStateHandle.contains(Routes.Args.COLLECTION_ID) ->
            ImageListSource.CollectionSource(savedStateHandle[Routes.Args.COLLECTION_ID]!!)
        else ->
            ImageListSource.TagSource(savedStateHandle[Routes.Args.TAG_ID]!!)
    }

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    val images: Flow<PagingData<Image>> = when (val s = source) {
        is ImageListSource.FolderSource ->
            imageRepository.pagedLibrary(s.id, false, 0, SortOption.IMPORT_DATE_DESC)
        is ImageListSource.CollectionSource -> imageRepository.pagedCollection(s.id)
        is ImageListSource.TagSource -> imageRepository.pagedTag(s.id)
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            _title.value = when (val s = source) {
                is ImageListSource.FolderSource -> folderRepository.getById(s.id)?.name ?: "Folder"
                is ImageListSource.CollectionSource -> collectionRepository.getById(s.id)?.name ?: "Collection"
                is ImageListSource.TagSource ->
                    tagRepository.getAll().firstOrNull { it.id == s.id }?.name?.let { "#$it" } ?: "Tag"
            }
        }
    }
}

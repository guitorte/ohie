package com.promptgallery.ui.feature.organize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptgallery.domain.model.Collection
import com.promptgallery.domain.model.Folder
import com.promptgallery.domain.model.SearchFilters
import com.promptgallery.domain.model.SmartQuery
import com.promptgallery.domain.model.Tag
import com.promptgallery.domain.repository.CollectionRepository
import com.promptgallery.domain.repository.FolderRepository
import com.promptgallery.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val collections: List<Collection> = emptyList(),
    val rootFolders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val folderRepository: FolderRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = combine(
        collectionRepository.observeAll(),
        folderRepository.observeChildren(null),
        tagRepository.observeAll(),
    ) { collections, folders, tags ->
        LibraryUiState(collections, folders, tags)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { collectionRepository.create(name.trim()) }
    }

    fun createSmartCollection(name: String, minRating: Int, favoritesOnly: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            collectionRepository.createSmart(
                name = name.trim(),
                query = SmartQuery(SearchFilters(minRating = minRating, favoritesOnly = favoritesOnly)),
            )
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { folderRepository.create(name.trim(), parentId = null) }
    }

    fun deleteCollection(id: String) = viewModelScope.launch { collectionRepository.delete(id) }

    fun deleteFolder(id: String) = viewModelScope.launch { folderRepository.delete(id) }

    fun deleteTag(id: String) = viewModelScope.launch { tagRepository.delete(id) }
}

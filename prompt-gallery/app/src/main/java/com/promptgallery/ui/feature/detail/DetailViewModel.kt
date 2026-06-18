package com.promptgallery.ui.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptgallery.core.util.Ids
import com.promptgallery.domain.model.Collection
import com.promptgallery.domain.model.ColorLabel
import com.promptgallery.domain.model.Image
import com.promptgallery.domain.model.ImageVersion
import com.promptgallery.domain.model.Tag
import com.promptgallery.domain.repository.CollectionRepository
import com.promptgallery.domain.repository.ImageRepository
import com.promptgallery.domain.repository.TagRepository
import com.promptgallery.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val image: Image? = null,
    val versions: List<ImageVersion> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val allCollections: List<Collection> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val imageRepository: ImageRepository,
    private val tagRepository: TagRepository,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    private val imageId: String = checkNotNull(savedStateHandle[Routes.Args.IMAGE_ID])

    val uiState: StateFlow<DetailUiState> = combine(
        imageRepository.observeImage(imageId),
        imageRepository.observeVersions(imageId),
        tagRepository.observeAll(),
        collectionRepository.observeAll(),
    ) { image, versions, tags, collections ->
        DetailUiState(
            image = image,
            versions = versions,
            allTags = tags,
            allCollections = collections,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun toggleFavorite() = launch { image ->
        imageRepository.setFavorite(listOf(image.id), !image.isFavorite)
    }

    fun setRating(rating: Int) = launch { image -> imageRepository.setRating(listOf(image.id), rating) }

    fun setColorLabel(label: ColorLabel) = launch { image ->
        imageRepository.setColorLabel(listOf(image.id), label)
    }

    fun updatePrompt(prompt: String, negativePrompt: String) = launch { image ->
        imageRepository.updatePrompt(image.id, prompt, negativePrompt, changeNote = "Edited")
    }

    fun restoreVersion(versionId: String) = launch { image ->
        imageRepository.restoreVersion(versionId, image.id)
    }

    fun setTags(tagNames: List<String>) = launch { image ->
        val ids = tagNames.map { tagRepository.getOrCreate(it).id }
        tagRepository.setTagsForImage(image.id, ids)
    }

    fun setCollections(collectionIds: List<String>) = launch { image ->
        collectionRepository.setCollectionsForImage(image.id, collectionIds)
    }

    fun delete(onDeleted: () -> Unit) = launch { image ->
        imageRepository.delete(listOf(image.id))
        onDeleted()
    }

    /** Creates an independent copy of the record pointing at the same file. */
    fun duplicate(onDuplicated: (String) -> Unit) = launch { image ->
        val now = System.currentTimeMillis()
        val copy = image.copy(
            id = Ids.newId(),
            title = "${image.title} (copy)",
            importDate = now,
            modifiedDate = now,
            isFavorite = false,
            tags = emptyList(),
            collectionIds = emptyList(),
        )
        imageRepository.save(copy)
        onDuplicated(copy.id)
    }

    private inline fun launch(crossinline block: suspend (Image) -> Unit) {
        val image = uiState.value.image ?: return
        viewModelScope.launch { block(image) }
    }
}

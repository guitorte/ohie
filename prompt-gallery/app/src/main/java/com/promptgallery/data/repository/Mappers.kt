package com.promptgallery.data.repository

import com.promptgallery.data.local.entity.CollectionEntity
import com.promptgallery.data.local.entity.FolderEntity
import com.promptgallery.data.local.entity.ImageEntity
import com.promptgallery.data.local.entity.ImageVersionEntity
import com.promptgallery.data.local.entity.PromptTemplateEntity
import com.promptgallery.data.local.entity.TagEntity
import com.promptgallery.data.local.relation.CollectionWithCount
import com.promptgallery.data.local.relation.FolderWithCount
import com.promptgallery.data.local.relation.ImageWithRelations
import com.promptgallery.data.local.relation.TagWithCount
import com.promptgallery.domain.model.Collection
import com.promptgallery.domain.model.ColorLabel
import com.promptgallery.domain.model.Folder
import com.promptgallery.domain.model.Image
import com.promptgallery.domain.model.ImageVersion
import com.promptgallery.domain.model.PromptTemplate
import com.promptgallery.domain.model.SmartQuery
import com.promptgallery.domain.model.Tag
import com.promptgallery.domain.model.TemplateVariable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Pure mapping functions between Room entities and domain models. Keeping these
 * in one place makes the data/domain boundary explicit and easy to test.
 */
internal val mapperJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// ---- Image ---------------------------------------------------------------

fun ImageEntity.toDomain(
    tags: List<Tag> = emptyList(),
    collectionIds: List<String> = emptyList(),
): Image = Image(
    id = id,
    filePath = filePath,
    fileName = fileName,
    thumbnailPath = thumbnailPath,
    title = title,
    description = description,
    prompt = prompt,
    negativePrompt = negativePrompt,
    aiModel = aiModel,
    aspectRatio = aspectRatio,
    width = width,
    height = height,
    seed = seed,
    sampler = sampler,
    cfg = cfg,
    steps = steps,
    isFavorite = isFavorite,
    rating = rating,
    colorLabel = ColorLabel.fromName(colorLabel),
    folderId = folderId,
    customNotes = customNotes,
    sourceUrl = sourceUrl,
    creationDate = creationDate,
    importDate = importDate,
    modifiedDate = modifiedDate,
    fileSize = fileSize,
    mimeType = mimeType,
    tags = tags,
    collectionIds = collectionIds,
)

fun ImageWithRelations.toDomain(): Image = image.toDomain(
    tags = tags.map { it.toDomain() },
    collectionIds = collections.map { it.id },
)

fun Image.toEntity(): ImageEntity = ImageEntity(
    id = id,
    filePath = filePath,
    fileName = fileName,
    thumbnailPath = thumbnailPath,
    title = title,
    description = description,
    prompt = prompt,
    negativePrompt = negativePrompt,
    aiModel = aiModel,
    aspectRatio = aspectRatio,
    width = width,
    height = height,
    seed = seed,
    sampler = sampler,
    cfg = cfg,
    steps = steps,
    isFavorite = isFavorite,
    rating = rating,
    colorLabel = colorLabel.name,
    folderId = folderId,
    customNotes = customNotes,
    sourceUrl = sourceUrl,
    creationDate = creationDate,
    importDate = importDate,
    modifiedDate = modifiedDate,
    fileSize = fileSize,
    mimeType = mimeType,
)

// ---- Tag -----------------------------------------------------------------

fun TagEntity.toDomain(): Tag = Tag(id, name, createdDate)

fun TagWithCount.toDomain(): Tag = Tag(id, name, createdDate, imageCount)

fun Tag.toEntity(): TagEntity = TagEntity(id, name, createdDate)

// ---- Collection ----------------------------------------------------------

fun CollectionEntity.toDomain(imageCount: Int = 0, coverThumbnailPath: String? = null): Collection =
    Collection(
        id = id,
        name = name,
        description = description,
        coverImageId = coverImageId,
        isSmartCollection = isSmartCollection,
        smartQuery = smartQuery?.let { runCatching { mapperJson.decodeFromString<SmartQuery>(it) }.getOrNull() },
        sortOrder = sortOrder,
        createdDate = createdDate,
        imageCount = imageCount,
        coverThumbnailPath = coverThumbnailPath,
    )

fun CollectionWithCount.toDomain(): Collection = Collection(
    id = id,
    name = name,
    description = description,
    coverImageId = coverImageId,
    isSmartCollection = isSmartCollection,
    smartQuery = smartQuery?.let { runCatching { mapperJson.decodeFromString<SmartQuery>(it) }.getOrNull() },
    sortOrder = sortOrder,
    createdDate = createdDate,
    imageCount = imageCount,
    coverThumbnailPath = coverThumbnailPath,
)

fun Collection.toEntity(): CollectionEntity = CollectionEntity(
    id = id,
    name = name,
    description = description,
    coverImageId = coverImageId,
    isSmartCollection = isSmartCollection,
    smartQuery = smartQuery?.let { mapperJson.encodeToString(SmartQuery.serializer(), it) },
    sortOrder = sortOrder,
    createdDate = createdDate,
)

// ---- Folder --------------------------------------------------------------

fun FolderEntity.toDomain(): Folder = Folder(id, name, parentId, sortOrder, createdDate)

fun FolderWithCount.toDomain(): Folder =
    Folder(id, name, parentId, sortOrder, createdDate, imageCount, childCount)

fun Folder.toEntity(): FolderEntity = FolderEntity(id, name, parentId, sortOrder, createdDate)

// ---- Prompt template -----------------------------------------------------

private val templateVariableListSerializer = ListSerializer(TemplateVariable.serializer())

fun PromptTemplateEntity.toDomain(): PromptTemplate = PromptTemplate(
    id = id,
    name = name,
    category = category,
    promptText = promptText,
    negativePromptText = negativePromptText,
    variables = runCatching {
        mapperJson.decodeFromString(templateVariableListSerializer, variablesJson)
    }.getOrDefault(emptyList()),
    description = description,
    useCount = useCount,
    createdDate = createdDate,
)

fun PromptTemplate.toEntity(): PromptTemplateEntity = PromptTemplateEntity(
    id = id,
    name = name,
    category = category,
    promptText = promptText,
    negativePromptText = negativePromptText,
    variablesJson = mapperJson.encodeToString(templateVariableListSerializer, variables),
    description = description,
    useCount = useCount,
    createdDate = createdDate,
)

// ---- Image version -------------------------------------------------------

fun ImageVersionEntity.toDomain(): ImageVersion =
    ImageVersion(id, imageId, prompt, negativePrompt, versionNumber, changeNote, editedDate)

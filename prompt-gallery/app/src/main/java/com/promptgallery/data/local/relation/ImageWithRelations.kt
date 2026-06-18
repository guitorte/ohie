package com.promptgallery.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.promptgallery.data.local.entity.CollectionEntity
import com.promptgallery.data.local.entity.ImageCollectionCrossRef
import com.promptgallery.data.local.entity.ImageEntity
import com.promptgallery.data.local.entity.ImageTagCrossRef
import com.promptgallery.data.local.entity.TagEntity

/**
 * An image together with its tags and the collections it belongs to. Room
 * populates the related lists with separate queries, so this is convenient for
 * detail screens but should be avoided for large paged lists.
 */
data class ImageWithRelations(
    @Embedded
    val image: ImageEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ImageTagCrossRef::class,
            parentColumn = "imageId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ImageCollectionCrossRef::class,
            parentColumn = "imageId",
            entityColumn = "collectionId",
        ),
    )
    val collections: List<CollectionEntity>,
)

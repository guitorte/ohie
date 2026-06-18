package com.promptgallery.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations. Each migration is written to produce exactly the DDL Room
 * generates for the target entities, so the post-migration validation passes.
 */
object Migrations {

    /**
     * v1 → v2: introduces reference images.
     *  - adds the `assetType` discriminator to `images` (existing rows = ARTWORK)
     *  - adds the self-referential artwork↔reference join table + indices
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `images` ADD COLUMN `assetType` TEXT NOT NULL DEFAULT 'ARTWORK'",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_images_assetType` ON `images` (`assetType`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `artwork_reference_cross_ref` (
                    `artworkId` TEXT NOT NULL,
                    `referenceId` TEXT NOT NULL,
                    `addedDate` INTEGER NOT NULL,
                    PRIMARY KEY(`artworkId`, `referenceId`),
                    FOREIGN KEY(`artworkId`) REFERENCES `images`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                    FOREIGN KEY(`referenceId`) REFERENCES `images`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_artwork_reference_cross_ref_artworkId` " +
                    "ON `artwork_reference_cross_ref` (`artworkId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_artwork_reference_cross_ref_referenceId` " +
                    "ON `artwork_reference_cross_ref` (`referenceId`)",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}

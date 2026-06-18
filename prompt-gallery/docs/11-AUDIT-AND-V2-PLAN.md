# Audit & v2 Plan — Asset Management, References, Backup

## 1. Audit of the current build (v1)

### Data model that exists
`images, tags, image_tag_cross_ref, collections, image_collection_cross_ref,
folders (self-nested), prompt_templates, image_versions, images_fts`.

### Capabilities present in data/repository but **not fully reachable in the UI**
| Capability | Data/Repo | UI status (v1) | Action |
| --- | --- | --- | --- |
| Move image → folder | `ImageRepository.moveToFolder` / `GalleryViewModel.bulkMoveToFolder` | No folder picker UI | **Add** bulk folder picker |
| Bulk add → collection | `bulkAddToCollection` | No collection picker UI | **Add** bulk collection picker |
| Rename folder | `FolderRepository.rename` | Not exposed | **Add** folder context menu |
| Delete folder | `FolderRepository.delete` | Not exposed | **Add** |
| Move folder / nest | `FolderRepository.move` (+cycle guard) | Not exposed | **Add** |
| Merge folders | — | — | **New** repo + UI |
| Edit / delete collection | `update` / `delete` | Only create | **Add** |
| Remove image from collection | `removeImage` | Not exposed | **Add** |
| Select all | — | Long-press select only | **Add** |
| Reference images | — | — | **New** (headline feature) |

### Conclusion
The organizational engine is largely built; v1 mostly lacked the **management
surfaces**. v2 adds the Reference subsystem, completes those surfaces, and makes
backup lossless.

## 2. Architecture decision — References as discriminated assets

A reference image needs tags, collections, notes, search, thumbnails and import
— all of which `ImageEntity` already provides. Rather than duplicate that into a
parallel table, a **single `images` table with an `assetType` discriminator**
(`ARTWORK` | `REFERENCE`) is used. This makes references genuinely first-class
(they reuse every existing subsystem) while minimizing new code.

The artwork↔reference link is a self-referential many-to-many join:

```
artwork_reference_cross_ref(artworkId → images.id, referenceId → images.id, addedDate)
```

This yields Obsidian-style backlinks for free:
- references of an artwork  = rows where `artworkId = ?`
- backlinks of a reference  = rows where `referenceId = ?` (usage list + count)

It also lays groundwork for the future asset graph (similarity, clustering,
visual search) since every node is one `images` row and edges are typed join rows.

## 3. Schema changes (v1 → v2, `version = 2`)

```sql
ALTER TABLE images ADD COLUMN assetType TEXT NOT NULL DEFAULT 'ARTWORK';
CREATE INDEX IF NOT EXISTS index_images_assetType ON images(assetType);

CREATE TABLE artwork_reference_cross_ref (
  artworkId TEXT NOT NULL,
  referenceId TEXT NOT NULL,
  addedDate INTEGER NOT NULL,
  PRIMARY KEY(artworkId, referenceId),
  FOREIGN KEY(artworkId)  REFERENCES images(id) ON DELETE CASCADE,
  FOREIGN KEY(referenceId) REFERENCES images(id) ON DELETE CASCADE);
CREATE INDEX index_artwork_reference_cross_ref_artworkId  ON artwork_reference_cross_ref(artworkId);
CREATE INDEX index_artwork_reference_cross_ref_referenceId ON artwork_reference_cross_ref(referenceId);
```

`MIGRATION_1_2` performs exactly the above; existing rows default to `ARTWORK`,
so all prior data is preserved with no reimport.

## 4. Backup format (lossless, referentially intact)

A single **ZIP** package:

```
prompt-gallery-backup.zip
├── library.json        # manifest v2 (schemaVersion 2)
├── prompts.md          # human-readable
└── images/<id>_<name>  # full-res binaries (thumbnails regenerated on restore)
```

`library.json` (manifest v2) preserves: images (incl. assetType, folderId,
ratings, favorites, color labels, all metadata), tags + memberships,
collections + memberships, folders (with parentId), prompt templates,
**artwork↔reference links**, and **user settings**.

Restore preserves original IDs so every relationship (folders, collections,
references) reconnects exactly — no broken links. Incremental backup includes
only assets whose `modifiedDate`/`importDate` is newer than a chosen timestamp,
written into the same package shape so it merges cleanly on restore.

## 5. Components regenerated (only what must change)
Entities (`ImageEntity` +col, new `ArtworkReferenceCrossRef`), `Migrations`,
`PromptGalleryDatabase` (v2), `DatabaseModule` (register migration), `ReferenceDao`,
`ImageDao`/`SearchDao` (assetType filter), mappers, `ImageRepository`(+impl),
`ImportExportRepository`(+impl, manifest v2 + incremental), new Reference UI +
detail backlinks, folder/collection management dialogs, bulk pickers, navigation.
Unrelated code (templates, theme, search-ranking core, security) is untouched.

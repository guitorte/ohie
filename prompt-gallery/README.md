# Prompt Gallery

A production-quality, **offline-first Android app** for AI artists to store, organize,
browse, search, and reuse AI-generated images together with the prompts that created them.

Think *Google Photos × Pinterest × Obsidian × Lightroom × prompt manager* — with a
clean, creative-tool aesthetic rather than a social-media one. Everything lives on the
device: no account, no mandatory cloud, the user owns all data.

---

## Highlights

- **Visual-first gallery** with Masonry, Grid, Timeline and Favorites layouts, backed by
  Paging 3 so it stays smooth past 10,000+ images.
- **Signature prompt card** under every image: expand/collapse, **one-tap copy** of the
  prompt and negative prompt, plus edit / duplicate / share.
- **Typo-tolerant local search** — FTS4 for instant prefix/partial matches, layered with
  trigram + Levenshtein reranking so "portriat" still finds *portrait*. Searches prompt,
  negative prompt, tags, title, notes, model and collections, with faceted filters.
- **Organization**: nested folders, collections, smart collections, tags, color labels,
  star ratings, favorites.
- **Bulk operations**: multi-select to tag, move, favorite, rate, color-label, add to a
  collection, or delete.
- **Import**: single/multiple images, whole folders (SAF document tree), and images
  shared from other apps. Embedded generation metadata (Automatic1111 / ComfyUI PNG text
  chunks and EXIF) is parsed automatically.
- **Export / backup**: JSON, CSV, Markdown, ZIP, and a restorable backup archive.
- **Prompt templates** with fillable `{variables}` and a curated starter library.
- **Version history** for prompt edits, with restore.
- **Premium Material 3 UI**: dark/light, dynamic color, edge-to-edge, smooth motion.
- **Optional at-rest encryption** of the database via SQLCipher, keyed by an
  Android Keystore–protected passphrase.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Room (+FTS4) · Coroutines/Flow · MVVM +
Repository pattern · Hilt · Navigation Compose · Paging 3 · Coil · DataStore ·
kotlinx.serialization.

## Architecture

A single `:app` module organized by layer:

```
com.promptgallery
├── app          Application, MainActivity, theme bootstrap, share-intent handling
├── core.util    dispatchers, ids, FTS query builder, fuzzy text matching
├── data
│   ├── local    Room entities, DAOs, relations, database
│   ├── repository  repository implementations + entity↔domain mappers
│   ├── storage  file/thumbnail storage, metadata extraction, security
│   └── importexport  import/export/backup engine + DTOs
├── domain       models, repository interfaces, use cases
├── di           Hilt modules (Database, Repository, Dispatcher)
└── ui
    ├── theme · navigation · components
    └── feature  gallery · detail · search · organize · templates · importflow · settings
```

Data flows one way: **UI → ViewModel (UiState/StateFlow) → UseCase/Repository → DAO**.
See [`docs/04-ARCHITECTURE.md`](docs/04-ARCHITECTURE.md).

## Documentation

| Doc | Contents |
| --- | --- |
| [01 Product Requirements](docs/01-PRODUCT-REQUIREMENTS.md) | Vision, personas, features, user stories, metrics |
| [02 UX Specification](docs/02-UX-SPECIFICATION.md) | Principles, interactions, gestures, flows, a11y |
| [03 UI Flows & Wireframes](docs/03-UI-FLOWS-AND-WIREFRAMES.md) | Flow diagrams and ASCII wireframes |
| [04 Architecture](docs/04-ARCHITECTURE.md) | Layers, MVVM, DI graph, paging, navigation |
| [05 Data Model & Schema](docs/05-DATA-MODEL-AND-SCHEMA.md) | ERD and full relational schema |
| [06 Search Architecture](docs/06-SEARCH-ARCHITECTURE.md) | FTS pipeline, ranking, typo tolerance |
| [07 Import/Export Architecture](docs/07-IMPORT-EXPORT-ARCHITECTURE.md) | SAF flows, metadata parsing, formats |
| [08 Implementation Plan](docs/08-IMPLEMENTATION-PLAN.md) | Phased roadmap, file inventory, dependencies |
| [09 Folder Structure](docs/09-FOLDER-STRUCTURE.md) | Annotated project tree |
| [10 Future Roadmap & Local AI](docs/10-FUTURE-ROADMAP-AND-LOCAL-AI.md) | Scaling, on-device ML modules |

## Building

Requires the Android SDK (compileSdk 35) and JDK 17+.

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew test               # JVM unit tests (search/text-matching/templates)
./gradlew installDebug       # install on a connected device/emulator
```

`minSdk` 26, `targetSdk` 35. No `INTERNET` permission is requested — the app is fully
offline and accesses media only through the Storage Access Framework.

## Testing

JVM unit tests cover the pure logic that powers search and templates
(`app/src/test`): edit-distance/trigram similarity, FTS query building, and template
variable substitution. The data layer is structured (interfaces + injected dispatchers)
for straightforward Room/instrumented and ViewModel testing — see the testing section of
the architecture doc.

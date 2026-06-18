# 09 — Project Folder & Package Structure

Complete annotated tree for **Prompt Gallery** (single `:app` module, Gradle Kotlin DSL, package root `com.promptgallery`). Every directory and key file has a one-line purpose. `…` denotes "and siblings of the same kind".

---

## 1. Repository root

```text
prompt-gallery/
├── settings.gradle.kts              # Module inclusion (:app), plugin & repo management
├── build.gradle.kts                 # Root build script — plugins declared `apply false`
├── gradle.properties                # JVM args, AndroidX, Compose flags, Kotlin opts
├── gradlew / gradlew.bat            # Gradle wrapper scripts
├── local.properties                 # SDK path + (gitignored) signing/secrets
├── .editorconfig                    # ktlint/IDE formatting rules
├── detekt.yml                       # detekt static-analysis config
├── README.md                        # Project overview & build instructions
├── gradle/
│   ├── libs.versions.toml           # Version catalog (all deps + versions in one place)
│   └── wrapper/
│       ├── gradle-wrapper.jar       # Wrapper binary
│       └── gradle-wrapper.properties# Pinned Gradle distribution version
├── docs/                            # Project documentation (this folder)
│   ├── 08-IMPLEMENTATION-PLAN.md    # Phased roadmap, inventory, standards, testing
│   ├── 09-FOLDER-STRUCTURE.md       # This file — annotated tree
│   └── 10-FUTURE-ROADMAP-AND-LOCAL-AI.md  # Scaling + optional on-device ML
├── .github/
│   └── workflows/
│       └── ci.yml                   # CI: lint, detekt, unit + instrumented tests, release
└── app/                             # The single application module (expanded below)
```

---

## 2. App module

```text
app/
├── build.gradle.kts                 # App module build: plugins, android{}, deps via catalog
├── proguard-rules.pro               # R8/ProGuard keep rules (Room, kotlinx-serialization, SQLCipher)
├── schemas/                         # Room exported schema JSONs (per DB version) for migration tests
│   └── com.promptgallery.data.local.AppDatabase/
│       ├── 1.json                   # Schema snapshot v1
│       └── 2.json                   # Schema snapshot v2 …
└── src/
    ├── main/
    │   ├── AndroidManifest.xml      # App component declarations, permissions, FileProvider
    │   ├── kotlin/com/promptgallery/   # ← all production source (tree below)
    │   └── res/                     # Android resources (tree below)
    ├── test/                        # JVM unit tests (Robolectric where needed)
    │   └── kotlin/com/promptgallery/   # mirrors main package layout
    └── androidTest/                 # Instrumented + UI tests
        ├── kotlin/com/promptgallery/
        └── assets/                  # Golden sample PNGs (A1111/ComfyUI/InvokeAI) for parser tests
```

---

## 3. Production source tree (`app/src/main/kotlin/com/promptgallery/`)

```text
com/promptgallery/
│
├── PromptGalleryApp.kt              # @HiltAndroidApp Application; WorkManager + Coil init
├── MainActivity.kt                  # @AndroidEntryPoint single activity; sets Compose content
│
├── core/                            # Cross-cutting infrastructure (no feature logic)
│   ├── dispatchers/
│   │   └── DispatcherProvider.kt    # Injectable Main/IO/Default dispatchers
│   ├── result/
│   │   ├── AppResult.kt             # Success/Error result wrapper
│   │   └── AppError.kt              # Typed domain error taxonomy
│   ├── common/
│   │   ├── UiState.kt               # Base Loading/Empty/Content/Error contract
│   │   └── UiEvent.kt               # One-shot UI event (nav, snackbar) base
│   ├── logging/
│   │   └── Logger.kt                # Logging facade (no-op in release)
│   ├── datastore/
│   │   └── SettingsDataStore.kt     # Persisted prefs: view mode, theme, sort, encryption flag
│   └── util/
│       ├── Hashing.kt               # SHA-256 / xxHash file & string hashing
│       ├── FileExt.kt               # File/URI/stream helpers
│       ├── DateTimeExt.kt           # Instant ↔ display formatting
│       └── StringExt.kt             # Prompt/trigram string helpers
│
├── data/                            # Data layer: persistence, sources, repository impls
│   ├── local/
│   │   ├── AppDatabase.kt           # @Database: entities, version, DAO accessors, FTS triggers
│   │   ├── Converters.kt           # Room @TypeConverters (Instant, List, enums, JSON)
│   │   ├── Migrations.kt            # Migration objects + registered list
│   │   ├── DatabaseCallback.kt      # onCreate FTS rebuild / seeding
│   │   ├── SqlCipherSupportFactory.kt  # Optional encrypted SupportSQLite factory
│   │   ├── entity/
│   │   │   ├── ImageEntity.kt       # Core image + prompt + generation params row
│   │   │   ├── TagEntity.kt         # Tag (name unique, color, usageCount)
│   │   │   ├── ImageTagCrossRef.kt  # Image↔Tag many-to-many join
│   │   │   ├── CollectionEntity.kt  # Collection; isSmart + serialized query for smart collections
│   │   │   ├── ImageCollectionCrossRef.kt # Image↔Collection join + position
│   │   │   ├── FolderEntity.kt      # Hierarchical folder (self-referential parentId)
│   │   │   ├── PromptTemplateEntity.kt  # Reusable prompt template + variables
│   │   │   ├── ImageVersionEntity.kt    # Version history snapshot per image
│   │   │   └── ImageFts.kt          # @Fts4 external-content table over ImageEntity
│   │   ├── relation/
│   │   │   ├── ImageWithTags.kt     # @Relation image + tags
│   │   │   ├── ImageWithCollections.kt # image + collections
│   │   │   ├── CollectionWithImages.kt # collection + (paged) images
│   │   │   ├── FolderWithChildren.kt   # nested folder tree node
│   │   │   └── ImageWithVersions.kt    # image + version history
│   │   └── dao/
│   │       ├── ImageDao.kt          # Paged view queries, CRUD, bulk ops, by-hash lookup
│   │       ├── TagDao.kt            # Upsert, prefix autocomplete, rename/merge
│   │       ├── ImageTagDao.kt       # Link/unlink, bulk tagging
│   │       ├── CollectionDao.kt     # CRUD, reorder, smart-collection read
│   │       ├── ImageCollectionDao.kt# Membership add/remove/reorder
│   │       ├── FolderDao.kt         # CRUD, move subtree, path maintenance
│   │       ├── PromptTemplateDao.kt # Template CRUD + usage bump
│   │       ├── ImageVersionDao.kt   # Add version, set-current, history
│   │       ├── SearchDao.kt         # FTS MATCH + ranked + trigram-fallback queries
│   │       └── StatsDao.kt          # Counts for empty states / dashboards
│   ├── source/
│   │   ├── SafFileStore.kt          # SAF read/write + persisted URI permissions
│   │   ├── ThumbnailStore.kt        # Cached downscaled thumbnails
│   │   ├── MetadataExtractor.kt     # Dispatch raw image bytes → ParsedMetadata
│   │   └── parser/
│   │       ├── A1111Parser.kt       # Automatic1111 PNG tEXt "parameters" parser
│   │       ├── ComfyUiParser.kt     # ComfyUI workflow JSON parser
│   │       ├── InvokeAiParser.kt    # InvokeAI metadata parser
│   │       └── ExifParser.kt        # EXIF/XMP fallback parser
│   └── repository/
│       ├── ImageRepositoryImpl.kt   # Implements domain ImageRepository
│       ├── TagRepositoryImpl.kt
│       ├── CollectionRepositoryImpl.kt
│       ├── FolderRepositoryImpl.kt
│       ├── SearchRepositoryImpl.kt
│       ├── TemplateRepositoryImpl.kt
│       ├── ImportExportRepositoryImpl.kt
│       └── SettingsRepositoryImpl.kt
│
├── domain/                          # Pure Kotlin: models, repository contracts, use cases
│   ├── model/
│   │   ├── Image.kt                 # Domain image model (decoupled from Room)
│   │   ├── Tag.kt · Collection.kt · Folder.kt
│   │   ├── PromptTemplate.kt · ImageVersion.kt
│   │   ├── SearchQuery.kt           # Structured search/filter spec (also smart-collection body)
│   │   ├── GalleryView.kt           # enum: Masonry/Grid/Timeline/Collection/Favorites
│   │   ├── SortOrder.kt · BulkAction.kt
│   ├── repository/
│   │   ├── ImageRepository.kt       # Interface; impl lives in data/repository
│   │   ├── TagRepository.kt · CollectionRepository.kt · FolderRepository.kt
│   │   ├── SearchRepository.kt · TemplateRepository.kt
│   │   └── ImportExportRepository.kt · SettingsRepository.kt
│   └── usecase/
│       ├── gallery/
│       │   ├── GetGalleryImagesUseCase.kt   # Paged images for the active view+filters
│       │   ├── SetViewModeUseCase.kt
│       │   └── ToggleFavoriteUseCase.kt
│       ├── search/
│       │   ├── SearchImagesUseCase.kt       # Typo-tolerant FTS search
│       │   ├── SaveSearchUseCase.kt
│       │   └── EvaluateSmartCollectionUseCase.kt
│       ├── organize/
│       │   ├── TagImagesUseCase.kt · MoveToFolderUseCase.kt
│       │   ├── AddToCollectionUseCase.kt · MergeTagsUseCase.kt
│       │   └── BulkActionUseCase.kt         # Transactional multi-image actions
│       ├── template/
│       │   ├── ApplyTemplateUseCase.kt
│       │   └── InterpolateTemplateUseCase.kt# {variable} substitution
│       ├── version/
│       │   ├── AddVersionUseCase.kt
│       │   └── RestoreVersionUseCase.kt
│       └── io/
│           ├── ImportImagesUseCase.kt
│           └── ExportCatalogUseCase.kt
│
├── io/                              # Import/export pipelines (WorkManager + serializers)
│   ├── import/
│   │   └── ImportWorker.kt          # Background import: SAF read → extract → dedup → insert
│   ├── export/
│   │   ├── ExportWorker.kt          # Background export orchestration + progress
│   │   └── format/
│   │       ├── JsonExporter.kt      # Lossless catalog JSON
│   │       ├── CsvExporter.kt       # Tabular CSV
│   │       ├── MarkdownExporter.kt  # Human-readable Markdown gallery
│   │       └── ZipExporter.kt       # Images + manifest archive
│   └── model/
│       ├── CatalogManifest.kt       # @Serializable export/import manifest
│       ├── ExportOptions.kt         # Format + scope + media options
│       └── ImportReport.kt          # Imported/skipped/failed summary
│
├── ui/                              # Presentation: Compose screens + ViewModels
│   ├── theme/
│   │   ├── Theme.kt                 # Material 3 theme + dynamic color + dark mode
│   │   ├── Color.kt · Type.kt · Shape.kt
│   ├── navigation/
│   │   ├── PromptGalleryNavHost.kt  # Top-level NavHost wiring all destinations
│   │   ├── Destinations.kt          # Type-safe route definitions + args
│   │   └── AppScaffold.kt           # Bottom nav / nav rail + top bar slot
│   ├── components/                  # Reusable composables
│   │   ├── EmptyState.kt · LoadingState.kt · ErrorState.kt
│   │   ├── TagChip.kt · ConfirmDialog.kt · ProgressDialog.kt
│   ├── gallery/
│   │   ├── GalleryViewModel.kt      # View mode, paged data, filters, selection entry
│   │   ├── GalleryScreen.kt         # Hosts the active view + view-mode bar
│   │   ├── MasonryGrid.kt           # Staggered masonry layout
│   │   ├── StaggeredGrid.kt · TimelineList.kt
│   │   ├── ImageCard.kt             # Coil-loaded thumbnail card
│   │   └── ViewModeBar.kt           # Masonry/Grid/Timeline/Collection/Favorites switch
│   ├── detail/
│   │   ├── ImageDetailViewModel.kt
│   │   ├── ImageDetailScreen.kt
│   │   ├── PromptSection.kt · ParamsTable.kt · VersionTimeline.kt
│   ├── search/
│   │   ├── SearchViewModel.kt
│   │   ├── SearchScreen.kt · SearchBar.kt · FilterSheet.kt · ResultsGrid.kt
│   ├── collections/
│   │   ├── CollectionsViewModel.kt
│   │   ├── CollectionsScreen.kt · CollectionDetailScreen.kt
│   │   └── SmartCollectionEditor.kt # Build/save query-backed smart collections
│   ├── folders/
│   │   ├── FoldersViewModel.kt
│   │   └── FolderTreeScreen.kt
│   ├── favorites/
│   │   ├── FavoritesViewModel.kt
│   │   └── FavoritesScreen.kt       # Reuses gallery components, favorites filter
│   ├── templates/
│   │   ├── TemplatesViewModel.kt
│   │   ├── TemplatesScreen.kt · TemplateEditor.kt · ApplyTemplateSheet.kt
│   ├── importexport/
│   │   ├── ImportExportViewModel.kt
│   │   ├── ImportScreen.kt · ExportScreen.kt
│   ├── bulk/
│   │   ├── BulkSelectionViewModel.kt# Shared multi-select state
│   │   └── SelectionActionBar.kt    # Contextual bulk-action overlay
│   └── settings/
│       ├── SettingsViewModel.kt
│       ├── SettingsScreen.kt
│       ├── EncryptionSection.kt     # SQLCipher opt-in toggle + migration
│       └── BackupSection.kt
│
└── di/                              # Hilt modules
    ├── DatabaseModule.kt            # AppDatabase, DAOs, SQLCipher factory
    ├── RepositoryModule.kt          # @Binds repo interface → impl
    ├── DispatcherModule.kt          # DispatcherProvider
    ├── DataStoreModule.kt           # DataStore instances
    ├── WorkerModule.kt              # Hilt WorkManager assisted factories
    ├── ImageLoaderModule.kt         # Coil ImageLoader (caches, decoders)
    └── MetadataModule.kt            # Extractor + parser graph
```

---

## 4. Resources (`app/src/main/res/`)

```text
res/
├── drawable/                        # Vector icons, illustrations (empty-state art)
│   └── ic_launcher_foreground.xml   # Adaptive launcher foreground
├── mipmap-anydpi-v26/
│   └── ic_launcher.xml              # Adaptive icon definition
├── mipmap-*/                        # PNG launcher fallbacks per density
├── values/
│   ├── strings.xml                  # All user-facing strings (localizable)
│   ├── colors.xml                   # Non-dynamic color fallbacks
│   ├── themes.xml                   # Base XML theme (splash, status bar) bridging to Compose
│   └── dimens.xml                   # Spacing / size tokens
├── values-night/
│   └── themes.xml                   # Dark theme overrides
├── values-<locale>/                 # Translations (e.g. values-es, values-de)
│   └── strings.xml
├── xml/
│   ├── file_paths.xml               # FileProvider exposed paths (export sharing)
│   ├── backup_rules.xml             # Auto-backup inclusion/exclusion
│   └── data_extraction_rules.xml    # Android 12+ data transfer rules
└── raw/                             # Seed/demo data, license text (optional)
```

---

## 5. Test trees

```text
app/src/test/kotlin/com/promptgallery/        # JVM unit tests (mirror main packages)
├── domain/usecase/…                  # e.g. InterpolateTemplateUseCaseTest.kt
├── data/repository/…                 # Repo tests with fakes
├── data/source/parser/…              # A1111ParserTest, ComfyUiParserTest …
├── ui/**/…ViewModelTest.kt           # ViewModel state tests (Turbine + coroutines-test)
└── testutil/
    ├── FakeRepositories.kt           # Hand-rolled fakes
    ├── TestFixtures.kt               # Sample domain objects
    └── MainDispatcherRule.kt         # Coroutine test dispatcher rule

app/src/androidTest/kotlin/com/promptgallery/  # Instrumented + UI tests
├── data/local/
│   ├── ImageDaoTest.kt               # In-memory Room DAO tests
│   ├── SearchDaoFtsTest.kt           # FTS trigger + ranking tests
│   ├── MigrationTest.kt              # Schema migration verification
│   └── SqlCipherMigrationTest.kt     # Plaintext→encrypted migration
├── io/ImportExportRoundTripTest.kt   # WorkManager import/export round-trip
├── ui/
│   ├── GalleryScreenTest.kt · SearchScreenTest.kt · BulkSelectionTest.kt
│   └── A11ySemanticsTest.kt
├── benchmark/                        # (or separate :baselineprofile / :benchmark module)
│   ├── StartupBenchmark.kt · GalleryScrollBenchmark.kt · SearchLatencyBenchmark.kt
└── testutil/
    └── HiltTestRunner.kt             # Custom runner using HiltTestApplication
```

---

## 6. Why this layout

- **Feature-cohesive `ui/` packages** keep a screen's ViewModel + composables together (easy to find, easy to extract later).
- **`domain/` holds only pure Kotlin** (models, interfaces, use cases) — no Android imports — so it is trivially unit-testable and the first candidate for a future `:core:domain` module.
- **`data/` mirrors persistence concerns** (entities/relations/dao/source/repository) so the Room surface is contained and a multi-module split is mechanical.
- **`io/` isolates import/export** because those pipelines are heavy, dependency-rich, and evolve independently.
- **`core/` carries cross-cutting infra** shared by all layers without creating cyclic dependencies.
- **Package-by-feature today maps 1:1 to modules tomorrow** — see `10-FUTURE-ROADMAP-AND-LOCAL-AI.md` for the migration path.

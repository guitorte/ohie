# Prompt Gallery — Product Requirements Document (PRD)

**Document owner:** Product Design
**Status:** Approved for build
**Last updated:** 2026-06-18
**Platform:** Android (Kotlin, Jetpack Compose, Material 3)
**Document version:** 1.0

---

## 1. Vision

Prompt Gallery is the **home for an AI artist's image library and the prompts that created it**. Today, AI artists generate thousands of images across Stable Diffusion, Midjourney, DALL·E, Flux and other tools, but the *prompt* — the most valuable, reusable creative asset — is scattered across screenshots, chat logs, text files, and lost generation histories. Images and their prompts drift apart.

Prompt Gallery keeps them together, forever, **on the user's device**. It combines the fast visual browsing of Google Photos, the curation of Pinterest, the structured knowledge management of Obsidian, the catalog/rating discipline of Lightroom, and dedicated prompt-management tooling — wrapped in a calm, creative-tool aesthetic that respects the artist's focus.

> **One-line vision:** *Never lose a prompt again. Find any image and the exact recipe behind it in seconds, fully offline.*

---

## 2. Problem Statement

AI artists face four compounding problems:

1. **Prompt/image divorce.** Generated images are saved to disk, but the prompt, negative prompt, model, seed, sampler, CFG and steps that produced them are lost or buried in tool logs. Reproducing or iterating on a successful result becomes guesswork.
2. **No purpose-built library.** General photo apps (Google Photos) have no concept of a prompt, model, or seed. Note apps (Obsidian/Notion) are text-first and handle large image libraries poorly. Neither searches *prompts*.
3. **Retrieval is slow.** With 10,000+ images, finding "that cyberpunk portrait with the teal rim light" requires scrolling, because nothing indexes the descriptive prompt text.
4. **Privacy & ownership anxiety.** Many AI artists do not want their unreleased work or proprietary prompts uploaded to a cloud service or used as training data.

---

## 3. Goals & Non-Goals

### 3.1 Goals
- **G1 — Unify image + prompt.** Every image record stores its full generation metadata as first-class, editable fields.
- **G2 — Instant retrieval.** Sub-second, typo-tolerant search across prompt, negative prompt, tags, title, notes, collections, and AI model at 10,000+ images.
- **G3 — Effortless reuse.** Copy any prompt or negative prompt to the clipboard in **one tap**.
- **G4 — Visual-first browsing.** Beautiful, fast, lazily-loaded galleries (masonry, grid, timeline) that scroll smoothly at scale.
- **G5 — Deep organization.** Folders (nested), collections (incl. smart collections), tags, color labels, ratings, favorites.
- **G6 — Offline-first & private.** Fully functional with no network and no account. Optional encrypted database.
- **G7 — Frictionless ingest.** Import single/multiple/folder/drag-drop with automatic embedded-metadata extraction (PNG tEXt, EXIF, A1111 parameters).
- **G8 — Portable.** Export to ZIP, JSON, CSV, Markdown; full prompt-library backup.

### 3.2 Non-Goals
- **NG1 — Not a social network.** No feeds, followers, likes, comments, or public profiles.
- **NG2 — Not an image generator.** Prompt Gallery does not call any model to *create* images (v1).
- **NG3 — No mandatory cloud / no mandatory account.** Sync is out of scope for v1.
- **NG4 — Not a raster editor.** No painting, inpainting, or pixel editing. (Crop/rotate is out of scope for v1.)
- **NG5 — Not a multi-user/collaboration tool.** Single-user, single-device in v1.

---

## 4. Target Users & Personas

### Persona A — "Maya", the Prolific Hobbyist
- Generates 50–200 images/week with Automatic1111 and Forge on a home PC, exports to phone.
- Pain: a folder of 8,000 PNGs with cryptic filenames; can't find or re-run her best results.
- Needs: bulk import with auto-metadata, fast masonry browsing, one-tap copy prompt, favorites.

### Persona B — "Devin", the Freelance AI Designer
- Delivers client concept art. Maintains reusable prompt templates per client/style.
- Pain: reinventing prompts, inconsistent style, no record of which seed/model delivered.
- Needs: prompt templates with variables, collections per client, ratings, Markdown/CSV export for handoff.

### Persona C — "Sora", the Curatorial Aesthete
- Treats AI art as a personal museum. Cares about mood boards and color harmony.
- Pain: general galleries don't let her curate by color/theme or build smart collections.
- Needs: color labels, smart collections, beautiful masonry, timeline view.

### Persona D — "Priya", the Privacy-Conscious Pro
- Works on unreleased/NDA material; refuses cloud upload.
- Pain: every modern tool wants an account and a server.
- Needs: 100% offline, local-only storage, SQLCipher-encrypted DB, no telemetry.

---

## 5. Feature List with Priorities (MoSCoW)

Priorities: **M** = Must (v1 launch), **S** = Should (v1 if time), **C** = Could (fast-follow), **W** = Won't (this release).

### 5.1 Library & Data
| ID | Feature | Priority |
|----|---------|----------|
| L1 | Image record with full canonical field set (prompt, negativePrompt, aiModel, seed, sampler, cfg, steps, dimensions, etc.) | M |
| L2 | Room persistence, MVVM + Repository, Hilt DI | M |
| L3 | Paging 3 pagination + thumbnail caching for 10,000+ images | M |
| L4 | Image versions (ImageVersion history of prompt edits) | S |
| L5 | Optional SQLCipher encrypted database | S |

### 5.2 Import
| ID | Feature | Priority |
|----|---------|----------|
| I1 | Import single image | M |
| I2 | Import multiple images | M |
| I3 | Import entire folder (recursive option) | M |
| I4 | Drag-and-drop import | S |
| I5 | Embedded metadata extraction: PNG tEXt / EXIF / A1111 `parameters` | M |
| I6 | Manual metadata entry/override on import | M |
| I7 | Duplicate detection on import (hash/path) | S |

### 5.3 Browse & View
| ID | Feature | Priority |
|----|---------|----------|
| B1 | Masonry gallery | M |
| B2 | Standard grid gallery (variable density) | M |
| B3 | Timeline view (grouped by date) | S |
| B4 | Collection view | M |
| B5 | Favorites view | M |
| B6 | Image detail screen with full prompt card | M |
| B7 | Swipe between images in detail | M |
| B8 | Zoom/pan full-resolution image | M |

### 5.4 Prompt Detail Card
| ID | Feature | Priority |
|----|---------|----------|
| P1 | Expand / Collapse card | M |
| P2 | Copy Prompt (one tap) | M |
| P3 | Copy Negative Prompt (one tap) | M |
| P4 | Edit prompt/metadata | M |
| P5 | Duplicate (clone record + image) | S |
| P6 | Share (Android share sheet: image, prompt, or both) | M |

### 5.5 Search
| ID | Feature | Priority |
|----|---------|----------|
| SR1 | FTS over prompt, negativePrompt, title, description, customNotes | M |
| SR2 | Filter by tags, collections, AI model, color label, rating, favorite, date | M |
| SR3 | Fuzzy / partial / typo-tolerant (trigram + Levenshtein rerank) | S |
| SR4 | Search history & saved searches | C |
| SR5 | Smart collection = saved query (smartQuery) | S |

### 5.6 Organize
| ID | Feature | Priority |
|----|---------|----------|
| O1 | Folders + nested folders | M |
| O2 | Collections (manual) | M |
| O3 | Smart collections (rule-based) | S |
| O4 | Tags (many-to-many) | M |
| O5 | Color labels (RED…PURPLE/NONE) | M |
| O6 | Rating (0–5) | M |
| O7 | Favorite toggle | M |

### 5.7 Bulk Operations
| ID | Feature | Priority |
|----|---------|----------|
| BK1 | Multi-select mode | M |
| BK2 | Bulk tag / move / favorite / color label / rating | M |
| BK3 | Bulk add to collection | M |
| BK4 | Bulk delete | M |
| BK5 | Bulk export | S |

### 5.8 Prompt Templates
| ID | Feature | Priority |
|----|---------|----------|
| T1 | Create/edit prompt templates with category | S |
| T2 | Variables (`{subject}`, `{style}`) via variablesJson | S |
| T3 | Use template → fill variables → copy / attach | S |
| T4 | useCount tracking & sort by most used | C |

### 5.9 Export & Backup
| ID | Feature | Priority |
|----|---------|----------|
| E1 | Export ZIP (images + metadata) | S |
| E2 | Export JSON | M |
| E3 | Export CSV | S |
| E4 | Export Markdown | S |
| E5 | Full prompt-library backup/restore | S |

### 5.10 System & Settings
| ID | Feature | Priority |
|----|---------|----------|
| SY1 | Material 3 dynamic color, dark/light/system theme | M |
| SY2 | Default gallery view & grid density preference | M |
| SY3 | App lock (biometric) + DB encryption toggle | S |
| SY4 | Storage location / cache management | S |
| SY5 | No telemetry by default; optional anonymous local-only diagnostics | M |
| SY6 | Cloud sync | W |

---

## 6. User Stories with Acceptance Criteria

### Epic 1 — Import & Metadata

**US-1.1** — *As Maya, I want to import a folder of A1111 PNGs so their prompts are extracted automatically.*
- **AC1:** Selecting a folder lists all supported images (png/jpg/jpeg/webp) with a count.
- **AC2:** For each PNG containing an A1111 `parameters` tEXt chunk, the app parses prompt, negativePrompt, aiModel, seed, sampler, cfg, steps, and dimensions into the correct canonical fields.
- **AC3:** EXIF `creationDate` populates `creationDate`; `importDate` is set to now.
- **AC4:** A thumbnail is generated and `thumbnailPath` stored; original at `filePath` is never modified.
- **AC5:** Import of 500 images completes without ANR and shows a progress indicator with cancel.

**US-1.2** — *As a user, I want to edit/override metadata an importer got wrong.*
- **AC1:** Every canonical field is editable from the image detail Edit screen.
- **AC2:** Saving updates `modifiedDate`.
- **AC3:** Numeric fields validate type (seed Long, cfg Float, steps Int) and reject invalid input with inline error.

### Epic 2 — Browse & View

**US-2.1** — *As Sora, I want a masonry gallery that scrolls smoothly through 10,000 images.*
- **AC1:** Initial paint < 1s on a mid-range device with 10,000 records.
- **AC2:** Scroll stays ≥ 55 fps median; images load lazily via paging; thumbnails are cached.
- **AC3:** Switching masonry ↔ grid preserves scroll position to the nearest visible item.

**US-2.2** — *As a user, I want to open an image and see its full prompt recipe.*
- **AC1:** Detail screen shows the image plus a prompt card with prompt, negativePrompt, aiModel, seed, sampler, cfg, steps.
- **AC2:** I can swipe left/right to the previous/next image in the current view's order.
- **AC3:** Pinch-to-zoom and double-tap zoom work on the full-resolution image.

### Epic 3 — Copy & Reuse (core value)

**US-3.1** — *As Devin, I want to copy a prompt in one tap.*
- **AC1:** A visible "Copy Prompt" control copies the exact `prompt` string to the clipboard in a single tap.
- **AC2:** A confirmation (snackbar/toast) appears within 150 ms.
- **AC3:** "Copy Negative Prompt" is separately available and copies only `negativePrompt`.

**US-3.2** — *As a user, I want to duplicate an image+prompt to iterate.*
- **AC1:** Duplicate creates a new record with a new `id`, copies all editable fields, and copies the file (new `filePath`/`fileName`).
- **AC2:** The duplicate opens directly in Edit mode.

### Epic 4 — Search

**US-4.1** — *As Maya, I want to find images by words in their prompt even with a typo.*
- **AC1:** Typing in search queries the FTS index over prompt/negativePrompt/title/description/customNotes and returns results incrementally.
- **AC2:** A misspelled token (e.g., "cyberpnk") still surfaces relevant results via trigram/Levenshtein reranking.
- **AC3:** Results return in < 500 ms at 10,000 images for a 2-word query.

**US-4.2** — *As a user, I want to combine search text with filters.*
- **AC1:** I can constrain by AI model, tags, collection, color label, rating ≥ N, favorites-only, and date range simultaneously.
- **AC2:** Active filters are shown as removable chips; clearing all resets results.

### Epic 5 — Organize

**US-5.1** — *As Sora, I want a smart collection that auto-fills.*
- **AC1:** Creating a smart collection stores a `smartQuery`; `isSmartCollection = true`.
- **AC2:** Opening it evaluates the query live; newly imported matching images appear without manual add.

**US-5.2** — *As a user, I want color labels and ratings to curate.*
- **AC1:** I can set `colorLabel` (one of the 7 values) and `rating` (0–5) from detail and from multi-select.
- **AC2:** Both are filterable in search and visible as overlays on thumbnails.

### Epic 6 — Bulk Operations

**US-6.1** — *As Maya, I want to select many images and tag/move them at once.*
- **AC1:** Long-press a thumbnail enters multi-select; tapping toggles selection; a count is shown.
- **AC2:** Bulk actions available: tag, add-to-collection, move-to-folder, favorite, color label, rating, export, delete.
- **AC3:** Bulk delete asks for confirmation and is undoable for 5 s via snackbar.

### Epic 7 — Prompt Templates

**US-7.1** — *As Devin, I want reusable templates with variables.*
- **AC1:** A template stores promptText/negativePromptText and a `variablesJson` list of variable names.
- **AC2:** Using a template prompts me to fill each variable, then lets me Copy or Attach the resolved text.
- **AC3:** Each use increments `useCount`.

### Epic 8 — Export & Privacy

**US-8.1** — *As Devin, I want to export selected images and prompts as Markdown for a client.*
- **AC1:** Export produces a Markdown file with each image (relative path) and its prompt metadata.
- **AC2:** JSON/CSV exports include all canonical fields with stable keys.

**US-8.2** — *As Priya, I want the database encrypted and the app locked.*
- **AC1:** Enabling encryption migrates the Room DB to SQLCipher transparently.
- **AC2:** App lock requires biometric/PIN on launch and after background timeout.
- **AC3:** No data leaves the device; no network permission is required for core features.

---

## 7. Success Metrics

| Metric | Target |
|--------|--------|
| Time-to-copy (open app → prompt on clipboard) | < 10 s median |
| Search latency @ 10k images | < 500 ms p95 |
| Gallery scroll smoothness | ≥ 55 fps median |
| Cold start to first paint | < 1.5 s on mid-range device |
| Import throughput | ≥ 5 images/sec with metadata parse |
| Crash-free sessions | ≥ 99.5% |
| D30 retention (active library users) | ≥ 40% |
| % images with a stored prompt | ≥ 80% (proxy for core-value delivery) |

---

## 8. Constraints

- **C1 — Offline-first:** all core features must work with zero connectivity and no account.
- **C2 — Local storage limits:** thumbnails and DB must be space-efficient; respect scoped storage / MediaStore on Android 10+.
- **C3 — Performance:** must remain fluid at 10,000+ records on mid-range hardware (4 GB RAM).
- **C4 — Tech stack fixed:** Kotlin, Jetpack Compose, Material 3, Room, Coroutines/Flow, MVVM + Repository, Hilt, Navigation Compose.
- **C5 — Accessibility:** WCAG 2.1 AA-aligned; full TalkBack support; min 48dp touch targets.
- **C6 — Privacy:** no telemetry by default; no third-party trackers; no required runtime network permission for core flows.

---

## 9. Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|------------|
| Metadata formats vary wildly (A1111 vs Comfy vs Midjourney) | Poor auto-fill | High | Pluggable parser strategy; graceful fallback to raw text in `customNotes`; manual override always available |
| Masonry performance degrades at scale | Janky scroll, churn | Medium | Paging 3, stable keys, fixed thumbnail dimensions from width/height, Coil disk+memory cache |
| Fuzzy search is slow at 10k rows | Frustration | Medium | Two-stage: FTS prefilter then Levenshtein rerank top-N only |
| SQLCipher migration data loss | Catastrophic | Low | Atomic migration with verified backup before swap; tested rollback |
| Storage bloat from duplicate thumbnails | Disk pressure | Medium | Content-hash dedupe; cache size cap with LRU eviction |
| Scoped storage restrictions on file access | Import failures | Medium | Use SAF/MediaStore; persist URI permissions |
| Scope creep toward social/generation | Delays v1 | Medium | NG list enforced in review; defer to fast-follow |
| Accidental bulk delete | Data loss | Medium | Confirm + 5s undo + (optional) trash/recently-deleted |

---

## 10. Release Scope Summary

**v1 (Must + selected Should):** full data model, import (single/multi/folder + A1111/PNG/EXIF parse), masonry + grid + collection + favorites views, image detail with prompt card, one-tap copy, edit, share, FTS search + filters, folders/collections/tags/color labels/ratings/favorites, multi-select bulk ops, JSON export, Material 3 theming, no-telemetry privacy.

**Fast-follow:** timeline view, smart collections, fuzzy rerank, prompt templates with variables, ZIP/CSV/Markdown export, image versions, SQLCipher + app lock, drag-drop import, duplicate detection, saved searches.

**Out (v1):** cloud sync, multi-user, image generation, raster editing.

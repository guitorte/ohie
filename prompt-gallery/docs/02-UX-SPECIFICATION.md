# Prompt Gallery — UX Specification

**Document owner:** Product Design
**Status:** Approved for build
**Last updated:** 2026-06-18
**Companion docs:** 01-PRODUCT-REQUIREMENTS.md, 03-UI-FLOWS-AND-WIREFRAMES.md

---

## 1. Design Principles

1. **Visual-first.** The image is the hero. Chrome recedes; the grid dominates the screen. Text and controls support browsing, never compete with it.
2. **Fast capture, fast retrieval, fast copy.** The three highest-frequency jobs — import, find, copy — are each reachable in the fewest possible taps. Copy Prompt is always **one tap** wherever a prompt is shown.
3. **Minimal taps, progressive disclosure.** Show the essentials; reveal depth (metadata, advanced filters, template variables) only when asked. The prompt card is collapsed by default with a one-tap expand.
4. **Calm creative-tool aesthetic.** Material 3 with dynamic color, generous spacing, restrained motion. No badges, streaks, gamification, or social cues. This is a studio, not a feed.
5. **Trust & privacy by default.** Local-only is communicated, not hidden. Destructive actions confirm and are reversible. The user owns and can export everything.
6. **Predictable & consistent.** The same gesture means the same thing everywhere (tap = open, long-press = select, swipe = navigate). Selection, filtering, and copy patterns are identical across all views.
7. **Resilient at scale.** Every list is lazy, paginated, and cached. The UI never blocks on I/O; long work shows progress and is cancelable.

---

## 2. Interaction Patterns (and their lineage)

| Pattern | Borrowed from | Application in Prompt Gallery |
|---------|---------------|-------------------------------|
| Edge-to-edge masonry/grid, lazy thumbnails | **Google Photos** | Default browse experience; grid density pinch |
| Long-press to enter multi-select, contextual action bar | **Google Photos / Apple Photos** | Bulk tagging, move, delete, export |
| Pinch to change grid density (zoom in/out the grid) | **Apple Photos** | Masonry/grid column count |
| Swipe between full-screen items; pull-down to dismiss detail | **Apple Photos / Google Photos** | Image detail navigation |
| Filmstrip + metadata/develop panel, ratings, color labels, flags | **Lightroom** | Detail metadata panel, rating 0–5, color labels, favorites |
| Save-to-board / collection picker bottom sheet | **Pinterest** | Add to collection; visual collection covers |
| Structured fields + templates with variables; saved queries | **Notion / Obsidian** | Prompt templates, smart collections, properties-style metadata |
| Type-ahead search with live results + filter chips | **Google Photos / Notion** | Search screen |
| Slide-up bottom sheet for actions & creation | **Material / Google apps** | Import sheet, collection sheet, share, sort |

---

## 3. Gesture Map

| Gesture | Context | Action |
|---------|---------|--------|
| **Tap** | Thumbnail (browse) | Open image detail |
| **Tap** | Thumbnail (multi-select active) | Toggle selection |
| **Long-press** | Thumbnail | Enter multi-select, select that item |
| **Long-press** | Collection / folder / tag | Context menu (rename, delete, set cover) |
| **Pinch in/out** | Gallery | Decrease/increase grid density (columns) |
| **Swipe left/right** | Image detail | Next / previous image |
| **Swipe down** | Image detail | Dismiss back to gallery |
| **Swipe up** | Image detail | Reveal/expand prompt + metadata card |
| **Pinch / double-tap** | Image detail | Zoom / reset zoom on full-res image |
| **Pull down** | Top of gallery | Refresh / re-scan import sources |
| **Swipe right on row** | List items (folders, templates) | Quick actions (optional) |
| **Two-finger swipe / drag** | External files onto app | Drag-and-drop import |
| **Tap & hold + drag** | Multi-select thumbnails | (Optional) reorder within manual collection |

> **Single-tap copy guarantee:** Copy Prompt and Copy Negative Prompt are direct buttons — never hidden behind a long-press or menu.

---

## 4. Key Task Flows

> Full diagrams and wireframes are in 03-UI-FLOWS-AND-WIREFRAMES.md. Below are the UX-level happy paths and their design intent.

### 4.1 Import images
**Intent:** ingest with zero busywork; metadata parsed automatically.
1. Tap **+ (FAB)** → Import bottom sheet.
2. Choose **Single / Multiple / Folder / Drag-drop**.
3. System picker (SAF) → selection.
4. App shows a **review sheet**: thumbnails, detected count, parsed-metadata badge (e.g., "412 of 500 prompts detected"), destination folder selector.
5. Tap **Import** → progress bar with running count + **Cancel**.
6. On finish: snackbar "500 imported · 412 with prompts" → tap to view.

**Design notes:** parsing happens in the background; user can leave. Unparsed images still import (raw `parameters` text, if any, lands in `customNotes`).

### 4.2 Attach / edit a prompt
1. Open image → prompt card (collapsed) → **Edit** (pencil).
2. Edit screen: grouped fields — *Prompt*, *Negative prompt*, *Model & generation* (aiModel, seed, sampler, cfg, steps), *Details* (title, description, notes, sourceUrl), *Organize* (folder, tags, color label, rating).
3. **Save** → updates `modifiedDate`; if prompt changed and versioning is on, append an `ImageVersion`.

### 4.3 Search
1. Tap search → keyboard focuses; recent searches shown.
2. Type → live FTS results stream in; typo-tolerant rerank applies.
3. Add **filter chips** (model, tags, collection, color, rating, favorites, date).
4. Tap result → detail. Optionally **Save as smart collection**.

### 4.4 Copy a prompt (core)
1. From thumbnail overflow **or** detail prompt card → tap **Copy Prompt**.
2. Snackbar "Prompt copied" within 150 ms.
- Negative prompt is a sibling one-tap action. No confirmation dialog, ever.

### 4.5 Bulk select & act
1. Long-press thumbnail → multi-select; top bar shows count + Select-all/Clear.
2. Tap more to add; bottom action bar: **Tag · Collection · Move · Favorite · Color · Rate · Export · Delete**.
3. Choose action → relevant sheet/picker → apply.
4. Destructive (delete) → confirm + 5s undo snackbar.

### 4.6 Create a collection
1. Library tab → **New collection** (sheet): name, description, optional cover.
2. Choose **Manual** or **Smart** (define rule → stored as `smartQuery`, `isSmartCollection = true`).
3. Add images via bulk select → **Add to collection** (Pinterest-style picker).

### 4.7 Use a prompt template
1. Templates tab → pick template (sorted by `useCount`).
2. Fill **variables** (from `variablesJson`) inline → live preview of resolved prompt.
3. **Copy** resolved prompt / negative, or **Attach to new image** during import. `useCount++`.

---

## 5. State Design (Empty / Loading / Error)

### 5.1 Empty states
| Screen | Illustration + message | Primary action |
|--------|------------------------|----------------|
| Gallery (fresh install) | "Your gallery is empty. Import your first AI image and its prompt." | **Import images** |
| Search (no query) | Recent searches + suggestions (popular models/tags) | — |
| Search (no results) | "No matches for '{query}'. Check filters or try fewer words." | **Clear filters** |
| Collection (empty) | "Nothing here yet. Add images to this collection." | **Add images** |
| Smart collection (empty) | "No images match this smart collection's rule yet." | **Edit rule** |
| Favorites (empty) | "Tap the heart on any image to add it here." | **Browse gallery** |
| Templates (empty) | "Create a reusable prompt template." | **New template** |
| Folders (empty) | "Organize your images into folders." | **New folder** |

### 5.2 Loading states
- **Gallery:** skeleton shimmer placeholders sized from each item's stored width/height (no layout shift); thumbnails fade in (150 ms) as Coil resolves.
- **Detail:** progressive image (thumbnail upscaled → full-res) ; metadata renders immediately from DB.
- **Search:** inline spinner in the field; results stream incrementally; "Searching…" only if > 300 ms.
- **Import:** determinate progress with count and Cancel; per-item status (parsed / no metadata).
- **Export:** determinate progress; result snackbar with **Open / Share**.

### 5.3 Error states
| Situation | Treatment |
|-----------|-----------|
| Image file missing/moved | Detail shows "File not found" placeholder + **Relink** / **Remove record** |
| Import file unreadable/unsupported | Skip with per-item note; end summary "3 skipped · view details" |
| Metadata parse failed | Image still imported; banner "Prompt not detected — add manually?" |
| Search index unavailable | Fallback to LIKE search + non-blocking toast "Limited search" |
| Storage full | Blocking dialog before write; suggest cache clear/export |
| Encryption migration failure | Roll back to backup; dialog "Encryption not applied; your data is safe" |
| Permission denied (SAF) | Inline rationale + **Grant access** re-prompt |

> **Error tone:** plain, blameless, actionable. Always offer a next step. Never lose user data on error.

---

## 6. Accessibility

- **TalkBack:** every interactive element has a content description; thumbnails announce title/model/rating; Copy actions announce success ("Prompt copied").
- **Touch targets:** minimum 48×48 dp; spacing avoids mis-taps in dense grids.
- **Contrast:** text/icon contrast meets WCAG AA (≥ 4.5:1 body, ≥ 3:1 large/icon) in both themes; dynamic-color palettes validated for contrast.
- **Dynamic type:** respect system font scaling up to 200%; layouts reflow, never clip.
- **Color independence:** color labels also carry a text/shape affordance (label name in menus, distinct chip), never color-only meaning.
- **Focus order:** logical traversal; focus trapped appropriately in sheets/dialogs; back/escape always exits.
- **Motion sensitivity:** honor "Remove animations"; disable parallax/auto-zoom; keep essential transitions only.
- **Captions for media:** N/A (still images), but title/description read aloud in detail.
- **Keyboard / external input:** arrow-key navigation in grid, Enter to open, Ctrl/Cmd+C copies prompt in detail.

---

## 7. Motion & Animation Guidelines

**Philosophy:** motion clarifies spatial relationships and provides feedback; it never delays the user or shows off.

| Element | Motion | Duration | Easing |
|---------|--------|----------|--------|
| Thumbnail → detail | Shared-element image transition (container transform) | 300 ms | Emphasized (M3) |
| Detail dismiss (swipe down) | Image scales toward origin, scrim fades | 250 ms | Standard decelerate |
| Prompt card expand/collapse | Height + content fade | 200 ms | Standard |
| Bottom sheet (import, collection, share) | Slide up + scrim | 250 ms | Emphasized decelerate |
| Grid density (pinch) | Smooth column reflow | tracks gesture | Spring (low bounce) |
| Multi-select enter | Checkmarks fade/scale in, scrim dim | 150 ms | Standard |
| Snackbar (copy/undo) | Slide up + auto-dismiss | in 150 / out 200; visible 4 s (5 s for undo) | Standard |
| Thumbnail load | Cross-fade from skeleton | 150 ms | Linear |
| FAB | Standard M3 FAB transitions; morphs to sheet on import | 200 ms | Emphasized |

**Rules:**
- Total perceived latency for any tap feedback ≤ 100 ms (use immediate ripple/state even if work is async).
- No animation blocks input; all are interruptible.
- Respect reduced-motion: replace transforms with simple cross-fades or none.
- Avoid looping/idle animation entirely — the studio is quiet.

---

## 8. Copy, Tone & Microcopy

- **Voice:** confident, concise, maker-to-maker. No exclamation spam, no cutesy mascots.
- **Buttons:** verbs ("Import", "Copy Prompt", "Add to collection").
- **Confirmations are quiet:** snackbars over dialogs, except for destructive/irreversible actions.
- **Privacy reassurance** appears at first import and in Settings: "Everything stays on your device."
- **Numbers matter to artists:** show counts (images, with-prompts, selected) prominently.

---

## 9. Theming & Layout Tokens

- **Material 3 dynamic color** seeded from wallpaper; manual override Light / Dark / System.
- **Surfaces:** elevated cards for prompt/metadata; gallery on lowest surface so images pop.
- **Spacing scale:** 4/8/12/16/24 dp; grid gutter 2–4 dp (tight, photo-app feel).
- **Typography:** M3 type scale; monospace for prompt body text (readability of tokens, brackets, weights).
- **Iconography:** Material Symbols, outlined; favorite = heart, rating = stars, color label = colored dot/chip.
- **Density:** grid density user-preference (Comfortable / Standard / Dense) mirrored by pinch.

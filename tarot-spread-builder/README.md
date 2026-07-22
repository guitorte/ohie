# Arcanum — Tarot Spread Builder

A standalone, static web app for building and rendering tarot/oracle card
spreads. This directory is an independent side project, unrelated to anything
else in this repo — extracted from a WebView-based Android app's debug build
(`assets/www`) so the HTML/CSS/JS can be edited directly.

## Running locally

No build step. Serve the folder statically and open `index.html`, e.g.:

```
python3 -m http.server 8080
```

Then visit `http://localhost:8080/`.

## Structure

- `index.html`, `css/style.css` — UI shell and screens
- `js/data.js` — card UIDs, deck configs, built-in spread layouts, storage
- `js/app.js` — screen navigation, spread list/editor, card selection flow
- `js/engine.js`, `js/renderer.js` — shuffle/draw logic and canvas rendering of a spread
- `decks/` — card artwork for the built-in decks (Lenormand, Rider-Waite-Smith, Tarot de Marseille)

## Android build

`android/` is a minimal Gradle/Kotlin wrapper that reconstructs the original
app's shell: a single `MainActivity` hosting a `WebView` pointed at
`file:///android_asset/www/index.html`, plus the `Android.saveImage(dataUrl,
filename)` JS bridge that `js/app.js` already calls to save an exported spread
to the gallery. The original app's compiled APK didn't include buildable
source, only the `assets/www` bundle above, so this wrapper is reconstructed
(package `com.tarot.app`, same bridge interface) rather than recovered.

The web app (`index.html`, `css/`, `js/`, `decks/`) is copied into
`android/app/src/main/assets/www` at build time (see
`android/app/build.gradle.kts:copyWebAssets`) — it isn't duplicated in git.

Builds automatically via `.github/workflows/arcanum-android.yml` on any push
under `tarot-spread-builder/**`, producing a debug APK as a workflow artifact
(`arcanum-debug-apk`). To build locally: `cd android && ./gradlew
assembleDebug` (requires an Android SDK; `ANDROID_HOME`/`local.properties` set
up as usual).

## Spread editor: fine 2×2 grid (half-steps in both axes)

The editor works on a **fine grid whose cell is half a card in both
dimensions**, so a card occupies a **2×2 block** of cells and always stays the
same size. Placing a card at a half-column *or* half-row offset is the same
gesture — just anchor its block one fine cell over or down. There is no special
"nudge": half-step positioning falls out of the grid itself, symmetrically on
both axes.

Tools are just **Carta** (place a card), **Sobrepost.** (an overlap slot: front
+ crossed back, consuming two of the spread's cards), and **Apagar** (remove).
`+ Linha`/`+ Coluna` grow the workspace by one card unit (two fine cells).

Custom spreads are stored in a grid format:

```js
{ nome, grid: { cols, rows, cards: [ { r, c, t } ] } }   // r,c in half-card cells; t: 'card'|'overlap'
```

The 20 built-in spreads keep the legacy row `estrutura` format and render
through the original row renderer. Opening a built-in (or an older custom
spread saved before this change) in the editor converts it into the grid model
on the fly — a card that carried a vertical `dy` offset simply lands on the
corresponding half-row.

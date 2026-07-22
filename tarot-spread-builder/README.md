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

## Spread editor: vertical half-steps

The spread editor's grid already let you place a card at half-column
resolution horizontally. It had no equivalent for the vertical axis — a card
could only sit flush on its row.

Two new tools, **½ Cima** and **½ Baixo**, nudge an already-placed card up or
down by half a card height (capped to one half-step in either direction).
Nudged cards are marked in the editor with a small vertical offset and a gold
outline, and serialize using the existing `[cardIndex, dy]` layout format that
the renderer already supported but the editor never wrote.

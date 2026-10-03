---
title: Cherrypick
emoji: 🍒
colorFrom: red
colorTo: pink
sdk: gradio
app_file: app.py
pinned: false
short_description: Bulk content extractor that lets you pick the pages first
---

# 🍒 Cherrypick

A bulk website-to-Markdown extractor, inspired by the *Bulk Content Extractor* tab of
[Agents-MCP-Hackathon/web-scraper](https://huggingface.co/spaces/Agents-MCP-Hackathon/web-scraper),
with one difference: **it shows you every link before downloading anything**, so you
decide what goes in.

## How it works

1. **Discover** – give it a start URL and a **depth**:
   - depth 1 → the links on the start page;
   - depth 2 → those, plus the sub-pages linked from *each* of them;
   - depth 3–4 → one or two levels further.

   Example: starting at an overview page that links to 36 cards, where each card
   page links to 3 sub-pages, depth 2 lists every card *and* its 3 sub-pages, grouped
   as a tree. Options: an optional path prefix (e.g. `/lenormand/`), same-domain only,
   `robots.txt`, a cap on pages opened, and **ignore menu links on sub-pages**, which
   drops links that repeat across many sibling pages (nav bars, prev/next, footers),
   so each page keeps only the sub-pages that are really its own. Only pages that
   must be opened to find *more* links are fetched; the deepest level is just listed.
2. **Pick** – every link appears as a checkbox, all ticked, with sub-pages indented
   under the page they came from. With **sub-pages follow their parent** on, unticking
   a page drops its whole branch in one click (the start page only toggles itself).
   For big lists, use the filter (`blog 2024 -tag` → all terms must match, `-word`
   excludes) together with **Select / Deselect / Invert shown**, or keep only links up to
   a given crawl depth. Selections on hidden rows are preserved while you filter.
3. **Extract** – only the ticked pages are downloaded and converted to Markdown
   (scripts, nav, header, footer and asides are stripped; `<main>`/`<article>` is
   preferred when present; links are made absolute). Pages already fetched during
   discovery are reused from cache.

The download is a `.zip` containing:

```
index.md          list of pages (and any that failed, with the reason)
combined.md       every page in one file, separated by ---
pages/<page>/*.md one folder per top-level page, holding it and its sub-pages
                  (each file has title/source front matter)
```

## Run locally

```bash
cd cherrypick
pip install -r requirements.txt
python app.py        # → http://127.0.0.1:7860
```

To publish it as a Hugging Face Space, push the contents of this folder to a new
Gradio Space — the front matter above is already set up.

## Files

- `crawler.py` – discovery, fetching, HTML→Markdown and zip building (no UI code)
- `app.py` – the Gradio interface and selection logic

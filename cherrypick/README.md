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

1. **Discover** – give it a start URL. It crawls breadth-first (depth 1–3, same domain
   by default, optional path prefix, respects `robots.txt`) and lists every page link it
   finds. Only pages that need to be opened to find *more* links are fetched; the
   leaves are just listed, so this step is quick.
2. **Pick** – every link appears as a checkbox, all ticked. Untick what you don't want.
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
pages/*.md        one file per page, with title/source front matter
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

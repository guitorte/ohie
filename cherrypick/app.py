"""Cherrypick — a bulk content extractor that lets you choose the pages first.

Step 1  Discover: crawl a site and list every page link found.
Step 2  Pick:     tick / untick links (with a filter to bulk-select groups).
Step 3  Extract:  download only the kept pages as Markdown, zipped.
"""

from __future__ import annotations

import os
import tempfile
from datetime import datetime

import gradio as gr

from crawler import Crawler, build_zip, combined_markdown

PREVIEW_CHARS = 20_000


# ---- selection helpers -----------------------------------------------------

def matches(link: dict, query: str) -> bool:
    """Space-separated terms must all appear; a leading '-' excludes a term."""
    hay = (link["url"] + " " + link["text"]).lower()
    for term in query.lower().split():
        if term.startswith("-") and len(term) > 1:
            if term[1:] in hay:
                return False
        elif term not in hay:
            return False
    return True


def visible(links: list[dict], query: str) -> list[dict]:
    return [l for l in links if matches(l, query or "")]


def label(link: dict) -> str:
    return f"{link['text']}  —  {link['url']}"


def render(links, selected, query):
    """Rebuild the checkbox list and counter from the source-of-truth state."""
    shown = visible(links, query)
    chosen = set(selected)
    box = gr.update(choices=[(label(l), l["url"]) for l in shown],
                    value=[l["url"] for l in shown if l["url"] in chosen])
    if not links:
        counter = "_No links yet — run a discovery first._"
    else:
        counter = f"**{len(chosen)} of {len(links)} links selected**"
        if len(shown) != len(links):
            counter += f" · showing {len(shown)} that match the filter"
    return box, counter


# ---- event handlers --------------------------------------------------------

def on_discover(url, depth, max_links, same_domain, prefix, robots,
                progress=gr.Progress()):
    url = (url or "").strip()
    if not url:
        raise gr.Error("Enter a URL to start from.")
    if "://" not in url:
        url = "https://" + url

    crawler = Crawler(respect_robots=robots)
    progress(0, desc="Starting…")

    def report(done, total, current):
        progress(min(done / total, 1), desc=f"Scanning {current}")

    try:
        found = crawler.discover(url, int(depth), int(max_links), same_domain,
                                 (prefix or "").strip(), on_progress=report)
    except Exception as exc:
        raise gr.Error(f"Discovery failed: {exc}")
    if len(found) <= 1 and url not in crawler.cache:
        raise gr.Error("Could not load the start page (check the URL, or robots.txt may block it).")

    links = [{"url": l.url, "text": l.text, "depth": l.depth} for l in found]
    selected = [l["url"] for l in links]
    box, counter = render(links, selected, "")
    return links, selected, crawler.cache, "", box, counter


def on_filter(query, links, selected):
    return render(links, selected, query)


def on_tick(value, query, links, selected):
    """Merge the ticks on the visible rows back into the full selection."""
    shown = {l["url"] for l in visible(links, query)}
    current = (set(selected) - shown) | set(value or [])
    new = [l["url"] for l in links if l["url"] in current]
    return (new, *render(links, new, query))


def bulk(mode):
    def handler(query, links, selected):
        shown = {l["url"] for l in visible(links, query)}
        current = set(selected)
        if mode == "all":
            current |= shown
        elif mode == "none":
            current -= shown
        else:  # invert
            current ^= shown
        new = [l["url"] for l in links if l["url"] in current]
        return (new, *render(links, new, query))
    return handler


def on_depth_select(max_d, query, links, selected):
    """Keep only links at or above a crawl depth (among the visible ones)."""
    shown = {l["url"] for l in visible(links, query)}
    current = set(selected)
    for l in links:
        if l["url"] in shown:
            (current.add if l["depth"] <= max_d else current.discard)(l["url"])
    new = [l["url"] for l in links if l["url"] in current]
    return (new, *render(links, new, query))


def on_extract(links, selected, cache, progress=gr.Progress()):
    if not selected:
        raise gr.Error("Nothing selected — tick at least one link.")
    crawler = Crawler(cache=dict(cache or {}))

    def report(done, total, current):
        progress(done / total, desc=f"Extracting {current}")

    pages = crawler.extract(list(selected), on_progress=report)
    ok = [p for p in pages if not p.error]
    failed = [p for p in pages if p.error]

    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    path = os.path.join(tempfile.mkdtemp(), f"cherrypick-{stamp}.zip")
    with open(path, "wb") as fh:
        fh.write(build_zip(pages))

    status = f"✅ Extracted **{len(ok)}** page(s)"
    if failed:
        status += f" · ⚠️ {len(failed)} failed:\n" + "\n".join(
            f"- {p.url} — {p.error}" for p in failed[:20])

    preview = combined_markdown(ok)
    if len(preview) > PREVIEW_CHARS:
        preview = preview[:PREVIEW_CHARS] + "\n\n… _(preview truncated — the zip has everything)_"
    return path, status, preview, crawler.cache


# ---- layout ----------------------------------------------------------------

CSS = """
#linklist { max-height: 480px; overflow-y: auto; }
#linklist label { width: 100%; font-size: 0.9em; }
"""

with gr.Blocks(title="Cherrypick · Bulk Content Extractor") as demo:
    links_state = gr.State([])
    selected_state = gr.State([])
    cache_state = gr.State({})

    gr.Markdown(
        "# 🍒 Cherrypick\n"
        "Bulk content extractor that asks before it downloads: **discover** the links "
        "on a site, **pick** the ones you want, then **extract** them to Markdown."
    )

    with gr.Group():
        gr.Markdown("### 1 · Discover")
        with gr.Row():
            url_in = gr.Textbox(label="Start URL", placeholder="https://example.com/docs/",
                                scale=4)
            discover_btn = gr.Button("🔎 Discover links", variant="primary", scale=1)
        with gr.Accordion("Crawl options", open=False):
            with gr.Row():
                depth_in = gr.Slider(1, 3, value=1, step=1, label="Depth",
                                     info="1 = links on the start page; 2 = also links on those pages…")
                max_in = gr.Slider(10, 1000, value=200, step=10, label="Max links to list")
            with gr.Row():
                same_in = gr.Checkbox(value=True, label="Stay on the same domain")
                robots_in = gr.Checkbox(value=True, label="Respect robots.txt")
                prefix_in = gr.Textbox(label="Only paths starting with (optional)",
                                       placeholder="/docs/")

    with gr.Group():
        gr.Markdown("### 2 · Pick")
        with gr.Row():
            filter_in = gr.Textbox(label="Filter", scale=3,
                                   placeholder="e.g.  blog 2024 -tag   (all terms must match; -word excludes)")
            depth_pick = gr.Dropdown([("Depth ≤ 0", 0), ("Depth ≤ 1", 1), ("Depth ≤ 2", 2),
                                      ("Depth ≤ 3", 3)], value=1, scale=1,
                                     label="Keep by depth")
        with gr.Row():
            all_btn = gr.Button("☑ Select shown", size="sm")
            none_btn = gr.Button("☐ Deselect shown", size="sm")
            inv_btn = gr.Button("⇄ Invert shown", size="sm")
            depth_btn = gr.Button("Apply depth rule", size="sm")
        counter = gr.Markdown("_No links yet — run a discovery first._")
        link_box = gr.CheckboxGroup(choices=[], label="Links", elem_id="linklist")

    with gr.Group():
        gr.Markdown("### 3 · Extract")
        extract_btn = gr.Button("📦 Extract selected", variant="primary")
        status_out = gr.Markdown()
        zip_out = gr.File(label="Download (.zip with one .md per page + combined.md)")
        with gr.Accordion("Preview (combined Markdown)", open=False):
            preview_out = gr.Markdown()

    pick_inputs = [filter_in, links_state, selected_state]
    pick_outputs = [selected_state, link_box, counter]

    discover_btn.click(
        on_discover,
        [url_in, depth_in, max_in, same_in, prefix_in, robots_in],
        [links_state, selected_state, cache_state, filter_in, link_box, counter],
    )
    url_in.submit(
        on_discover,
        [url_in, depth_in, max_in, same_in, prefix_in, robots_in],
        [links_state, selected_state, cache_state, filter_in, link_box, counter],
    )
    filter_in.change(on_filter, pick_inputs, [link_box, counter])
    link_box.input(on_tick, [link_box, *pick_inputs], pick_outputs)
    all_btn.click(bulk("all"), pick_inputs, pick_outputs)
    none_btn.click(bulk("none"), pick_inputs, pick_outputs)
    inv_btn.click(bulk("invert"), pick_inputs, pick_outputs)
    depth_btn.click(on_depth_select, [depth_pick, *pick_inputs], pick_outputs)
    extract_btn.click(on_extract, [links_state, selected_state, cache_state],
                      [zip_out, status_out, preview_out, cache_state])


if __name__ == "__main__":
    demo.launch(css=CSS, theme=gr.themes.Soft())

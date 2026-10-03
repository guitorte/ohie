"""Crawling and extraction logic, kept free of any UI code.

Two phases:
  discover()  walks a site breadth-first and returns every page link it finds,
              without downloading the leaf pages.
  extract()   downloads only the URLs the user kept and turns each into Markdown.
"""

from __future__ import annotations

import io
import re
import time
import zipfile
from collections import deque
from dataclasses import dataclass, field
from urllib import robotparser
from urllib.parse import urldefrag, urljoin, urlparse, urlunparse

import requests
from bs4 import BeautifulSoup
from markdownify import markdownify

USER_AGENT = "Mozilla/5.0 (compatible; Cherrypick/1.0; +bulk content extractor)"
TIMEOUT = 15

# Links ending in these are files, not pages, so they are never listed.
SKIP_EXTENSIONS = {
    ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".ico", ".bmp", ".avif",
    ".pdf", ".zip", ".gz", ".tar", ".rar", ".7z", ".exe", ".dmg", ".apk",
    ".mp3", ".mp4", ".wav", ".ogg", ".webm", ".mov", ".avi",
    ".css", ".js", ".json", ".xml", ".rss", ".woff", ".woff2", ".ttf",
    ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".csv",
}

# Upper bound on how many links one discovery may list.
MAX_LISTED = 5000

# Page furniture stripped before converting to Markdown.
NOISE_TAGS = ["script", "style", "noscript", "nav", "footer", "header", "aside",
              "form", "svg", "iframe", "button", "template"]


@dataclass
class Link:
    url: str
    text: str          # anchor text (or page title once fetched)
    depth: int
    parent: str | None = None   # page this link was first found on


@dataclass
class Page:
    url: str
    title: str = ""
    markdown: str = ""
    error: str = ""


@dataclass
class Crawler:
    """Holds the HTTP session and an HTML cache shared by both phases."""

    delay: float = 0.2
    respect_robots: bool = True
    session: requests.Session = field(default_factory=requests.Session)
    cache: dict[str, str] = field(default_factory=dict)
    _robots: dict[str, robotparser.RobotFileParser | None] = field(default_factory=dict)

    def __post_init__(self):
        self.session.headers["User-Agent"] = USER_AGENT

    # ---- helpers ---------------------------------------------------------

    def allowed(self, url: str) -> bool:
        if not self.respect_robots:
            return True
        parts = urlparse(url)
        root = f"{parts.scheme}://{parts.netloc}"
        if root not in self._robots:
            rp = robotparser.RobotFileParser()
            try:
                resp = self.session.get(root + "/robots.txt", timeout=TIMEOUT)
                rp.parse(resp.text.splitlines() if resp.ok else [])
            except requests.RequestException:
                rp = None  # unreachable robots.txt: assume allowed
            self._robots[root] = rp
        rp = self._robots[root]
        return rp is None or rp.can_fetch(USER_AGENT, url)

    def fetch(self, url: str) -> str:
        """Return the page's HTML, from cache when possible. Raises on failure."""
        if url in self.cache:
            return self.cache[url]
        if not self.allowed(url):
            raise PermissionError("blocked by robots.txt")
        resp = self.session.get(url, timeout=TIMEOUT)
        resp.raise_for_status()
        ctype = resp.headers.get("Content-Type", "")
        if "html" not in ctype and ctype:
            raise ValueError(f"not an HTML page ({ctype.split(';')[0]})")
        resp.encoding = resp.encoding or resp.apparent_encoding
        self.cache[url] = resp.text
        time.sleep(self.delay)
        return resp.text

    # ---- phase 1: discover ----------------------------------------------

    def discover(self, start_url: str, max_depth: int = 1, max_pages: int = 100,
                 same_domain: bool = True, path_prefix: str = "",
                 skip_menus: bool = True, on_progress=None) -> list[Link]:
        """Breadth-first walk from start_url, returned as a tree.

        Depth 0 is the start page itself. Pages at depth < max_depth are
        opened to harvest their links (at most max_pages of them); pages at
        max_depth are only listed. Each link remembers the page it was first
        found on, and the result is ordered parent-first, children right
        below it, so a page and its sub-pages sit together.

        With skip_menus, links found below depth 1 that repeat across many
        sibling pages (menus, "next card" bars, footers) are dropped, so each
        page keeps only the sub-pages that are really its own.
        """
        start_url = normalize(start_url)
        start_host = urlparse(start_url).netloc
        found: dict[str, Link] = {start_url: Link(start_url, "(start page)", 0)}
        seen_on: dict[str, set[str]] = {}       # link -> pages that link to it
        opened_at: dict[int, int] = {}          # depth -> pages opened there
        queue = deque([start_url])
        opened = 0

        while queue and opened < max_pages and len(found) < MAX_LISTED:
            url = queue.popleft()
            depth = found[url].depth
            if depth >= max_depth:
                continue
            opened += 1
            if on_progress:
                on_progress(opened, max_pages, url)
            try:
                html = self.fetch(url)
            except Exception:
                continue
            opened_at[depth] = opened_at.get(depth, 0) + 1
            soup = BeautifulSoup(html, "html.parser")
            if soup.title and soup.title.string and found[url].text == "(start page)":
                found[url].text = clean(soup.title.string)

            for a in soup.find_all("a", href=True):
                link = normalize(urljoin(url, a["href"]))
                if not link or link == url or not is_page(link):
                    continue
                parts = urlparse(link)
                if same_domain and parts.netloc != start_host:
                    continue
                if path_prefix and not parts.path.startswith(path_prefix):
                    continue
                seen_on.setdefault(link, set()).add(url)
                if link in found or len(found) >= MAX_LISTED:
                    continue
                text = clean(a.get_text(" ")) or a.get("title", "") or parts.path or link
                found[link] = Link(link, text[:120], depth + 1, parent=url)
                queue.append(link)

        if skip_menus:
            found = drop_menu_links(found, seen_on, opened_at)
        return tree_order(found, start_url)

    # ---- phase 2: extract -----------------------------------------------

    def extract(self, urls: list[str], on_progress=None) -> list[Page]:
        pages = []
        for i, url in enumerate(urls):
            if on_progress:
                on_progress(i, len(urls), url)
            try:
                title, md = html_to_markdown(self.fetch(url), url)
                pages.append(Page(url, title or url, md))
            except Exception as exc:
                pages.append(Page(url, url, error=str(exc) or exc.__class__.__name__))
        return pages


# ---- pure functions ------------------------------------------------------

def drop_menu_links(found: dict[str, Link], seen_on: dict[str, set[str]],
                    opened_at: dict[int, int]) -> dict[str, Link]:
    """Remove depth-2+ links that appear on many pages of the level above.

    A link printed on 3+ sibling pages and on at least a quarter of them is
    site furniture, not a sub-page. Anything found only through such a link
    goes too. Depth-1 links (from the start page) are always kept.
    """
    def is_menu(link: Link) -> bool:
        if link.depth < 2:
            return False
        siblings = opened_at.get(link.depth - 1, 0)
        hits = len(seen_on.get(link.url, ()))
        return hits >= 3 and hits >= 0.25 * siblings

    kept: dict[str, Link] = {}
    for url, link in found.items():          # insertion order = parents first
        if is_menu(link) or (link.parent and link.parent not in kept):
            continue
        kept[url] = link
    return kept


def tree_order(found: dict[str, Link], root: str) -> list[Link]:
    """Depth-first order: each page followed by its own sub-pages."""
    children: dict[str, list[Link]] = {}
    for link in found.values():
        if link.parent:
            children.setdefault(link.parent, []).append(link)
    out: list[Link] = []
    stack = [found[root]]
    while stack:
        link = stack.pop()
        out.append(link)
        stack.extend(reversed(children.get(link.url, [])))
    return out


def normalize(url: str) -> str:
    """Drop #fragments, lowercase scheme/host, and reject non-http links."""
    url, _ = urldefrag(url.strip())
    parts = urlparse(url)
    if parts.scheme not in ("http", "https") or not parts.netloc:
        return ""
    return urlunparse(parts._replace(scheme=parts.scheme.lower(),
                                     netloc=parts.netloc.lower(),
                                     path=parts.path or "/"))


def is_page(url: str) -> bool:
    path = urlparse(url).path.lower()
    dot = path.rfind(".")
    return dot == -1 or path[dot:] not in SKIP_EXTENSIONS


def clean(text: str) -> str:
    return re.sub(r"\s+", " ", text or "").strip()


def html_to_markdown(html: str, base_url: str = "") -> tuple[str, str]:
    soup = BeautifulSoup(html, "html.parser")
    title = clean(soup.title.string) if soup.title and soup.title.string else ""
    for tag in soup(NOISE_TAGS):
        tag.decompose()
    if base_url:  # absolute links keep working once the Markdown leaves the site
        for tag, attr in (("a", "href"), ("img", "src")):
            for el in soup.find_all(tag, **{attr: True}):
                el[attr] = urljoin(base_url, el[attr])
    body = soup.find("main") or soup.find("article") or soup.body or soup
    md = markdownify(str(body), heading_style="ATX", bullets="-")
    md = re.sub(r"[ \t]+\n", "\n", md)
    md = re.sub(r"\n{3,}", "\n\n", md).strip()
    return title, md


def slugify(url: str) -> str:
    parts = urlparse(url)
    slug = parts.path.strip("/").replace("/", "__")
    if parts.query:
        slug += "_" + parts.query
    slug = re.sub(r"[^A-Za-z0-9._-]+", "-", slug).strip("-")
    slug = re.sub(r"\.(s?html?|php|aspx?)$", "", slug)
    return slug[:150] or "index"


def build_zip(pages: list[Page], folders: dict[str, str] | None = None) -> bytes:
    """One Markdown file per page, a combined file, and an index.

    folders maps a page URL to a sub-folder of pages/ (used to keep each
    page together with its sub-pages); pages without one go in pages/.
    """
    folders = folders or {}
    buf = io.BytesIO()
    used: set[str] = set()
    index = ["# Index\n"]
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
        for page in pages:
            if page.error:
                index.append(f"- ✗ {page.url} — {page.error}")
                continue
            folder = folders.get(page.url, "")
            name = "pages/" + (folder + "/" if folder else "") + slugify(page.url)
            while name + ".md" in used:
                name += "_"
            used.add(name + ".md")
            zf.writestr(name + ".md", page_document(page))
            index.append(f"- [{page.title}]({name}.md) — {page.url}")
        zf.writestr("combined.md", combined_markdown(pages))
        zf.writestr("index.md", "\n".join(index) + "\n")
    return buf.getvalue()


def page_document(page: Page) -> str:
    title = page.title.replace('"', "'")
    return f'---\ntitle: "{title}"\nsource: {page.url}\n---\n\n{page.markdown}\n'


def combined_markdown(pages: list[Page]) -> str:
    parts = []
    for page in pages:
        if page.error:
            continue
        parts.append(f"# {page.title}\n\n<{page.url}>\n\n{page.markdown}")
    return "\n\n---\n\n".join(parts) + "\n"

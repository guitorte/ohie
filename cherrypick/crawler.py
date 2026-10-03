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

# Page furniture stripped before converting to Markdown.
NOISE_TAGS = ["script", "style", "noscript", "nav", "footer", "header", "aside",
              "form", "svg", "iframe", "button", "template"]


@dataclass
class Link:
    url: str
    text: str          # anchor text (or page title once fetched)
    depth: int


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

    def discover(self, start_url: str, max_depth: int = 1, max_links: int = 200,
                 same_domain: bool = True, path_prefix: str = "",
                 on_progress=None) -> list[Link]:
        """Breadth-first walk from start_url.

        Depth 0 is the start page itself. Pages at depth < max_depth are
        downloaded to harvest their links; pages at max_depth are only listed.
        """
        start_url = normalize(start_url)
        start_host = urlparse(start_url).netloc
        found: dict[str, Link] = {start_url: Link(start_url, "(start page)", 0)}
        queue = deque([start_url])

        while queue and len(found) < max_links:
            url = queue.popleft()
            depth = found[url].depth
            if depth >= max_depth:
                continue
            if on_progress:
                on_progress(len(found), max_links, url)
            try:
                html = self.fetch(url)
            except Exception:
                continue
            soup = BeautifulSoup(html, "html.parser")
            if soup.title and soup.title.string and found[url].text == "(start page)":
                found[url].text = clean(soup.title.string)

            for a in soup.find_all("a", href=True):
                link = normalize(urljoin(url, a["href"]))
                if not link or link in found or not is_page(link):
                    continue
                parts = urlparse(link)
                if same_domain and parts.netloc != start_host:
                    continue
                if path_prefix and not parts.path.startswith(path_prefix):
                    continue
                text = clean(a.get_text(" ")) or a.get("title", "") or parts.path or link
                found[link] = Link(link, text[:120], depth + 1)
                queue.append(link)
                if len(found) >= max_links:
                    break

        return list(found.values())

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
    slug = (parts.netloc + parts.path).strip("/").replace("/", "__")
    if parts.query:
        slug += "_" + parts.query
    slug = re.sub(r"[^A-Za-z0-9._-]+", "-", slug).strip("-")
    return slug[:150] or "page"


def build_zip(pages: list[Page]) -> bytes:
    """One Markdown file per page, a combined file, and an index."""
    buf = io.BytesIO()
    used: set[str] = set()
    index = ["# Index\n"]
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
        for page in pages:
            if page.error:
                index.append(f"- ✗ {page.url} — {page.error}")
                continue
            name = slugify(page.url)
            while name + ".md" in used:
                name += "_"
            used.add(name + ".md")
            zf.writestr(f"pages/{name}.md", page_document(page))
            index.append(f"- [{page.title}](pages/{name}.md) — {page.url}")
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

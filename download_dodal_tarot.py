#!/usr/bin/env python3
"""
Download Jean Dodal Tarot deck images from Wikimedia Commons.

Jean Dodal was a cardmaker in Lyon c.1701-1715. His deck is one of the
best-preserved examples of the early Tarot de Marseille tradition.
All images are in the public domain.

Usage:
    python download_dodal_tarot.py [--output-dir ./dodal_tarot] [--major-only]
"""

import argparse
import json
import time
import urllib.request
import urllib.parse
from pathlib import Path

API_BASE = "https://commons.wikimedia.org/w/api.php"
CATEGORY = "Tarot de Marseille - Jean Dodal"
USER_AGENT = "DodalTarotDownloader/1.0 (educational/research use)"

TRUMP_NAMES = {
    "Fool":    ("00", "The Fool - Le Mat"),
    "I":       ("01", "The Magician - Le Bateleur"),
    "II":      ("02", "The High Priestess - La Papesse"),
    "III":     ("03", "The Empress - L'Impératrice"),
    "IIII":    ("04", "The Emperor - L'Empereur"),
    "V":       ("05", "The Pope - Le Pape"),
    "VI":      ("06", "The Lovers - L'Amoureux"),
    "VII":     ("07", "The Chariot - Le Chariot"),
    "VIII":    ("08", "Justice - La Justice"),
    "VIIII":   ("09", "The Hermit - L'Hermite"),
    "X":       ("10", "Wheel of Fortune - La Roue de Fortune"),
    "XI":      ("11", "Strength - La Force"),
    "XII":     ("12", "The Hanged Man - Le Pendu"),
    "XIII":    ("13", "Death - La Mort"),
    "XIIII":   ("14", "Temperance - La Tempérance"),
    "XV":      ("15", "The Devil - Le Diable"),
    "XVI":     ("16", "The Tower - La Maison Dieu"),
    "XVII":    ("17", "The Star - L'Étoile"),
    "XVIII":   ("18", "The Moon - La Lune"),
    "XVIIII":  ("19", "The Sun - Le Soleil"),
    "XX":      ("20", "Judgement - Le Jugement"),
    "XXI":     ("21", "The World - Le Monde"),
}


def api_get(params: dict) -> dict:
    params["format"] = "json"
    url = API_BASE + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def get_category_files(category: str) -> list[dict]:
    """Return all File: members of a Commons category (handles continuation)."""
    files = []
    params = {
        "action": "query",
        "list": "categorymembers",
        "cmtitle": f"Category:{category}",
        "cmtype": "file",
        "cmlimit": "500",
    }
    while True:
        data = api_get(params)
        files.extend(data["query"]["categorymembers"])
        cont = data.get("continue")
        if not cont:
            break
        params.update(cont)
    return files


def get_image_urls(titles: list[str]) -> dict[str, str]:
    """Batch-fetch the full-resolution URL for each File: title."""
    urls = {}
    # API allows up to 50 titles per request
    for i in range(0, len(titles), 50):
        batch = titles[i : i + 50]
        data = api_get({
            "action": "query",
            "titles": "|".join(batch),
            "prop": "imageinfo",
            "iiprop": "url|size|extmetadata",
        })
        for page in data["query"]["pages"].values():
            if "imageinfo" in page:
                info = page["imageinfo"][0]
                urls[page["title"]] = {
                    "url": info["url"],
                    "width": info.get("width"),
                    "height": info.get("height"),
                }
    return urls


def classify_card(title: str) -> tuple[str, str]:
    """Return (subfolder, clean_name) for a given Wikimedia file title."""
    # Strip the "File:" namespace prefix, then drop the extension
    bare = title.removeprefix("File:")
    name = Path(bare).stem  # e.g. "Jean Dodal Tarot trump XIV"
    if "trump" in name.lower():
        key = name.split("trump ")[-1].strip()
        if key in TRUMP_NAMES:
            number, label = TRUMP_NAMES[key]
            return "major_arcana", f"{number} - {label}"
        return "major_arcana", name
    # Minor arcana: strip the redundant "Jean Dodal Tarot " prefix for brevity
    clean = name.removeprefix("Jean Dodal Tarot ").strip()
    return "minor_arcana", clean


def download_file(url: str, dest: Path, retries: int = 3) -> bool:
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists():
        print(f"  [skip] {dest.name} already exists")
        return True
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(1, retries + 1):
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                dest.write_bytes(resp.read())
            print(f"  [ok]   {dest.name}")
            return True
        except Exception as exc:
            print(f"  [err]  {dest.name} attempt {attempt}: {exc}")
            if attempt < retries:
                time.sleep(2 ** attempt)
    return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", default="./dodal_tarot")
    parser.add_argument("--major-only", action="store_true",
                        help="Download only the 22 major arcana cards")
    args = parser.parse_args()

    out = Path(args.output_dir)
    out.mkdir(parents=True, exist_ok=True)

    print(f"Querying Wikimedia Commons category: {CATEGORY!r} ...")
    files = get_category_files(CATEGORY)
    print(f"  Found {len(files)} file(s) in category.")

    titles = [f["title"] for f in files]
    print("Fetching image metadata ...")
    image_info = get_image_urls(titles)

    metadata = []
    failed = []

    for title, info in image_info.items():
        subfolder, clean_name = classify_card(title)

        if args.major_only and subfolder != "major_arcana":
            continue

        ext = Path(urllib.parse.urlparse(info["url"]).path).suffix
        filename = f"{clean_name}{ext}"
        dest = out / subfolder / filename

        success = download_file(info["url"], dest)
        entry = {
            "wikimedia_title": title,
            "local_path": str(dest.relative_to(out)),
            "subfolder": subfolder,
            "label": clean_name,
            "url": info["url"],
            "width": info.get("width"),
            "height": info.get("height"),
            "downloaded": success,
        }
        metadata.append(entry)
        if not success:
            failed.append(title)
        time.sleep(0.3)  # be polite to the API

    meta_path = out / "metadata.json"
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)

    print(f"\nDone. {len(metadata) - len(failed)}/{len(metadata)} files downloaded.")
    print(f"Metadata saved to {meta_path}")
    if failed:
        print(f"Failed ({len(failed)}): {failed}")


if __name__ == "__main__":
    main()

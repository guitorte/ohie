#!/usr/bin/env python3
"""
Download Jean Dodal Tarot deck images from two public-domain sources:

  --source wikimedia  Wikimedia Commons category (22 major arcana + a few minor)
  --source bnf        Bibliothèque nationale de France Gallica IIIF scan
                      ark:/12148/btv1b10537343h  — the full 78-card deck
  --source both       (default) Wikimedia first, then BnF for remaining cards

Jean Dodal was a cardmaker in Lyon c.1701-1715.
All images are in the public domain.

Usage:
    python download_dodal_tarot.py [--output-dir ./dodal_tarot] [--source both]
"""

import argparse
import json
import time
import urllib.request
import urllib.error
import urllib.parse
from pathlib import Path

# ── Wikimedia Commons ────────────────────────────────────────────────────────
WM_API       = "https://commons.wikimedia.org/w/api.php"
WM_CATEGORY  = "Tarot de Marseille - Jean Dodal"

# ── Bibliothèque nationale de France (Gallica IIIF) ─────────────────────────
BNF_ARK          = "btv1b10537343h"
BNF_IIIF_BASE    = f"https://gallica.bnf.fr/iiif/ark:/12148/{BNF_ARK}"
BNF_MANIFEST_URL = f"{BNF_IIIF_BASE}/manifest.json"
# Full-resolution image URL template  (f-index is 1-based)
BNF_IMAGE_URL    = BNF_IIIF_BASE + "/f{n}/full/full/0/native.jpg"

USER_AGENT = "DodalTarotDownloader/1.0 (educational/research use)"

# ── Standard Tarot de Marseille ordering (78 cards) ─────────────────────────
# Used to label BnF pages, which Gallica does not annotate individually.
# The BnF Dodal scan (btv1b10537343h) presents cards in this order:
#   major arcana first (The Fool last, as was common), then suits in order
#   batons → coupes → épées → deniers, each Ace→10 then Valet/Cavalier/Reine/Roi.
# Adjust FIRST_CARD_PAGE if the manifest has a cover/title page before the cards.
FIRST_CARD_PAGE = 1   # set to 2 if page 1 is a cover image

CARD_ORDER = (
    # ── Major Arcana ──────────────────────────────────────────────────────
    [("major_arcana", f"{n:02d} - {label}") for n, label in [
        ( 1, "The Magician - Le Bateleur"),
        ( 2, "The High Priestess - La Papesse"),
        ( 3, "The Empress - L'Impératrice"),
        ( 4, "The Emperor - L'Empereur"),
        ( 5, "The Pope - Le Pape"),
        ( 6, "The Lovers - L'Amoureux"),
        ( 7, "The Chariot - Le Chariot"),
        ( 8, "Justice - La Justice"),
        ( 9, "The Hermit - L'Hermite"),
        (10, "Wheel of Fortune - La Roue de Fortune"),
        (11, "Strength - La Force"),
        (12, "The Hanged Man - Le Pendu"),
        (13, "Death - La Mort"),
        (14, "Temperance - La Tempérance"),
        (15, "The Devil - Le Diable"),
        (16, "The Tower - La Maison Dieu"),
        (17, "The Star - L'Étoile"),
        (18, "The Moon - La Lune"),
        (19, "The Sun - Le Soleil"),
        (20, "Judgement - Le Jugement"),
        (21, "The World - Le Monde"),
        ( 0, "The Fool - Le Mat"),
    ]] +
    # ── Minor Arcana — Batons (Wands) ─────────────────────────────────────
    [("minor_arcana", f"batons {r:02d} - {label}") for r, label in [
        (1,"Ace of Batons"),(2,"2 of Batons"),(3,"3 of Batons"),
        (4,"4 of Batons"),(5,"5 of Batons"),(6,"6 of Batons"),
        (7,"7 of Batons"),(8,"8 of Batons"),(9,"9 of Batons"),
        (10,"10 of Batons"),(11,"Valet of Batons"),(12,"Knight of Batons"),
        (13,"Queen of Batons"),(14,"King of Batons"),
    ]] +
    # ── Minor Arcana — Coupes (Cups) ──────────────────────────────────────
    [("minor_arcana", f"coupes {r:02d} - {label}") for r, label in [
        (1,"Ace of Cups"),(2,"2 of Cups"),(3,"3 of Cups"),
        (4,"4 of Cups"),(5,"5 of Cups"),(6,"6 of Cups"),
        (7,"7 of Cups"),(8,"8 of Cups"),(9,"9 of Cups"),
        (10,"10 of Cups"),(11,"Valet of Cups"),(12,"Knight of Cups"),
        (13,"Queen of Cups"),(14,"King of Cups"),
    ]] +
    # ── Minor Arcana — Épées (Swords) ─────────────────────────────────────
    [("minor_arcana", f"epees {r:02d} - {label}") for r, label in [
        (1,"Ace of Swords"),(2,"2 of Swords"),(3,"3 of Swords"),
        (4,"4 of Swords"),(5,"5 of Swords"),(6,"6 of Swords"),
        (7,"7 of Swords"),(8,"8 of Swords"),(9,"9 of Swords"),
        (10,"10 of Swords"),(11,"Valet of Swords"),(12,"Knight of Swords"),
        (13,"Queen of Swords"),(14,"King of Swords"),
    ]] +
    # ── Minor Arcana — Deniers (Coins/Pentacles) ──────────────────────────
    [("minor_arcana", f"deniers {r:02d} - {label}") for r, label in [
        (1,"Ace of Coins"),(2,"2 of Coins"),(3,"3 of Coins"),
        (4,"4 of Coins"),(5,"5 of Coins"),(6,"6 of Coins"),
        (7,"7 of Coins"),(8,"8 of Coins"),(9,"9 of Coins"),
        (10,"10 of Coins"),(11,"Valet of Coins"),(12,"Knight of Coins"),
        (13,"Queen of Coins"),(14,"King of Coins"),
    ]]
)

# ── Wikimedia trump key → (number, label) ────────────────────────────────────
TRUMP_NAMES = {
    # Decimal keys — actual Wikimedia filenames use "trump 01", "trump 02", …
    "Fool":   ("00", "The Fool - Le Mat"),
    "01":     ("01", "The Magician - Le Bateleur"),
    "02":     ("02", "The High Priestess - La Papesse"),
    "03":     ("03", "The Empress - L'Impératrice"),
    "04":     ("04", "The Emperor - L'Empereur"),
    "05":     ("05", "The Pope - Le Pape"),
    "06":     ("06", "The Lovers - L'Amoureux"),
    "07":     ("07", "The Chariot - Le Chariot"),
    "08":     ("08", "Justice - La Justice"),
    "09":     ("09", "The Hermit - L'Hermite"),
    "10":     ("10", "Wheel of Fortune - La Roue de Fortune"),
    "11":     ("11", "Strength - La Force"),
    "12":     ("12", "The Hanged Man - Le Pendu"),
    "13":     ("13", "Death - La Mort"),
    "14":     ("14", "Temperance - La Tempérance"),
    "15":     ("15", "The Devil - Le Diable"),
    "16":     ("16", "The Tower - La Maison Dieu"),
    "17":     ("17", "The Star - L'Étoile"),
    "18":     ("18", "The Moon - La Lune"),
    "19":     ("19", "The Sun - Le Soleil"),
    "20":     ("20", "Judgement - Le Jugement"),
    "21":     ("21", "The World - Le Monde"),
    # Roman-numeral keys — fallback
    "I":      ("01", "The Magician - Le Bateleur"),
    "II":     ("02", "The High Priestess - La Papesse"),
    "III":    ("03", "The Empress - L'Impératrice"),
    "IIII":   ("04", "The Emperor - L'Empereur"),
    "V":      ("05", "The Pope - Le Pape"),
    "VI":     ("06", "The Lovers - L'Amoureux"),
    "VII":    ("07", "The Chariot - Le Chariot"),
    "VIII":   ("08", "Justice - La Justice"),
    "VIIII":  ("09", "The Hermit - L'Hermite"),
    "X":      ("10", "Wheel of Fortune - La Roue de Fortune"),
    "XI":     ("11", "Strength - La Force"),
    "XII":    ("12", "The Hanged Man - Le Pendu"),
    "XIII":   ("13", "Death - La Mort"),
    "XIIII":  ("14", "Temperance - La Tempérance"),
    "XV":     ("15", "The Devil - Le Diable"),
    "XVI":    ("16", "The Tower - La Maison Dieu"),
    "XVII":   ("17", "The Star - L'Étoile"),
    "XVIII":  ("18", "The Moon - La Lune"),
    "XVIIII": ("19", "The Sun - Le Soleil"),
    "XX":     ("20", "Judgement - Le Jugement"),
    "XXI":    ("21", "The World - Le Monde"),
}


# ── Shared helpers ────────────────────────────────────────────────────────────

def http_get(url: str, *, as_json: bool = False, params: dict | None = None):
    if params:
        url = url + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as resp:
        raw = resp.read()
    return json.loads(raw.decode("utf-8")) if as_json else raw


def download_file(url: str, dest: Path, retries: int = 4) -> bool:
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists():
        print(f"  [skip] {dest.name}")
        return True
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(1, retries + 1):
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                dest.write_bytes(resp.read())
            print(f"  [ok]   {dest.name}")
            return True
        except urllib.error.HTTPError as exc:
            if exc.code == 429:
                wait = int(exc.headers.get("Retry-After", 60))
                print(f"  [429]  {dest.name} — waiting {wait}s (attempt {attempt}/{retries})")
                time.sleep(wait)
            else:
                print(f"  [err]  {dest.name} attempt {attempt}: HTTP {exc.code}")
                if attempt < retries:
                    time.sleep(2 ** attempt)
        except Exception as exc:
            print(f"  [err]  {dest.name} attempt {attempt}: {exc}")
            if attempt < retries:
                time.sleep(2 ** attempt)
    return False


# ── Wikimedia Commons source ──────────────────────────────────────────────────

def wm_classify(title: str) -> tuple[str, str]:
    bare = title.removeprefix("File:")
    name = Path(bare).stem
    if "trump" in name.lower():
        key = name.split("trump ")[-1].strip()
        if key in TRUMP_NAMES:
            number, label = TRUMP_NAMES[key]
            return "major_arcana", f"{number} - {label}"
        return "major_arcana", name
    clean = name.removeprefix("Jean Dodal Tarot ").strip()
    return "minor_arcana", clean


def download_wikimedia(out: Path, major_only: bool) -> tuple[list[dict], set[str]]:
    """Download from Wikimedia Commons. Returns (metadata_list, set_of_local_paths)."""
    print(f"\n── Wikimedia Commons ────────────────────────────────")
    print(f"Querying category: {WM_CATEGORY!r} ...")
    files = []
    params: dict = {
        "action": "query", "list": "categorymembers",
        "cmtitle": f"Category:{WM_CATEGORY}", "cmtype": "file",
        "cmlimit": "500", "format": "json",
    }
    while True:
        data = http_get(WM_API, as_json=True, params=params)
        files.extend(data["query"]["categorymembers"])
        if not (cont := data.get("continue")):
            break
        params.update(cont)
    print(f"  Found {len(files)} file(s).")

    # Batch-fetch image URLs
    image_info: dict[str, dict] = {}
    titles = [f["title"] for f in files]
    for i in range(0, len(titles), 50):
        batch = titles[i : i + 50]
        data = http_get(WM_API, as_json=True, params={
            "action": "query", "titles": "|".join(batch),
            "prop": "imageinfo", "iiprop": "url|size", "format": "json",
        })
        for page in data["query"]["pages"].values():
            if "imageinfo" in page:
                info = page["imageinfo"][0]
                image_info[page["title"]] = {
                    "url": info["url"],
                    "width": info.get("width"),
                    "height": info.get("height"),
                }

    metadata = []
    downloaded_labels: set[str] = set()
    for title, info in image_info.items():
        subfolder, clean_name = wm_classify(title)
        if major_only and subfolder != "major_arcana":
            continue
        ext = Path(urllib.parse.urlparse(info["url"]).path).suffix
        dest = out / subfolder / f"{clean_name}{ext}"
        success = download_file(info["url"], dest)
        if success:
            downloaded_labels.add(clean_name)
        metadata.append({
            "source": "wikimedia",
            "wikimedia_title": title,
            "local_path": str(dest.relative_to(out)),
            "subfolder": subfolder,
            "label": clean_name,
            "url": info["url"],
            "width": info.get("width"),
            "height": info.get("height"),
            "downloaded": success,
        })
        time.sleep(1.5)

    return metadata, downloaded_labels


# ── BnF Gallica IIIF source ───────────────────────────────────────────────────

def download_bnf(out: Path, skip_labels: set[str]) -> list[dict]:
    """
    Download from BnF Gallica via IIIF.
    skip_labels: card labels already downloaded from Wikimedia (skip duplicates).
    """
    print(f"\n── BnF Gallica (IIIF) ───────────────────────────────")
    print(f"Fetching IIIF manifest: {BNF_MANIFEST_URL} ...")
    try:
        manifest = http_get(BNF_MANIFEST_URL, as_json=True)
    except Exception as exc:
        print(f"  [err] Could not fetch manifest: {exc}")
        return []

    canvases = manifest.get("sequences", [{}])[0].get("canvases", [])
    print(f"  Manifest has {len(canvases)} page(s).")

    # Print the first few labels to help the user verify page ordering
    print("  First 5 canvas labels:", [c.get("label","?") for c in canvases[:5]])

    metadata = []
    card_idx = 0  # index into CARD_ORDER

    for page_num, canvas in enumerate(canvases, start=1):
        # Skip pages before the cards start (cover page etc.)
        if page_num < FIRST_CARD_PAGE:
            print(f"  [skip] page {page_num} (pre-card page)")
            continue

        if card_idx >= len(CARD_ORDER):
            print(f"  [info] All {len(CARD_ORDER)} cards mapped; ignoring remaining pages.")
            break

        subfolder, label = CARD_ORDER[card_idx]
        card_idx += 1

        # Skip if already downloaded from Wikimedia
        if label in skip_labels:
            print(f"  [dup]  {label} — already have from Wikimedia")
            continue

        # Try to get the image URL from the canvas itself; fall back to template
        try:
            img_url = canvas["images"][0]["resource"]["@id"]
            # Request full resolution (some Gallica URLs embed a size limit)
            img_url = img_url.replace("/full/304,/", "/full/full/")
        except (KeyError, IndexError):
            img_url = BNF_IMAGE_URL.format(n=page_num)

        dest = out / subfolder / f"{label}.jpg"
        success = download_file(img_url, dest)
        metadata.append({
            "source": "bnf_gallica",
            "bnf_page": page_num,
            "canvas_label": canvas.get("label", ""),
            "local_path": str(dest.relative_to(out)),
            "subfolder": subfolder,
            "label": label,
            "url": img_url,
            "downloaded": success,
        })
        time.sleep(1.5)

    return metadata


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--output-dir", default="./dodal_tarot")
    parser.add_argument("--source", choices=["wikimedia", "bnf", "both"],
                        default="both",
                        help="Which source(s) to download from (default: both)")
    parser.add_argument("--major-only", action="store_true",
                        help="Download only the 22 major arcana (Wikimedia source only)")
    parser.add_argument("--bnf-first-page", type=int, default=FIRST_CARD_PAGE,
                        help=f"IIIF page index where cards start (default: {FIRST_CARD_PAGE}). "
                             "Set to 2 if the manifest has a cover page before the first card.")
    args = parser.parse_args()

    global FIRST_CARD_PAGE
    FIRST_CARD_PAGE = args.bnf_first_page

    out = Path(args.output_dir)
    out.mkdir(parents=True, exist_ok=True)

    all_metadata: list[dict] = []
    downloaded_labels: set[str] = set()

    if args.source in ("wikimedia", "both"):
        wm_meta, downloaded_labels = download_wikimedia(out, args.major_only)
        all_metadata.extend(wm_meta)

    if args.source in ("bnf", "both") and not args.major_only:
        bnf_meta = download_bnf(out, skip_labels=downloaded_labels)
        all_metadata.extend(bnf_meta)

    meta_path = out / "metadata.json"
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(all_metadata, f, ensure_ascii=False, indent=2)

    ok = sum(1 for e in all_metadata if e.get("downloaded"))
    print(f"\nDone. {ok}/{len(all_metadata)} files downloaded.")
    print(f"Metadata saved to {meta_path}")
    failed = [e["label"] for e in all_metadata if not e.get("downloaded")]
    if failed:
        print(f"Failed ({len(failed)}): {failed}")


if __name__ == "__main__":
    main()

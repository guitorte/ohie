# World Cup 2022 — Backs & Wingers dashboard

A small, dependency-free, mobile-first dashboard for exploring player performance
at the 2022 FIFA World Cup, built around **left/right full-backs and wingers**.

Open `index.html` (served over HTTP) — it loads `data.json` and runs entirely in
the browser. No build step, no server-side code, works on GitHub Pages.

## The two datasets (assessment)

| | swaptr/`fifa-world-cup-2022-player-data` | rhugvedbhojane/`...players-statistics` |
|---|---|---|
| Source | **FBref** (Opta-style event stats) | Transfermarkt-ish + FBref summary |
| Shape | 9 outfield CSVs, **680 players**, one file per stat family (passing, possession, defense, shooting, GCA, misc, …) — ~30 columns each | single file, **813 rows**, 18 columns |
| Strength | Deep per-90 metrics: crosses into box, progressive passes, take-ons, touches by zone, tackles/interceptions, xG/xA, shot- & goal-creating actions | Squad metadata: **jersey number**, boot/brand, kit sponsor, FIFA ranking, DOB |
| Weakness | Position is only **GK / DF / MF / FW** | Coarser stats (only a handful of per-90s), some values known to be wrong; position also only GK/DF/MF/FW |

**Key finding that shaped the design:** *neither* dataset labels a player as a
left-back, right-back, left-winger, etc. Both only carry the broad FBref bucket
(GK/DF/MF/FW), and left/right cannot be derived from these stats. So the dashboard
treats the granular **role as an editable field** you assign and refine, while it
does the heavy lifting on the performance numbers.

This project uses the **FBref set as the spine** (it's far richer for flank play)
and joins the second dataset on player name to add jersey number, boots and FIFA
ranking (610/680 matched).

## What the dashboard does

- **Default view = Backs & Wingers**, sorted by crosses into the box per 90.
- Quick filters: Full-backs · Wingers · Defenders · Midfield · Forwards · Everyone.
- Search (player / club / country), team filter, minimum-minutes slider, and a
  **Compare** menu to sort/score by any of 29 metrics (Attack · Wide · Defend · Security).
- Tap a player → grouped **percentile bars** (vs qualified outfield players, ≥ 1.5
  full games) with per-90 values, plus jersey/club/boots/FIFA-rank.
- **Editable**: each player has a role dropdown (CB, LB, RB, LWB, RWB, LM, RM, LW,
  RW, …) seeded with an auto-suggestion, plus a free-text note. Edits save to
  `localStorage` and can be **exported/imported** as JSON or reset.

The auto role seed is a transparent heuristic (in `build_data.py`): defenders with
high wide-progression percentiles → `FB`, otherwise `CB`; forwards leaning on
take-ons/crosses → `W`, otherwise `ST`. It only suggests — you assign left/right.

## Rebuilding the data

```bash
cd wc2022-flanks
python3 build_data.py    # stdlib only, no pip install needed
```

Reads the CSVs in `data/` and writes `data.json`. Source CSVs are committed under
`data/` for provenance and reproducibility.

### Credit
Stats: [FBref.com](https://fbref.com) via
[Kaggle / swaptr](https://www.kaggle.com/datasets/swaptr/fifa-world-cup-2022-player-data).
Squad metadata via
[Kaggle / rhugvedbhojane](https://www.kaggle.com/datasets/rhugvedbhojane/fifa-world-cup-2022-players-statistics).

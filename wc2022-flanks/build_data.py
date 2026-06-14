#!/usr/bin/env python3
"""
Build a single combined dataset for the WC 2022 flank-players dashboard.

Inputs (data/):
  - FBref multi-file player data (swaptr/fifa-world-cup-2022-player-data):
      player_stats.csv, player_passing.csv, player_passing_types.csv,
      player_possession.csv, player_defense.csv, player_misc.csv,
      player_gca.csv, player_shooting.csv, player_playingtime.csv
  - players_stats_rhugvedbhojane.csv (rhugvedbhojane/...players-statistics)
      used only to enrich jersey number + boot/brand + FIFA ranking.

Output:
  - data.json  (consumed by index.html, no server needed)

No third-party deps: standard library only so it runs anywhere.
"""
import csv, json, os, unicodedata, datetime

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "data")


def fnum(v):
    """Parse an FBref cell to float, or None when blank/non-numeric."""
    if v is None:
        return None
    v = v.strip().replace("%", "")
    if v in ("", "-", "NA", "nan"):
        return None
    try:
        return float(v)
    except ValueError:
        return None


def load_csv(name):
    with open(os.path.join(DATA, name), newline="", encoding="utf-8") as fh:
        return list(csv.DictReader(fh))


def key(row):
    return (row["player"].strip(), row["team"].strip())


def norm(s):
    s = unicodedata.normalize("NFKD", s or "")
    s = "".join(c for c in s if not unicodedata.combining(c))
    return "".join(c for c in s.lower() if c.isalnum() or c == " ").strip()


# ---------------------------------------------------------------- load FBref
stats = load_csv("player_stats.csv")
passing = {key(r): r for r in load_csv("player_passing.csv")}
ptypes = {key(r): r for r in load_csv("player_passing_types.csv")}
poss = {key(r): r for r in load_csv("player_possession.csv")}
defense = {key(r): r for r in load_csv("player_defense.csv")}
misc = {key(r): r for r in load_csv("player_misc.csv")}
gca = {key(r): r for r in load_csv("player_gca.csv")}
shoot = {key(r): r for r in load_csv("player_shooting.csv")}
ptime = {key(r): r for r in load_csv("player_playingtime.csv")}

# ------------------------------------------------ enrichment (second dataset)
enrich = {}
for r in load_csv("players_stats_rhugvedbhojane.csv"):
    # headers carry stray spaces; fetch defensively
    g = {k.strip(): (v.strip() if isinstance(v, str) else v) for k, v in r.items()}
    nm = norm(g.get("Player Name", ""))
    if not nm:
        continue
    enrich[nm] = {
        "jersey": g.get("National Team Jersey Number") or None,
        "boots": g.get("Brand Sponsor/Brand Used") or None,
        "fifa_rank": fnum(g.get("FIFA Ranking")),
    }

# ---------------------------------------------------------- metric catalogue
# (key, label, source dict, column, group, per90?, higher_is_better)
# source 's' = the stats base row itself.
METRICS = [
    # Attacking output
    ("goals",        "Goals",              "s",  "goals",                       "Attack",  True,  True),
    ("assists",      "Assists",            "s",  "assists",                     "Attack",  True,  True),
    ("xg",           "xG",                 "s",  "xg",                          "Attack",  True,  True),
    ("xa",           "xA",                 "s",  "xg_assist",                   "Attack",  True,  True),
    ("sca",          "Shot-creating acts", gca,  "sca",                         "Attack",  True,  True),
    ("gca",          "Goal-creating acts", gca,  "gca",                         "Attack",  True,  True),
    ("shots",        "Shots",              shoot,"shots",                       "Attack",  True,  True),
    # Wide / progression play -- most relevant to backs & wingers
    ("crosses",      "Crosses",            misc, "crosses",                     "Wide",    True,  True),
    ("crs_pen",      "Crosses into box",   passing,"crosses_into_penalty_area", "Wide",    True,  True),
    ("pass_pen",     "Passes into box",    passing,"passes_into_penalty_area",  "Wide",    True,  True),
    ("prog_pass",    "Progressive passes", passing,"progressive_passes",        "Wide",    True,  True),
    ("prog_rec",     "Prog. passes rec'd", poss, "progressive_passes_received", "Wide",    True,  True),
    ("take_ons",     "Take-ons won",       poss, "dribbles_completed",          "Wide",    True,  True),
    ("take_on_pct",  "Take-on success %",  poss, "dribbles_completed_pct",      "Wide",    False, True),
    ("tch_att3",     "Touches att 3rd",    poss, "touches_att_3rd",             "Wide",    True,  True),
    ("tch_attpen",   "Touches att pen",    poss, "touches_att_pen_area",        "Wide",    True,  True),
    # Defending
    ("tackles",      "Tackles",            defense,"tackles",                   "Defend",  True,  True),
    ("tkl_won",      "Tackles won",        defense,"tackles_won",               "Defend",  True,  True),
    ("tkl_int",      "Tackles + int.",     defense,"tackles_interceptions",     "Defend",  True,  True),
    ("interceptions","Interceptions",      defense,"interceptions",             "Defend",  True,  True),
    ("blocks",       "Blocks",             defense,"blocks",                    "Defend",  True,  True),
    ("clearances",   "Clearances",         defense,"clearances",                "Defend",  True,  True),
    ("drib_tkl_pct", "Dribblers tackled %",defense,"dribble_tackles_pct",       "Defend",  False, True),
    ("aerials_won",  "Aerials won",        misc, "aerials_won",                 "Defend",  True,  True),
    ("aerials_pct",  "Aerial win %",       misc, "aerials_won_pct",             "Defend",  False, True),
    # Ball security
    ("pass_pct",     "Pass completion %",  passing,"passes_pct",                "Security",False, True),
    ("miscontrols",  "Miscontrols",        poss, "miscontrols",                 "Security",True,  False),
    ("dispossessed", "Dispossessed",       poss, "dispossessed",                "Security",True,  False),
    ("fouls",        "Fouls committed",    misc, "fouls",                       "Security",True,  False),
]

GROUPS = ["Attack", "Wide", "Defend", "Security"]

# ------------------------------------------------------- assemble per player
players = []
for s in stats:
    k = key(s)
    nineties = fnum(s.get("minutes_90s")) or 0.0
    src = {
        "s": s, "passing": passing.get(k, {}), "ptypes": ptypes.get(k, {}),
        "poss": poss.get(k, {}), "defense": defense.get(k, {}),
        "misc": misc.get(k, {}), "gca": gca.get(k, {}), "shoot": shoot.get(k, {}),
    }

    metrics = {}
    for mkey, label, source, col, group, per90, hib in METRICS:
        row = s if source == "s" else (src["gca"] if source is gca else
              src["passing"] if source is passing else src["poss"] if source is poss else
              src["defense"] if source is defense else src["misc"] if source is misc else
              src["shoot"] if source is shoot else {})
        raw = fnum(row.get(col))
        if raw is None:
            metrics[mkey] = {"raw": None, "p90": None}
            continue
        if per90:
            p90 = round(raw / nineties, 3) if nineties >= 0.5 else None
            metrics[mkey] = {"raw": raw, "p90": p90}
        else:  # already a rate / percentage
            metrics[mkey] = {"raw": raw, "p90": raw}

    pt = ptime.get(k, {})
    e = enrich.get(norm(s["player"]), {})
    players.append({
        "name": s["player"].strip(),
        "team": s["team"].strip(),
        "club": (s.get("club") or "").strip(),
        "pos": s["position"].strip(),        # FBref: GK/DF/MF/FW (+ combos)
        "age": (s.get("age") or "").split("-")[0],
        "minutes": int(fnum(s.get("minutes")) or 0),
        "games": int(fnum(s.get("games")) or 0),
        "starts": int(fnum(s.get("games_starts")) or 0),
        "nineties": round(nineties, 1),
        "jersey": e.get("jersey"),
        "boots": e.get("boots"),
        "fifa_rank": e.get("fifa_rank"),
        "metrics": metrics,
    })

# ----------------------------------------- percentiles vs qualified outfield
QUAL_90s = 1.5  # ~135 mins minimum to enter the percentile pool
pool = [p for p in players if p["pos"] != "GK" and p["nineties"] >= QUAL_90s]

for mkey, label, source, col, group, per90, hib in METRICS:
    vals = sorted(v for v in (p["metrics"][mkey]["p90"] for p in pool) if v is not None)
    n = len(vals)
    for p in players:
        cell = p["metrics"][mkey]
        v = cell["p90"]
        if v is None or n == 0 or p["pos"] == "GK" or p["nineties"] < QUAL_90s:
            cell["pct"] = None
            continue
        # share of pool at or below v -> 0..100, then invert if lower is better
        below = sum(1 for x in vals if x <= v)
        pct = round(100.0 * below / n)
        cell["pct"] = pct if hib else (100 - pct)

# ------------------------------------------------------- heuristic role seed
# Source data only knows GK/DF/MF/FW. Seed a *suggested* role (no left/right,
# which cannot be derived from these stats) for the user to refine by editing.
def avg(p, keys):
    xs = [p["metrics"][k]["pct"] for k in keys if p["metrics"][k]["pct"] is not None]
    return sum(xs) / len(xs) if xs else 0

for p in players:
    pos = p["pos"]
    if "GK" in pos:
        p["role_auto"] = "GK"
    elif pos.startswith("DF"):
        wide = avg(p, ["crosses", "crs_pen", "prog_pass", "tch_att3", "take_ons"])
        p["role_auto"] = "FB" if wide >= 55 else "CB"
    elif pos.startswith("FW"):
        wide = avg(p, ["take_ons", "crosses", "prog_rec"])
        box = avg(p, ["shots", "tch_attpen", "xg"])
        p["role_auto"] = "W" if wide >= box else "ST"
    else:  # MF or combos starting with MF
        p["role_auto"] = "MF"

players.sort(key=lambda p: (-p["minutes"], p["name"]))

out = {
    "built": datetime.date.today().isoformat(),
    "qualify_90s": QUAL_90s,
    "groups": GROUPS,
    "metrics": [
        {"key": m[0], "label": m[1], "group": m[4], "per90": m[5], "higher": m[6]}
        for m in METRICS
    ],
    "sources": {
        "fbref": "kaggle.com/datasets/swaptr/fifa-world-cup-2022-player-data",
        "enrich": "kaggle.com/datasets/rhugvedbhojane/fifa-world-cup-2022-players-statistics",
    },
    "players": players,
}

with open(os.path.join(HERE, "data.json"), "w", encoding="utf-8") as fh:
    json.dump(out, fh, ensure_ascii=False, separators=(",", ":"))

matched = sum(1 for p in players if p["jersey"])
print(f"players: {len(players)}  pooled(qualified outfield): {len(pool)}")
print(f"jersey/boots matched from 2nd dataset: {matched}/{len(players)}")
print("wrote data.json")

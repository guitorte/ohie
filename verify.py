"""Independent verification of the mastered files."""
import numpy as np
import soundfile as sf
import pyloudnorm as pyln
from scipy import signal
from pathlib import Path

ROOT = Path("/home/user/ohie")
TARGETS = [
    (ROOT / "⚡ Virada no Diabo (Master Streaming -14 LUFS).wav", -14.0, -1.0, "streaming"),
    (ROOT / "⚡ Virada no Diabo (Master Loud -9 LUFS).wav",       -9.0,  -1.0, "loud"),
    (ROOT / "⚡ Virada no Diabo (Master CD 16bit 44k1).wav",      -14.0, -1.0, "cd"),
]

def short_term(x, sr, win=3.0, hop=0.5):
    win_n = int(win * sr); hop_n = int(hop * sr)
    m = pyln.Meter(sr); out = []
    for s in range(0, len(x) - win_n, hop_n):
        try:
            l = m.integrated_loudness(x[s:s+win_n])
            if np.isfinite(l): out.append(l)
        except Exception: pass
    return np.array(out)

print(f"{'file':<58s} {'sr':>6s} {'fmt':>8s}  {'LUFS-I':>8s} {'TP dB':>7s} {'LRA':>6s} {'peak':>7s}  status")
print("-" * 120)
for path, tgt_lufs, ceiling, label in TARGETS:
    info = sf.info(str(path))
    x, sr = sf.read(str(path), always_2d=True)
    meter = pyln.Meter(sr)
    lufs = meter.integrated_loudness(x)
    up = signal.resample_poly(x, 4, 1, axis=0)
    tp = 20*np.log10(np.max(np.abs(up)) + 1e-12)
    peak = 20*np.log10(np.max(np.abs(x)) + 1e-12)
    st = short_term(x, sr)
    lra = (np.percentile(st, 95) - np.percentile(st, 10)) if len(st) > 10 else float("nan")
    ok_lufs = abs(lufs - tgt_lufs) < 0.5
    ok_tp = tp <= ceiling + 0.05
    status = "PASS" if (ok_lufs and ok_tp) else "FAIL"
    short_name = path.name
    print(f"{short_name:<58s} {sr:>6d} {info.subtype:>8s}  {lufs:+8.2f} {tp:+7.2f} {lra:6.2f} {peak:+7.2f}  {status}")
    if not ok_lufs:
        print(f"   ! LUFS off target: got {lufs:+.2f}, want {tgt_lufs:+.2f}")
    if not ok_tp:
        print(f"   ! TP over ceiling: {tp:+.2f} > {ceiling:+.2f}")

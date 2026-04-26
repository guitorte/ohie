"""Source analysis for the unmastered mix."""
import numpy as np
import soundfile as sf
import pyloudnorm as pyln
from scipy import signal

SRC = "/home/user/ohie/⚡ Virada no Diabo.wav"

x, sr = sf.read(SRC, always_2d=True)
n_samp, n_ch = x.shape
dur = n_samp / sr
print(f"File: {SRC}")
print(f"Sample rate: {sr} Hz | channels: {n_ch} | duration: {dur:.2f}s | samples: {n_samp}")
print(f"dtype on disk: 16-bit PCM | working dtype: {x.dtype}")

# Peak / RMS / DC / crest
peak = np.max(np.abs(x), axis=0)
rms = np.sqrt(np.mean(x**2, axis=0))
dc = np.mean(x, axis=0)
crest = 20 * np.log10(peak / (rms + 1e-12))
print(f"\nSample peak (dBFS): L={20*np.log10(peak[0]+1e-12):+.2f}  R={20*np.log10(peak[1]+1e-12):+.2f}")
print(f"RMS (dBFS):         L={20*np.log10(rms[0]+1e-12):+.2f}  R={20*np.log10(rms[1]+1e-12):+.2f}")
print(f"DC offset:          L={dc[0]:+.6f}  R={dc[1]:+.6f}")
print(f"Crest factor (dB):  L={crest[0]:.2f}  R={crest[1]:.2f}")

# Channel balance & stereo correlation
mid = (x[:, 0] + x[:, 1]) * 0.5
side = (x[:, 0] - x[:, 1]) * 0.5
mid_rms = np.sqrt(np.mean(mid**2))
side_rms = np.sqrt(np.mean(side**2))
print(f"\nMid RMS: {20*np.log10(mid_rms+1e-12):+.2f} dBFS")
print(f"Side RMS: {20*np.log10(side_rms+1e-12):+.2f} dBFS")
print(f"Side/Mid ratio: {side_rms/(mid_rms+1e-12):.3f} (1.0 = wide; <0.3 = narrow)")
corr = np.corrcoef(x[:, 0], x[:, 1])[0, 1]
print(f"L/R correlation: {corr:+.3f} (1=mono, 0=uncorrelated, -1=out-of-phase)")

# ITU-R BS.1770 loudness
meter = pyln.Meter(sr)
lufs_i = meter.integrated_loudness(x)
print(f"\nIntegrated loudness (LUFS-I): {lufs_i:+.2f}")
# Loudness range
try:
    lra = pyln.Meter(sr).integrated_loudness(x)  # placeholder
except Exception:
    pass

# Compute LRA manually via short-term loudness windows (3s, 100ms hop) per BS.1770/EBU R128
def short_term_loudness(x, sr, win=3.0, hop=0.1):
    win_n = int(win * sr)
    hop_n = int(hop * sr)
    m = pyln.Meter(sr)
    out = []
    for start in range(0, len(x) - win_n, hop_n):
        seg = x[start:start + win_n]
        try:
            l = m.integrated_loudness(seg)
            if np.isfinite(l):
                out.append(l)
        except Exception:
            pass
    return np.array(out)

st = short_term_loudness(x, sr)
if len(st) > 10:
    lra = np.percentile(st, 95) - np.percentile(st, 10)
    print(f"Loudness Range (LRA): {lra:.2f} LU")
    print(f"Short-term range: min={st.min():.2f}  max={st.max():.2f} LUFS")

# True peak via 4x oversampling (polyphase resample)
def true_peak_db(x, sr, os=4):
    up = signal.resample_poly(x, os, 1, axis=0)
    return 20 * np.log10(np.max(np.abs(up)) + 1e-12)

tp = true_peak_db(x, sr, os=4)
print(f"\nTrue peak (4x oversampled): {tp:+.2f} dBTP")

# Frequency balance — band energies
def band_energy(x, sr, lo, hi):
    sos = signal.butter(4, [lo, hi], btype="band", fs=sr, output="sos")
    y = signal.sosfilt(sos, x, axis=0)
    return 20 * np.log10(np.sqrt(np.mean(y**2)) + 1e-12)

bands = [
    ("sub 20-60",    20,    60),
    ("bass 60-200",  60,   200),
    ("lo-mid 200-500", 200,  500),
    ("mid 500-2k",  500,  2000),
    ("hi-mid 2k-6k", 2000, 6000),
    ("presence 6k-12k", 6000, 12000),
    ("air 12k-20k", 12000, 20000),
]
print("\nBand energies (dBFS RMS):")
for name, lo, hi in bands:
    if hi >= sr / 2:
        hi = sr / 2 - 1
    e = band_energy(x, sr, lo, hi)
    print(f"  {name:18s}: {e:+.2f}")

# Suggested headroom & target
print("\n--- Verdict ---")
print(f"Streaming targets:  LUFS-I ≈ -14 | TP ≤ -1.0 dBTP")
print(f"Loud master target: LUFS-I ≈ -9  | TP ≤ -1.0 dBTP")
print(f"Current gap to -14 LUFS: {-14 - lufs_i:+.2f} dB needed")
print(f"Current gap to -9 LUFS:  {-9 - lufs_i:+.2f} dB needed")

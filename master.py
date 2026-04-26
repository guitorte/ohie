"""Mastering chain for '⚡ Virada no Diabo'.

Chain (in order, float64):
  1. DC offset removal
  2. 30 Hz subsonic high-pass (Butterworth 2nd order, very gentle)
  3. Tonal EQ:
       - low-shelf  +0.8 dB @ 80 Hz   (weight)
       - high-shelf +2.5 dB @ 10 kHz  (air)
  4. Mid/Side widener (side gain +1.5 dB) — modest, mix is very mono
  5. Glue compressor (single-band feed-forward, slow attack/release)
       ratio 1.8:1, thr -18 dBFS, ~1 dB GR on average
  6. Tape-style soft saturation (tanh) — adds harmonics, smooths transients
  7. Pre-limiter gain to hit target LUFS
  8. True-peak limiter (4x oversampled lookahead limiter, ceiling -1.0 dBTP)
  9. Final LUFS trim (linear) to land exactly on target
 10. Output WAV (24-bit) and 16-bit/44.1k dithered

Implementation is intentionally self-contained (numpy/scipy/soundfile/pyloudnorm).
"""
from __future__ import annotations
import numpy as np
import soundfile as sf
import pyloudnorm as pyln
from scipy import signal


# ---------------- DSP primitives ----------------

def remove_dc(x: np.ndarray) -> np.ndarray:
    return x - np.mean(x, axis=0, keepdims=True)


def highpass(x: np.ndarray, sr: int, fc: float = 30.0, order: int = 2) -> np.ndarray:
    sos = signal.butter(order, fc, btype="highpass", fs=sr, output="sos")
    return signal.sosfilt(sos, x, axis=0)


def _biquad_shelf(kind: str, fc: float, gain_db: float, sr: int, q: float = 0.707):
    """RBJ cookbook shelving filter."""
    A = 10 ** (gain_db / 40.0)
    w0 = 2 * np.pi * fc / sr
    cosw = np.cos(w0)
    sinw = np.sin(w0)
    alpha = sinw / (2 * q)
    if kind == "low":
        b0 =    A * ((A + 1) - (A - 1) * cosw + 2 * np.sqrt(A) * alpha)
        b1 =  2*A * ((A - 1) - (A + 1) * cosw)
        b2 =    A * ((A + 1) - (A - 1) * cosw - 2 * np.sqrt(A) * alpha)
        a0 =        (A + 1) + (A - 1) * cosw + 2 * np.sqrt(A) * alpha
        a1 =   -2 * ((A - 1) + (A + 1) * cosw)
        a2 =        (A + 1) + (A - 1) * cosw - 2 * np.sqrt(A) * alpha
    elif kind == "high":
        b0 =    A * ((A + 1) + (A - 1) * cosw + 2 * np.sqrt(A) * alpha)
        b1 = -2*A * ((A - 1) + (A + 1) * cosw)
        b2 =    A * ((A + 1) + (A - 1) * cosw - 2 * np.sqrt(A) * alpha)
        a0 =        (A + 1) - (A - 1) * cosw + 2 * np.sqrt(A) * alpha
        a1 =    2 * ((A - 1) - (A + 1) * cosw)
        a2 =        (A + 1) - (A - 1) * cosw - 2 * np.sqrt(A) * alpha
    else:
        raise ValueError(kind)
    b = np.array([b0, b1, b2]) / a0
    a = np.array([1.0, a1 / a0, a2 / a0])
    return b, a


def shelf(x: np.ndarray, sr: int, kind: str, fc: float, gain_db: float, q: float = 0.707):
    b, a = _biquad_shelf(kind, fc, gain_db, sr, q)
    return signal.lfilter(b, a, x, axis=0)


def widen(x: np.ndarray, side_gain_db: float = 1.5) -> np.ndarray:
    """Mid/side widener. Positive dB widens, negative narrows."""
    mid  = (x[:, 0] + x[:, 1]) * 0.5
    side = (x[:, 0] - x[:, 1]) * 0.5
    side *= 10 ** (side_gain_db / 20.0)
    out = np.column_stack([mid + side, mid - side])
    return out


def glue_compressor(
    x: np.ndarray, sr: int,
    threshold_db: float = -18.0,
    ratio: float = 1.8,
    attack_ms: float = 30.0,
    release_ms: float = 250.0,
    knee_db: float = 6.0,
    makeup_db: float = 0.0,
) -> np.ndarray:
    """Feed-forward compressor with linked stereo detection (max abs of L/R)."""
    atk = np.exp(-1.0 / (sr * attack_ms * 1e-3))
    rel = np.exp(-1.0 / (sr * release_ms * 1e-3))
    det = np.max(np.abs(x), axis=1) + 1e-12
    det_db = 20 * np.log10(det)
    # Soft-knee static curve (gain reduction in dB, positive value = how many dB to subtract)
    over = det_db - threshold_db
    half_knee = knee_db / 2.0
    gr_static = np.zeros_like(det_db)
    above = over >= half_knee
    inside = (over > -half_knee) & (~above)
    gr_static[above] = over[above] * (1 - 1 / ratio)
    if knee_db > 0:
        x_in = over[inside] + half_knee
        gr_static[inside] = (1 - 1 / ratio) * (x_in ** 2) / (2 * knee_db)
    # Smooth gain reduction with attack/release (envelope follower on gr_static, not signal)
    gr = np.zeros_like(gr_static)
    g = 0.0
    for n in range(len(gr_static)):
        target = gr_static[n]
        coeff = atk if target > g else rel
        g = target + (g - target) * coeff
        gr[n] = g
    gain_lin = 10 ** ((-gr + makeup_db) / 20.0)
    return x * gain_lin[:, None]


def soft_saturation(x: np.ndarray, drive_db: float = 1.0) -> np.ndarray:
    """Symmetric tanh saturation; drive in dB before, normalize after to keep RMS roughly stable."""
    drive = 10 ** (drive_db / 20.0)
    rms_in = np.sqrt(np.mean(x**2) + 1e-12)
    y = np.tanh(x * drive) / np.tanh(drive)
    rms_out = np.sqrt(np.mean(y**2) + 1e-12)
    return y * (rms_in / rms_out)


def true_peak_limiter(
    x: np.ndarray, sr: int,
    ceiling_db: float = -1.0,
    lookahead_ms: float = 5.0,
    release_ms: float = 50.0,
    oversample: int = 4,
) -> np.ndarray:
    """Lookahead peak limiter operating on oversampled signal for ISP control.

    Strategy:
      * Upsample 4x.
      * Compute per-sample needed gain (<=1) to keep |y| <= ceiling.
      * Take per-window MIN gain over a future lookahead window (so attack pre-empts peaks).
      * Smooth the gain with a one-pole release (gain rises slowly back to 1).
      * Apply, downsample.
    """
    ceiling = 10 ** (ceiling_db / 20.0)
    up = signal.resample_poly(x, oversample, 1, axis=0)
    sr_up = sr * oversample
    la_n = max(1, int(sr_up * lookahead_ms * 1e-3))
    rel_coef = np.exp(-1.0 / (sr_up * release_ms * 1e-3))

    # per-sample required gain
    peak = np.max(np.abs(up), axis=1) + 1e-12
    need = np.minimum(1.0, ceiling / peak)

    # forward-look minimum over lookahead window (use sliding minimum)
    # implement via reversed running max trick: pad at end
    pad = np.concatenate([need, np.ones(la_n, dtype=need.dtype)])
    # 1-D minimum filter using scipy
    from scipy.ndimage import minimum_filter1d
    fwd_min = minimum_filter1d(pad, size=la_n, origin=-(la_n // 2))[: len(need)]

    # release smoothing: gain only rises slowly back up
    g = np.empty_like(fwd_min)
    cur = 1.0
    for n in range(len(fwd_min)):
        tgt = fwd_min[n]
        if tgt < cur:           # attack: instant (lookahead already pre-empts)
            cur = tgt
        else:                   # release: one-pole back toward target (1 if no peak)
            cur = tgt + (cur - tgt) * rel_coef
        g[n] = cur

    y_up = up * g[:, None]
    y = signal.resample_poly(y_up, 1, oversample, axis=0)
    # belt-and-braces: hard clip any tiny residual above ceiling (sample-domain)
    y = np.clip(y, -ceiling, ceiling)
    return y


def measure(x: np.ndarray, sr: int, label: str = "") -> dict:
    meter = pyln.Meter(sr)
    lufs = meter.integrated_loudness(x)
    up = signal.resample_poly(x, 4, 1, axis=0)
    tp = 20 * np.log10(np.max(np.abs(up)) + 1e-12)
    peak = 20 * np.log10(np.max(np.abs(x)) + 1e-12)
    rms = 20 * np.log10(np.sqrt(np.mean(x**2)) + 1e-12)
    if label:
        print(f"  [{label}]  LUFS-I={lufs:+.2f}  TP={tp:+.2f}  peak={peak:+.2f}  rms={rms:+.2f}")
    return {"lufs": lufs, "tp": tp, "peak": peak, "rms": rms}


def normalize_to_lufs(x: np.ndarray, sr: int, target_lufs: float, ceiling_db: float = -1.0) -> np.ndarray:
    """Final LUFS trim respecting TP ceiling. If trim would push TP over ceiling, clamp."""
    meter = pyln.Meter(sr)
    cur = meter.integrated_loudness(x)
    gain_db = target_lufs - cur
    # check tp budget
    up = signal.resample_poly(x, 4, 1, axis=0)
    tp_cur = 20 * np.log10(np.max(np.abs(up)) + 1e-12)
    tp_after = tp_cur + gain_db
    if tp_after > ceiling_db:
        gain_db = ceiling_db - tp_cur
    return x * (10 ** (gain_db / 20.0))


# ---------------- TPDF dither ----------------

def tpdf_dither_to_int16(x: np.ndarray) -> np.ndarray:
    """16-bit triangular PDF dither. x assumed in [-1, 1]."""
    # 1 LSB = 1 / 32768
    lsb = 1.0 / 32768.0
    rng = np.random.default_rng(0xC0FFEE)
    n = rng.uniform(-0.5, 0.5, size=x.shape)
    m = rng.uniform(-0.5, 0.5, size=x.shape)
    dithered = x + (n + m) * lsb
    dithered = np.clip(dithered, -1.0, 1.0 - lsb)
    return np.round(dithered * 32768.0).astype(np.int16)


# ---------------- Mastering chain ----------------

def _hit_target_lufs(
    pre: np.ndarray, sr: int, target_lufs: float, ceiling_db: float,
    limiter_release_ms: float = 50.0, max_drive_db: float = 18.0,
) -> tuple[np.ndarray, float, float]:
    """Binary search pre-limiter drive so that limiter-output LUFS hits target.

    Returns (output, drive_db_used, achieved_lufs).
    """
    meter = pyln.Meter(sr)

    def render(drive_db):
        y = pre * (10 ** (drive_db / 20.0))
        y = true_peak_limiter(y, sr, ceiling_db=ceiling_db, lookahead_ms=5.0,
                              release_ms=limiter_release_ms, oversample=4)
        return y, meter.integrated_loudness(y)

    lo, hi = 0.0, max_drive_db
    y_lo, l_lo = render(lo)
    y_hi, l_hi = render(hi)
    if l_lo >= target_lufs:
        # already loud enough at zero drive — do final fine trim
        return y_lo, lo, l_lo
    if l_hi < target_lufs - 0.5:
        # can't reach target even at max drive — return loudest available
        print(f"    [warn] cannot reach {target_lufs:+.1f} LUFS even with {hi:+.1f} dB drive; got {l_hi:+.2f}")
        return y_hi, hi, l_hi
    # bisect
    for _ in range(14):
        mid = 0.5 * (lo + hi)
        y_mid, l_mid = render(mid)
        if abs(l_mid - target_lufs) < 0.15:
            return y_mid, mid, l_mid
        if l_mid < target_lufs:
            lo, l_lo, y_lo = mid, l_mid, y_mid
        else:
            hi, l_hi, y_hi = mid, l_mid, y_mid
    return y_mid, mid, l_mid


def master_chain(
    x: np.ndarray, sr: int,
    target_lufs: float,
    ceiling_db: float = -1.0,
    extra_widen_db: float = 1.5,
    extra_air_db: float = 2.5,
    extra_low_db: float = 0.8,
    comp_thr_db: float = -10.0,
    comp_ratio: float = 1.5,
    sat_drive_db: float = 1.0,
    limiter_release_ms: float = 50.0,
) -> tuple[np.ndarray, dict]:
    print(f"\n>>> mastering for target {target_lufs:+.1f} LUFS / TP ≤ {ceiling_db:+.1f} dBTP")
    measure(x, sr, "in        ")
    y = remove_dc(x)
    y = highpass(y, sr, fc=30.0, order=2)
    y = shelf(y, sr, "low",  fc=80.0,    gain_db=extra_low_db)
    y = shelf(y, sr, "high", fc=10000.0, gain_db=extra_air_db)
    y = widen(y, side_gain_db=extra_widen_db)
    measure(y, sr, "post-EQ   ")
    y = glue_compressor(y, sr, threshold_db=comp_thr_db, ratio=comp_ratio,
                        attack_ms=50.0, release_ms=350.0, knee_db=6.0, makeup_db=0.0)
    measure(y, sr, "post-comp ")
    y = soft_saturation(y, drive_db=sat_drive_db)
    measure(y, sr, "post-sat  ")
    y, drive_used, l_after = _hit_target_lufs(
        y, sr, target_lufs, ceiling_db, limiter_release_ms=limiter_release_ms,
    )
    print(f"    [drive used: {drive_used:+.2f} dB → {l_after:+.2f} LUFS]")
    measure(y, sr, "post-limit")
    # final fine trim (≤ 0.3 dB) to land exactly on target if TP allows
    y = normalize_to_lufs(y, sr, target_lufs, ceiling_db=ceiling_db)
    final = measure(y, sr, "final     ")
    return y, final


# ---------------- Main ----------------

def main():
    src = "/home/user/ohie/⚡ Virada no Diabo.wav"
    x, sr = sf.read(src, always_2d=True)
    print(f"Loaded {src} : {sr} Hz / {x.shape[0]} samples / {x.shape[1]} ch")

    # 1. Streaming master (Spotify/YouTube safe): -14 LUFS, TP -1
    stream, _ = master_chain(
        x, sr,
        target_lufs=-14.0, ceiling_db=-1.0,
        extra_widen_db=1.5, extra_air_db=2.5, extra_low_db=0.8,
        comp_thr_db=-10.0, comp_ratio=1.5,
        sat_drive_db=1.0,
        limiter_release_ms=80.0,
    )
    out_stream = "/home/user/ohie/⚡ Virada no Diabo (Master Streaming -14 LUFS).wav"
    sf.write(out_stream, stream, sr, subtype="PCM_24")
    print(f"  -> wrote {out_stream}")

    # 2. Loud YouTube master: aim -9 LUFS (will fall to whatever the chain can honestly do)
    loud, _ = master_chain(
        x, sr,
        target_lufs=-9.0, ceiling_db=-1.0,
        extra_widen_db=1.5, extra_air_db=2.5, extra_low_db=0.8,
        comp_thr_db=-12.0, comp_ratio=1.8,   # slightly firmer comp
        sat_drive_db=2.0,                    # more harmonics for density
        limiter_release_ms=40.0,
    )
    out_loud = "/home/user/ohie/⚡ Virada no Diabo (Master Loud -9 LUFS).wav"
    sf.write(out_loud, loud, sr, subtype="PCM_24")
    print(f"  -> wrote {out_loud}")

    # 3. CD-ready master: 16-bit / 44.1 kHz, dithered, derived from streaming master
    sr_cd = 44100
    cd = signal.resample_poly(stream, sr_cd, sr, axis=0)
    # safety: re-check TP after resample (resampling can introduce small overshoot)
    cd = true_peak_limiter(cd, sr_cd, ceiling_db=-1.0, lookahead_ms=5.0,
                           release_ms=50.0, oversample=4)
    cd = normalize_to_lufs(cd, sr_cd, -14.0, ceiling_db=-1.0)
    cd_int = tpdf_dither_to_int16(cd)
    out_cd = "/home/user/ohie/⚡ Virada no Diabo (Master CD 16bit 44k1).wav"
    sf.write(out_cd, cd_int, sr_cd, subtype="PCM_16")
    print(f"  -> wrote {out_cd}")


if __name__ == "__main__":
    main()

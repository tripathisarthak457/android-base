#!/usr/bin/env python3
"""
Measures the shimmer in a screen recording: where the band is in every frame, how long a sweep and a
pause last, and whether one straight band crosses every bone (REPORT.md section 5.4 step 3).

    sweep.py REC.mp4 OUT_PREFIX [--region x0,y0,x1,y1] [--rtl] [--dp-px 1.8333] [--until T]

A pixel is a bone pixel when the band visibly passes over it during the recording (its 90th
percentile luma is at least 5 above its median). Its plain luma is its median: no frames are recorded
during a pause, but the band covers any one pixel for well under half of a sweep. Per frame, a bone
pixel's excess is its luma minus its plain luma. The band's phase is u = x cos 20 + y sin 20 (u =
-x cos 20 + y sin 20 right to left), in recording px.

Writes OUT_PREFIX.frames.tsv (per frame: time, lit pixels, excess-weighted centroid u), OUT_PREFIX.rows.tsv
(per strip frame and bone row: the fitted band centre and its u) and OUT_PREFIX.json (the summary).
"""

import argparse
import json
import statistics as st

from media import COS, SIN, fit_band, gray_frames, pts, size

parser = argparse.ArgumentParser()
parser.add_argument("rec")
parser.add_argument("out")
parser.add_argument("--region", default=None)
parser.add_argument("--rtl", action="store_true")
parser.add_argument("--dp-px", type=float, default=2.75 / 1.5, help="recording px per dp")
parser.add_argument("--band-dp", type=float, default=240)
parser.add_argument("--until", type=float, default=None, help="ignore frames after this time (s)")
parser.add_argument("--strip", type=int, default=6)
parser.add_argument("--step", type=int, default=3)
args = parser.parse_args()

w, h = size(args.rec)
times = pts(args.rec)
x0, y0, x1, y1 = (int(v) for v in args.region.split(",")) if args.region else (0, 200, w, h - 80)
step = args.step
sign = -1 if args.rtl else 1
half_width = args.band_dp * args.dp_px / 2 / COS     # the band's half-width along a row, in recording px
last = len(times) if args.until is None else sum(1 for t in times if t <= args.until)
sample_points = [(x, y) for y in range(y0, y1, step) for x in range(x0, x1, step)]
offsets = [y * w + x for x, y in sample_points]

# Pass 1: per-pixel median and 90th percentile over up to 90 frames spread over the recording.
pick = set(range(0, last, max(1, last // 90)))
columns = [[] for _ in offsets]
for index, frame in enumerate(gray_frames(args.rec, w, h)):
    if index >= last:
        break
    if index in pick:
        for k, offset in enumerate(offsets):
            columns[k].append(frame[offset])
base, mask = {}, []
for k, values in enumerate(columns):
    values.sort()
    median = values[len(values) // 2]
    p90 = values[int(len(values) * 0.9)]
    if p90 - median >= 5:
        mask.append(k)
        base[k] = median
bone_luma = st.median(base.values()) if base else None
u_of = {k: sign * sample_points[k][0] * COS + sample_points[k][1] * SIN for k in mask}

# Pass 2: per frame, how much of the bone is lit and where.
rows = []
for index, frame in enumerate(gray_frames(args.rec, w, h)):
    if index >= last:
        break
    lit = 0
    su = se = 0.0
    for k in mask:
        e = frame[offsets[k]] - base[k]
        if e > 3:
            lit += 1
            su += u_of[k] * e
            se += e
    rows.append((index, times[index], lit, su / se if se else float("nan")))
with open(args.out + ".frames.tsv", "w") as handle:
    handle.write("index\tt\tlit_px\tcentroid_u\n")
    for index, t, lit, u in rows:
        handle.write(f"{index}\t{t:.4f}\t{lit}\t{u:.1f}\n")

# Sweeps: runs of frames with the band on some bone, split where no band shows for 0.3 s or more.
threshold = max(10, 0.01 * len(mask))
visible = [(i, t, u) for i, t, lit, u in rows if lit >= threshold]
sweeps = []
for i, t, u in visible:
    if sweeps and t - sweeps[-1][-1][1] < 0.3:
        sweeps[-1].append((i, t, u))
    else:
        sweeps.append([(i, t, u)])
starts = [s[0][1] for s in sweeps]
complete = [s for s in sweeps if s[0][1] > times[0] + 0.05 and s[-1][1] < times[last - 1] - 0.05]
summary = {
    "recording": args.rec,
    "size": [w, h],
    "frames": last,
    "span_s": round(times[last - 1] - times[0], 3),
    "region": [x0, y0, x1, y1],
    "bone_pixels_sampled": len(mask),
    "bone_luma": bone_luma,
    "band_half_width_px": round(half_width, 1),
    "sweeps": [{"start": round(s[0][1], 3), "end": round(s[-1][1], 3), "visible_s": round(s[-1][1] - s[0][1], 3),
                "frames": len(s)} for s in sweeps],
    "periods_s": [round(b - a, 3) for a, b in zip(starts, starts[1:])],
    "gaps_without_band_s": [round(b[0][1] - a[-1][1], 3) for a, b in zip(sweeps, sweeps[1:])],
}
# Frames the screen recorder wrote between sweeps: it writes a frame only when the screen changes.
summary["frames_between_sweeps"] = [sum(1 for _, t, _, _ in rows if a[-1][1] < t < b[0][1])
                                    for a, b in zip(sweeps, sweeps[1:])]

# Pass 3: one straight band? Fit the band centre on every bone row of a few frames of one sweep.
target = max(complete or sweeps, key=len) if sweeps else []
strip = []
if target:
    span = target[-1][1] - target[0][1]
    for j in range(args.strip):
        t_want = target[0][1] + span * (j + 0.5) / args.strip
        strip.append(min(target, key=lambda e: abs(e[1] - t_want))[0])
by_row = {}
for k in mask:
    by_row.setdefault(sample_points[k][1], []).append(k)
fits = []
strip_set = set(strip)
for index, frame in enumerate(gray_frames(args.rec, w, h)):
    if index > max(strip_set, default=-1):
        break
    if index not in strip_set:
        continue
    for y, ks in sorted(by_row.items()):
        if len(ks) < 12:
            continue
        xs = [sample_points[k][0] for k in ks]
        es = [frame[offsets[k]] - base[k] for k in ks]
        xc, amplitude, coverage = fit_band(xs, es, half_width, min(xs) - half_width, max(xs) + half_width)
        inside = min(xs) - 0.25 * half_width <= xc <= max(xs) + 0.25 * half_width
        if amplitude >= 4 and coverage >= 6 and inside:
            fits.append((index, times[index], y, xc, sign * xc * COS + y * SIN, amplitude))
with open(args.out + ".rows.tsv", "w") as handle:
    handle.write("index\tt\ty\tband_x\tband_u\tamplitude\n")
    for f in fits:
        handle.write(f"{f[0]}\t{f[1]:.4f}\t{f[2]}\t{f[3]:.1f}\t{f[4]:.1f}\t{f[5]:.1f}\n")
per_frame = []
for index in strip:
    us = [f[4] for f in fits if f[0] == index]
    if len(us) < 3:
        per_frame.append({"index": index, "rows": len(us)})
        continue
    median = st.median(us)
    deviations = sorted(abs(u - median) for u in us)
    per_frame.append({"index": index, "t": round(times[index], 3), "rows": len(us), "median_u": round(median, 1),
                      "max_abs_dev_px": round(deviations[-1], 1),
                      "p90_abs_dev_px": round(deviations[int(len(deviations) * 0.9)], 1),
                      "rows_within_2px": round(sum(1 for d in deviations if d <= 2) / len(deviations), 3),
                      "y_range": [min(f[2] for f in fits if f[0] == index), max(f[2] for f in fits if f[0] == index)]})
summary["strip"] = per_frame
with open(args.out + ".json", "w") as handle:
    json.dump(summary, handle, indent=1)
print(json.dumps(summary, indent=1))

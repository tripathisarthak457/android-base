#!/usr/bin/env python3
"""
One straight band? Measured on lossless screencaps (REPORT.md section 5.4 step 3), where the video's
compression noise (about +-10 recording px on a fitted centre) cannot hide a misaligned bone.

    coherence.py --region x0,y0,x1,y1 --out OUT.json SHOT.png...

The shot whose bones are darkest overall is taken as the plain one (the band is off screen or in its
pause); a pixel is a bone pixel when that shot has it within 1 of the bone colour on every channel, the
bone colour being the most common colour in the region that is not the page or a card. In every other
shot, each sampled bone row's band centre is fitted (a triangle of the band's known half-width, least
squares, to the luma lift over the plain shot), and turned into the band's phase u = x cos 20 + y sin 20
(device px). One band means one u per shot: the spread of u across rows is the measure.
"""

import argparse
import json
import statistics as st
from collections import Counter

from media import COS, SIN, fit_band, rgb_image

parser = argparse.ArgumentParser()
parser.add_argument("shots", nargs="+")
parser.add_argument("--region", required=True)
parser.add_argument("--out", required=True)
parser.add_argument("--band-px", type=float, default=240 * 2.75)
parser.add_argument("--rtl", action="store_true")
parser.add_argument("--row-step", type=int, default=12)
parser.add_argument("--bone", default=None, help="the bone colour RRGGBB (default: the third most common colour)")
args = parser.parse_args()

x0, y0, x1, y1 = (int(v) for v in args.region.split(","))
sign = -1 if args.rtl else 1
half_width = args.band_px / 2 / COS
images = {path: rgb_image(path) for path in args.shots}
w, h, _ = next(iter(images.values()))


def luma_rows(raw):
    return {y: [0.2126 * raw[(y * w + x) * 3] + 0.7152 * raw[(y * w + x) * 3 + 1] + 0.0722 * raw[(y * w + x) * 3 + 2]
                for x in range(x0, x1)] for y in range(y0, y1, args.row_step)}


lumas = {path: luma_rows(raw) for path, (_, _, raw) in images.items()}
# The plain shot: the lowest total luma over the region (the band only ever brightens a bone).
plain = min(lumas, key=lambda p: sum(sum(r) for r in lumas[p].values()))
_, _, plain_raw = images[plain]
colours = Counter()
for y in range(y0, y1, args.row_step):
    for x in range(x0, x1, 2):
        o = (y * w + x) * 3
        colours[(plain_raw[o], plain_raw[o + 1], plain_raw[o + 2])] += 1
top = [c for c, _ in colours.most_common(6)]
# Page and card are the two most common colours; the bone is the next one that is not near-white/black text.
bone = tuple(int(args.bone[i:i + 2], 16) for i in (0, 2, 4)) if args.bone else top[2]
mask = {}
for y in range(y0, y1, args.row_step):
    xs = []
    for x in range(x0, x1):
        o = (y * w + x) * 3
        if all(abs(plain_raw[o + i] - bone[i]) <= 1 for i in range(3)):
            xs.append(x)
    if len(xs) >= 20:
        mask[y] = xs
result = {"plain_shot": plain.split("/")[-1], "colours_by_count": ["%02X%02X%02X" % c for c in top],
          "bone": "%02X%02X%02X" % bone, "band_half_width_px": round(half_width, 1), "rows_with_bone": len(mask),
          "shots": []}
for path in args.shots:
    if path == plain:
        continue
    rows = lumas[path]
    base = lumas[plain]
    fits = []
    for y, xs in mask.items():
        es = [rows[y][x - x0] - base[y][x - x0] for x in xs]
        if max(es) < 3:
            continue
        sub_x, sub_e = xs[::2], es[::2]
        xc, amplitude, coverage = fit_band(sub_x, sub_e, half_width, min(xs) - half_width, max(xs) + half_width)
        if amplitude >= 6 and coverage >= 15 and min(xs) - 0.25 * half_width <= xc <= max(xs) + 0.25 * half_width:
            fits.append((y, xc, sign * xc * COS + y * SIN, amplitude, min(xs) <= xc <= max(xs)))
    shot = {"shot": path.split("/")[-1], "rows": len(fits)}
    if len(fits) >= 5:
        us = [f[2] for f in fits]
        median = st.median(us)
        dev = sorted(abs(u - median) for u in us)
        shot.update({"median_u": round(median, 1), "max_abs_dev_px": round(dev[-1], 1),
                     "p90_abs_dev_px": round(dev[int(len(dev) * 0.9)], 1), "median_abs_dev_px": round(dev[len(dev) // 2], 2),
                     "rows_within_2px": round(sum(1 for d in dev if d <= 2) / len(dev), 3),
                     "y_range": [fits[0][0], fits[-1][0]],
                     "fits": [[f[0], round(f[1], 1), round(f[2], 1), round(f[3], 1), f[4]] for f in fits]})
        # Rows whose band centre lies on the row's own bone pixels: there the fit interpolates; elsewhere
        # (a short avatar chord, say) it extrapolates a ramp and its error grows.
        inside = sorted(abs(f[2] - median) for f in fits if f[4])
        if inside:
            shot["centre_on_bone"] = {"rows": len(inside), "median_abs_dev_px": round(inside[len(inside) // 2], 2),
                                      "p90_abs_dev_px": round(inside[int(len(inside) * 0.9)], 1),
                                      "max_abs_dev_px": round(inside[-1], 1),
                                      "within_2px": round(sum(1 for d in inside if d <= 2) / len(inside), 3)}
        # What a perfect single band would measure here: the same rows and pixels, the band at the median
        # phase with each row's fitted amplitude, rounded to whole luma steps as the screen stores it.
        sim = []
        for y, xc, u, amplitude, _ in fits:
            xs = mask[y]
            true_x = sign * (median - y * SIN) / COS
            es = [round(amplitude * max(0.0, 1 - abs(x - true_x) / half_width)) for x in xs]
            sx, _, _ = fit_band(xs[::2], es[::2], half_width, min(xs) - half_width, max(xs) + half_width)
            sim.append(abs(sign * sx * COS + y * SIN - median))
        sim.sort()
        shot["perfect_band_quantised"] = {"median_abs_dev_px": round(sim[len(sim) // 2], 2),
                                          "p90_abs_dev_px": round(sim[int(len(sim) * 0.9)], 1),
                                          "max_abs_dev_px": round(sim[-1], 1)}
    result["shots"].append(shot)
open(args.out, "w").write(json.dumps(result, indent=1))
print(json.dumps({k: v for k, v in result.items() if k != "shots"}, indent=1))
for shot in result["shots"]:
    print({k: v for k, v in shot.items() if k != "fits"})

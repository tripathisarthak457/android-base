#!/usr/bin/env python3
"""Writes the before/after strips of qa/shimmer/after/ (before = EVIDENCE.md's recordings of Oct 12)."""

import json
import os

from media import pts
from strip import compose

HERE = os.path.dirname(os.path.abspath(__file__))
AFTER = os.path.dirname(HERE)
EVIDENCE = os.path.join(os.path.dirname(AFTER), "evidence")
REC_B = os.path.join(EVIDENCE, "rec")
REC_A = os.path.join(AFTER, "rec")


def at(rec, t):
    times = pts(rec)
    return min(range(len(times)), key=lambda i: abs(times[i] - t)), times


def frames_between(rec, t0, t1, n):
    times = pts(rec)
    out = []
    for k in range(n):
        want = t0 + (t1 - t0) * k / (n - 1)
        out.append(min(range(len(times)), key=lambda i: abs(times[i] - want)))
    return out, times


def cells(rec, indices, times, crop, prefix=""):
    t0 = times[indices[0]]
    return [{"src": rec, "index": i, "crop": crop, "label": f"{prefix}+{times[i] - t0:.2f} s"} for i in indices]


def load(name):
    return json.load(open(os.path.join(AFTER, "analysis", name)))


# 1-2. The catalog's Skeletons group, as EVIDENCE.md recorded it (Feedback page).
for theme, (a0, a1) in (("light", (1.482, 2.731)), ("dark", (1.156, 2.42))):
    rb = os.path.join(REC_B, f"cat-skel-{theme}.mp4")
    ra = os.path.join(REC_A, f"cat-skel-{theme}.mp4")
    ib, tb = frames_between(rb, 0.30, 1.42, 5)
    ia, ta = frames_between(ra, a0 + 0.05 * (a1 - a0), a1 - 0.05 * (a1 - a0), 5)
    compose([
        ("BEFORE (Oct 12): each placeholder sweeps its own band; one 1.4 s cycle", cells(rb, ib, tb, (0, 560, 720, 590))),
        ("AFTER (46ec781): one tilted band crosses every bone; one 1.2 s sweep", cells(ra, ia, ta, (0, 470, 720, 950))),
    ], os.path.join(AFTER, f"strip-catalog-skeletons-{theme}.png"), 432,
        title=f"Catalog, Feedback > Skeletons, {theme}: before vs after")

# 3-4. A skeleton list: the sample list's cold start before; the catalog's Loading page after.
for theme, before_window, (a0, a1) in (("light", (1.50, 2.24), (1.199, 2.422)), ("dark", (1.53, 1.79), (1.329, 2.537))):
    rb = os.path.join(REC_B, f"ptr-cold-{theme}.mp4")
    ra = os.path.join(REC_A, f"cat-loading-{theme}.mp4")
    ib, tb = frames_between(rb, *before_window, 5)
    ia, ta = frames_between(ra, a0 + 0.08 * (a1 - a0), a1 - 0.08 * (a1 - a0), 5)
    compose([
        ("BEFORE (Oct 12): sample list cold start, bands out of step per bone", cells(rb, ib, tb, (0, 380, 720, 1060))),
        ("AFTER (46ec781): catalog Loading page, 12 redacted rows, one band", cells(ra, ia, ta, (0, 300, 720, 1060))),
    ], os.path.join(AFTER, f"strip-skeleton-list-{theme}.png"), 432,
        title=f"A skeleton list sweeping, {theme}: before vs after")

# 5-8. Cold start of the sample list with its data cleared: skeleton, last skeleton frame, first content frame.
for name, theme_b in (("light", "light"), ("dark", "dark"), ("font150-light", "font150-light")):
    rows = []
    for label, rec, analysis in (
            ("BEFORE (Oct 12)", os.path.join(REC_B, f"ptr-cold-{theme_b}.mp4"), f"before-ptr-cold-{theme_b}.json"),
            ("AFTER (46ec781)", os.path.join(REC_A, f"ptr-cold-{name}.mp4"), f"ptr-cold-{name}.json")):
        data = load(analysis)
        times = pts(rec)
        last, first = data["last_skeleton_frame"], data["first_content_frame"]
        settled = at(rec, data["settled_skeleton_from_s"] + 0.6 * (times[last] - data["settled_skeleton_from_s"]))[0]
        guides = [(data["cards_content"][0][0], "red"), (data["cards_skeleton"][0][0], "0x00A0FF")]
        rows.append((f"{label}", [
            {"src": rec, "index": settled, "lines": guides, "label": f"skeleton t {times[settled]:.2f} s"},
            {"src": rec, "index": last, "lines": guides, "label": f"last skeleton t {times[last]:.3f} s"},
            {"src": rec, "index": first, "lines": guides, "label": f"first content t {times[first]:.3f} s"},
        ]))
    compose(rows, os.path.join(AFTER, f"strip-cold-start-{name}.png"), 360,
            title=f"Sample list cold start, {name}. Red: first content card top. Blue: first skeleton card top.")

# Landscape: the before recording is letterboxed portrait (720x1600), the after one landscape (1600x720).
rows = []
for label, rec, analysis, crop in (
        ("BEFORE (Oct 12)", os.path.join(REC_B, "ptr-cold-landscape-light.mp4"), "before-ptr-cold-landscape-light.json",
         (0, 638, 720, 324)),
        ("AFTER (46ec781)", os.path.join(REC_A, "ptr-cold-landscape-light.mp4"), "ptr-cold-landscape-light.json", None)):
    data = load(analysis)
    times = pts(rec)
    last, first = data["last_skeleton_frame"], data["first_content_frame"]
    guides = [(data["cards_content"][0][0], "red"), (data["cards_skeleton"][0][0], "0x00A0FF")]
    rows.append((f"{label}", [
        {"src": rec, "index": last, "crop": crop, "lines": guides, "label": f"last skeleton t {times[last]:.3f} s"},
        {"src": rec, "index": first, "crop": crop, "lines": guides, "label": f"first content t {times[first]:.3f} s"},
    ]))
compose(rows, os.path.join(AFTER, "strip-cold-start-landscape-light.png"), 720,
        title="Sample list cold start, landscape light. Red: first content card top. Blue: first skeleton card top.")

# 9. Lifting a redaction in place (catalog Loading page, "Load"): no row moves (after only; new page).
rec = os.path.join(REC_A, "cat-loading-light-load.mp4")
times = pts(rec)
tops = [(319, "red"), (508, "red"), (664, "red"), (856, "red"), (1013, "red"), (1204, "red"), (1362, "red")]
compose([("AFTER (46ec781). Red: card tops", [
    {"src": rec, "index": 152, "lines": tops, "label": f"redacted t {times[152]:.3f} s"},
    {"src": rec, "index": 153, "lines": tops, "label": f"revealed t {times[153]:.3f} s"},
])], os.path.join(AFTER, "strip-reveal-in-place-light.png"), 540, title="Catalog Loading page: redacted to revealed")

# 10. Right to left: the band enters at the right edge and moves left (after only).
rec = os.path.join(REC_A, "cat-loading-light-rtl.mp4")
ia, ta = frames_between(rec, 1.348 + 0.1, 2.554 - 0.1, 5)
compose([("AFTER (46ec781): --ez rtl true, one sweep", cells(rec, ia, ta, (0, 300, 720, 1060)))],
        os.path.join(AFTER, "strip-rtl-light.png"), 432, title="Catalog Loading page, right to left")
print("strips written")

#!/usr/bin/env python3
"""
Checks a cold start of the sample list (REPORT.md section 5.4 step 5) from a screen recording.

    coldstart.py REC.mp4 OUT_PREFIX --theme light|dark --column X --area x0,y0,x1,y1 [--until 4.0]

Per frame it counts bone runs (horizontal runs of 24+ px in the bone's luma range) and glyph pixels
(text: darker than 110 in light, brighter than 150 in dark) inside --area, and reads the card edges
down --column (a column inside the cards that no text or bone reaches). A frame is "skeleton" with
5+ bone runs and almost no glyphs, "content" with many glyphs and no bones, "both" with both.

It reports when the skeleton shows and goes, whether any frame shows both states, how far each card
moves at the swap, and whether anything is drawn above the first card while the skeleton shows: the
band above it (from y 60, past the status bar's clock, to the first card's top less 4 px) must be
the same in every skeleton frame as in the first content frame.
"""

import argparse
import json

from media import gray_frames, pts, size

parser = argparse.ArgumentParser()
parser.add_argument("rec")
parser.add_argument("out")
parser.add_argument("--theme", choices=["light", "dark"], required=True)
parser.add_argument("--column", type=int, required=True)
parser.add_argument("--area", required=True)
parser.add_argument("--until", type=float, default=4.5)
parser.add_argument("--bone", default=None, help="bone luma range lo,hi")
parser.add_argument("--title", default="160,280,500", help="the large title's rows y0,y1 and width x1")
parser.add_argument("--margin-x", type=int, default=12, help="a column of bare page beside the cards")
args = parser.parse_args()

w, h = size(args.rec)
times = pts(args.rec)
ax0, ay0, ax1, ay1 = (int(v) for v in args.area.split(","))
light = args.theme == "light"
lo, hi = (int(v) for v in args.bone.split(",")) if args.bone else ((200, 238) if light else (38, 72))


def glyph(v):
    return v < 110 if light else v > 150


def bone_runs(frame):
    runs = 0
    for y in range(ay0, ay1, 6):
        run = 0
        row = y * w
        for x in range(ax0, ax1, 2):
            if lo <= frame[row + x] <= hi:
                run += 2
            else:
                if run >= 24:
                    runs += 1
                run = 0
        if run >= 24:
            runs += 1
    return runs


def glyphs(frame):
    return sum(1 for y in range(ay0, ay1, 6) for x in range(ax0, ax1, 2) if glyph(frame[y * w + x]))


def card_runs(frame):
    column = [frame[y * w + args.column] for y in range(h)]
    margin = [frame[y * w + args.margin_x] for y in range(h)]
    background = max(set(margin[ay0:ay1]), key=margin[ay0:ay1].count)
    runs, start = [], None
    for y in range(ay0, ay1):
        on = abs(column[y] - margin[y]) > 3
        if on and start is None:
            start = y
        elif not on and start is not None:
            if y - start >= 30:
                runs.append((start, y - 1))
            start = None
    if start is not None and ay1 - start >= 30:
        runs.append((start, ay1 - 1))
    return background, runs


frames = []
kept = {}
for index, frame in enumerate(gray_frames(args.rec, w, h)):
    if times[index] > args.until:
        break
    b, g = bone_runs(frame), glyphs(frame)
    state = "both" if b >= 5 and g >= 200 else "skeleton" if b >= 5 and g < 60 else "content" if g >= 200 else "other"
    background, runs = card_runs(frame)
    frames.append({"index": index, "t": round(times[index], 4), "bone_runs": b, "glyph_px": g, "state": state,
                   "background": background, "cards": runs})
    if state in ("skeleton", "content", "both"):
        kept[index] = frame

# The skeleton is the longest run of consecutive skeleton frames (the launch animation can pass for
# one briefly); the swap is from its last frame to the next content frame.
best, run = [], []
for f in frames:
    run = run + [f] if f["state"] == "skeleton" else []
    if len(run) > len(best):
        best = run
skeleton = best
content = [f for f in frames if f["state"] == "content" and skeleton and f["index"] > skeleton[-1]["index"]]
summary = {"recording": args.rec, "theme": args.theme, "frames_read": len(frames)}
if skeleton and content:
    last_skeleton = skeleton[-1]
    first_content = content[0]
    between = [f for f in frames if last_skeleton["index"] < f["index"] < first_content["index"]]
    summary["frames_between_skeleton_and_content"] = [(f["index"], f["t"], f["state"]) for f in between]
    summary.update({
        "skeleton_from_s": skeleton[0]["t"], "skeleton_to_s": last_skeleton["t"],
        "skeleton_shown_s": round(first_content["t"] - skeleton[0]["t"], 3),
        "last_skeleton_frame": last_skeleton["index"], "first_content_frame": first_content["index"],
        "swap_gap_ms": round((first_content["t"] - last_skeleton["t"]) * 1000, 1),
        "cards_skeleton": last_skeleton["cards"], "cards_content": first_content["cards"],
    })
    shifts = []
    for a, b in zip(last_skeleton["cards"], first_content["cards"]):
        shifts.append({"top": b[0] - a[0], "bottom": b[1] - a[1]})
    summary["card_shift_px"] = shifts
    # Nothing above the first card while the skeleton shows: compare with the first content frame.
    top_limit = (first_content["cards"][0][0] if first_content["cards"] else ay0) - 4
    reference = kept[first_content["index"]]
    # Only frames after the launch animation: the large title (rows --title) must already be drawn as in
    # the content frame (at most 20 sampled pixels off by more than 12), or the window's own fade-in is
    # what differs.
    ty0, ty1, tx1 = (int(v) for v in args.title.split(","))

    def title_difference(frame):
        return sum(1 for y in range(ty0, ty1, 2) for x in range(0, tx1, 2)
                   if abs(frame[y * w + x] - reference[y * w + x]) > 12)
    settled = [f for f in skeleton if title_difference(kept[f["index"]]) <= 20]
    summary["settled_skeleton_frames"] = len(settled)
    summary["settled_skeleton_from_s"] = settled[0]["t"] if settled else None
    worst, total = [(0, None)], 0
    for f in settled:
        frame = kept[f["index"]]
        changed = total = 0
        for y in range(60, top_limit, 2):
            for x in range(0, w, 2):
                if x < 130 and y < 140:
                    continue        # the phone's refresh-rate overlay, which changes on its own
                total += 1
                if abs(frame[y * w + x] - reference[y * w + x]) > 12:
                    changed += 1
        worst.append((changed, f["index"]))
    worst.sort(key=lambda e: e[0], reverse=True)
    summary["above_first_card"] = {"rows": [60, top_limit], "sampled_px": total,
                                   "most_changed_px_in_a_skeleton_frame": worst[0][0], "at_frame": worst[0][1]}
    # Do the bones move? Largest change of any bone-range pixel between skeleton frames.
    if len(settled) >= 2:
        first = kept[settled[0]["index"]]
        moved = 0
        for f in settled[1:]:
            frame = kept[f["index"]]
            for y in range(ay0, ay1, 6):
                for x in range(ax0, ax1, 2):
                    v = first[y * w + x]
                    if lo <= v <= hi:
                        moved = max(moved, abs(frame[y * w + x] - v))
        summary["bone_max_change_between_settled_skeleton_frames"] = moved
with open(args.out + ".frames.tsv", "w") as handle:
    handle.write("index\tt\tstate\tbone_runs\tglyph_px\tcards\n")
    for f in frames:
        handle.write(f"{f['index']}\t{f['t']}\t{f['state']}\t{f['bone_runs']}\t{f['glyph_px']}\t{f['cards']}\n")
with open(args.out + ".json", "w") as handle:
    json.dump(summary, handle, indent=1)
print(json.dumps(summary, indent=1))

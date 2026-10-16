#!/usr/bin/env python3
"""
The skeleton's colours as the phone draws them, from lossless screencaps (REPORT.md section 5.4 step 4).

    colours.py --area x0,y0,x1,y1 --card-x X --page-x X --bone RRGGBB --highlight RRGGBB SHOT.png...

Reads the page colour down column --page-x and the card colour down column --card-x (the modes inside
--area), counts the pixels of the bone colour (exactly, +-1 per channel), and finds the brightest pixel
on the bone's lift line (every channel between the bone and highlight tokens, +-2) in each shot: the
band's peak where the band is on a bone. Prints CIELAB L* for each and the differences the report's
colour rule names: bone to every surface >= 7, highlight minus bone 4.5..6, highlight to every
surface >= 3 in light.
"""

import argparse
import json
from collections import Counter

from media import hex_rgb, rgb_image, srgb_to_lab_l

parser = argparse.ArgumentParser()
parser.add_argument("shots", nargs="+")
parser.add_argument("--area", required=True)
parser.add_argument("--card-x", type=int, required=True)
parser.add_argument("--page-x", type=int, required=True)
parser.add_argument("--bone", required=True)
parser.add_argument("--highlight", required=True)
parser.add_argument("--out", default=None)
args = parser.parse_args()

x0, y0, x1, y1 = (int(v) for v in args.area.split(","))
bone, high = hex_rgb(args.bone), hex_rgb(args.highlight)
lo = [min(a, b) - 2 for a, b in zip(bone, high)]
hi = [max(a, b) + 2 for a, b in zip(bone, high)]


def lstar(rgb):
    return round(srgb_to_lab_l(*rgb), 2)


result = {"bone_token": args.bone, "highlight_token": args.highlight, "shots": []}
pages, cards = Counter(), Counter()
peak_overall = None
for path in args.shots:
    w, h, raw = rgb_image(path)

    def px(x, y):
        o = (y * w + x) * 3
        return raw[o], raw[o + 1], raw[o + 2]

    page = Counter(px(args.page_x, y) for y in range(y0, y1)).most_common(1)[0][0]
    card = Counter(px(args.card_x, y) for y in range(y0, y1)).most_common(1)[0][0]
    pages[page] += 1
    cards[card] += 1
    exact = peak = 0
    peak_rgb = None
    for y in range(y0, y1, 2):
        for x in range(x0, x1, 2):
            p = px(x, y)
            if all(abs(a - b) <= 1 for a, b in zip(p, bone)):
                exact += 1
            if all(l <= c <= u for c, l, u in zip(p, lo, hi)):
                luma = 0.2126 * p[0] + 0.7152 * p[1] + 0.0722 * p[2]
                if luma > peak:
                    peak, peak_rgb = luma, p
    shot = {"shot": path.split("/")[-1], "page": "%02X%02X%02X" % page, "card": "%02X%02X%02X" % card,
            "bone_px_exact": exact, "peak": ("%02X%02X%02X" % peak_rgb) if peak_rgb else None,
            "peak_lift_L": round(lstar(peak_rgb) - lstar(bone), 2) if peak_rgb else None}
    result["shots"].append(shot)
    if peak_rgb and (peak_overall is None or lstar(peak_rgb) > lstar(peak_overall)):
        peak_overall = peak_rgb

page = pages.most_common(1)[0][0]
card = cards.most_common(1)[0][0]
result["L"] = {"page": lstar(page), "card": lstar(card), "bone_token": lstar(bone), "highlight_token": lstar(high),
               "brightest_on_lift_line": lstar(peak_overall) if peak_overall else None}
result["page"] = "%02X%02X%02X" % page
result["card"] = "%02X%02X%02X" % card
result["brightest_on_lift_line"] = ("%02X%02X%02X" % peak_overall) if peak_overall else None
result["rule"] = {
    "bone_to_page": round(abs(lstar(bone) - lstar(page)), 2),
    "bone_to_card": round(abs(lstar(bone) - lstar(card)), 2),
    "highlight_lift_token": round(lstar(high) - lstar(bone), 2),
    "highlight_lift_measured": round(lstar(peak_overall) - lstar(bone), 2) if peak_overall else None,
    "highlight_to_page": round(abs(lstar(high) - lstar(page)), 2),
    "highlight_to_card": round(abs(lstar(high) - lstar(card)), 2),
}
text = json.dumps(result, indent=1)
print(text)
if args.out:
    open(args.out, "w").write(text)

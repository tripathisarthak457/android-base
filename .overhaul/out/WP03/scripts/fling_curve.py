"""
REQUIREMENTS 41 on the phone: the catalog's Deceleration lab flung at a range of speeds, each fling's
distance and duration (as the app measured them on the device) set against UIScrollView's curve for
the same release, then a fling back into the list's top recorded for the edge.

    fling_curve.py --serial 9a7c74f4 --package com.example.wp03.look.catalog --out DIR

Run it under the adb lock with the phone awake and unlocked (with_phone.sh). Writes DIR/fling.md,
DIR/fling.json and DIR/edge/ (video, frames, strips). Exits 1 when a fling's distance strays from the
curve by more than 5 % or its duration by more than 10 %.
"""

import argparse
import importlib.util
import json
import os
import re
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("cp1_record", os.path.join(HERE, "cp1_record.py"))
cp1 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cp1)

GFX_LINES = re.compile(
    r"^\s*((?:Total frames rendered|Janky frames(?: \(legacy\))?|\d+th (?:gpu )?percentile)): (.*)$",
    re.MULTILINE,
)
READOUT = re.compile(r"Released at (\d+) dp/s: (\d+) dp in ([\d.]+) s\. iOS: (\d+) dp in ([\d.]+) s\.")
# Swipe times in milliseconds over the same distance: a slow drag-and-release up to a hard throw.
SWIPES = (700, 400, 250, 160, 110, 80)
DISTANCE_TOLERANCE = 0.05
DURATION_TOLERANCE = 0.10


def reveal_lab(device):
    """
    Scrolls the page slowly until the lab sits fully on screen. Once the lab shows, the page is dragged by
    what lies above it: a drag that lands on the lab scrolls the lab's own list, not the page.
    """
    width, height = device.size()
    x = width // 2
    top_limit, bottom_limit = int(height * 0.15), height - int(height * 0.12) - 160
    for _ in range(30):
        nodes = device.nodes()
        box = nodes.get("fling_lab")
        if box and box[1] >= top_limit and box[3] <= bottom_limit:
            return box, nodes
        if box is None:
            start, end = int(height * 0.8), int(height * 0.4)
        elif box[1] < top_limit:
            start = min(box[3] + 40, height - int(height * 0.12))
            end = start + (top_limit - box[1]) + 40
        else:
            start = box[1] - 40
            end = max(start - (box[3] - bottom_limit + 40), int(height * 0.12))
        device.shell(f"input swipe {x} {start} {x} {end} 1200")
        time.sleep(1.0)
    raise SystemExit("fling: could not bring the lab on screen")


def readout(device):
    for key in device.nodes():
        match = READOUT.search(key)
        if match:
            return match.group(0), [float(v) for v in match.groups()]
    return None, None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    device = cp1.Device(args.serial)
    cp1.require_unlocked(device)
    cp1.launch(device, args.package, "Motion")
    box, _ = reveal_lab(device)
    x = (box[0] + box[2]) // 2
    start, end = box[3] - 60, box[1] + 60

    device.shell(f"dumpsys gfxinfo {args.package} reset >/dev/null")
    runs, previous, failed = [], None, False
    for millis in SWIPES:
        device.shell(f"input swipe {x} {start} {x} {end} {millis}")
        time.sleep(5.0)
        text, values = readout(device)
        if values is None or text == previous:
            print(f"swipe {millis} ms: no new fling reported ({text})")
            failed = True
            continue
        previous = text
        velocity, distance, seconds, ios_distance, ios_seconds = values
        distance_error = (distance - ios_distance) / ios_distance
        duration_error = (seconds - ios_seconds) / ios_seconds
        ok = abs(distance_error) <= DISTANCE_TOLERANCE and abs(duration_error) <= DURATION_TOLERANCE
        failed |= not ok
        runs.append({
            "swipe_ms": millis, "velocity_dp_s": velocity, "distance_dp": distance, "seconds": seconds,
            "ios_distance_dp": ios_distance, "ios_seconds": ios_seconds,
            "distance_error": round(distance_error, 4), "duration_error": round(duration_error, 4), "ok": ok,
        })
        print(f"swipe {millis} ms: {text} ({distance_error:+.1%} distance, {duration_error:+.1%} duration)")

    # The app-wide summary comes first; each window's own block repeats the names after it.
    frames = list(dict(reversed(GFX_LINES.findall(device.shell(f"dumpsys gfxinfo {args.package}")))).items())[::-1]

    # The edge: from a fresh list a short fling down it, then a hard throw back that reaches the top
    # with speed to spare, which the rubber band takes and springs back.
    cp1.launch(device, args.package, "Motion")
    box, _ = reveal_lab(device)
    cp1.record(
        device, args.out, "edge",
        [f"input swipe {x} {start} {x} {end} 400", 3.5, f"input swipe {x} {end} {x} {start} 70"],
        seconds=8, focus=box,
    )

    with open(os.path.join(args.out, "fling.json"), "w") as handle:
        json.dump(runs, handle, indent=2)
    lines = [
        "# Fling against iOS's curve (catalog Deceleration lab, release build)",
        "",
        f"Package `{args.package}`, device `{args.serial}`. Distance and duration are measured by the app;",
        "the iOS columns are UIScrollView.DecelerationRate.normal (0.998 per ms) for the same release speed.",
        "",
        "| Swipe | Release (dp/s) | Distance (dp) | iOS (dp) | Error | Duration (s) | iOS (s) | Error |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for run in runs:
        lines.append(
            f"| {run['swipe_ms']} ms | {run['velocity_dp_s']:.0f} | {run['distance_dp']:.0f} | {run['ios_distance_dp']:.0f} "
            f"| {run['distance_error']:+.1%} | {run['seconds']:.2f} | {run['ios_seconds']:.2f} | {run['duration_error']:+.1%} |"
        )
    lines += ["", "Frames over the flings (`dumpsys gfxinfo`, reset before the first):", ""]
    lines += [f"- {name}: {value.strip()}" for name, value in frames]
    lines += ["", "Edge: `edge/edge.mp4`, `edge/edge_crop_strip.png` (a short fling down, then a throw back into the top)."]
    with open(os.path.join(args.out, "fling.md"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(lines) + "\n")
    print("FLING CURVE " + ("FAILED" if failed or not runs else "OK"))
    return 1 if failed or not runs else 0


if __name__ == "__main__":
    sys.exit(main())

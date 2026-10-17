#!/usr/bin/env python3
"""
Screenshots of every catalog section, scrolled through to the end, for a visual check.

    catalog_shots.py --serial emulator-5554 --package com.example.wp03.cp1.catalog --out DIR
                     [--theme light|dark] [--font-scale 2.0] [--suffix light] [section...]

Run it under the adb lock. Each page is shot, scrolled slowly by most of a screen, and shot again
until a shot repeats. A tile per section (`<section>-<suffix>.png`, the shots side by side) is
written next to the raw shots.
"""

import argparse
import hashlib
import os
import subprocess
import sys
import time

SECTIONS = [
    "Foundations", "Feel", "Text", "Buttons", "Inputs", "Selection", "Lists", "Containers", "Bars",
    "Overlays", "Sheets", "Feedback", "Loading", "Motion", "Adaptive", "Pickers", "Fields", "Data", "Collections",
]


def adb(serial, *args, binary=False):
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, check=True)
    return result.stdout if binary else result.stdout.decode()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--theme", default="light")
    parser.add_argument("--font-scale", type=float, default=1.0)
    parser.add_argument("--suffix", default="light")
    parser.add_argument("--max-shots", type=int, default=8)
    parser.add_argument("sections", nargs="*", default=SECTIONS)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    activity = adb(args.serial, "shell",
                   f"cmd package resolve-activity --brief -a android.intent.action.MAIN "
                   f"-c android.intent.category.LAUNCHER {args.package}").strip().splitlines()[-1].strip()
    size = adb(args.serial, "shell", "wm size").strip().splitlines()[-1].split()[-1]
    width, height = (int(v) for v in size.split("x"))
    for section in args.sections:
        adb(args.serial, "shell", f"am force-stop {args.package}")
        adb(args.serial, "shell",
            f"am start -W -n {activity} --es section {section} --es theme {args.theme} --ef fontScale {args.font_scale}")
        time.sleep(2.0)
        shots, seen = [], set()
        for index in range(args.max_shots):
            png = adb(args.serial, "exec-out", "screencap -p", binary=True)
            digest = hashlib.sha1(png).hexdigest()
            if digest in seen:
                break
            seen.add(digest)
            path = os.path.join(args.out, f"{section}-{args.suffix}-{index}.png")
            with open(path, "wb") as handle:
                handle.write(png)
            shots.append(path)
            x = width // 2
            # Slow enough that nothing flings, so consecutive shots overlap a little.
            adb(args.serial, "shell", f"input swipe {x} {int(height * 0.85)} {x} {int(height * 0.25)} 900")
            time.sleep(1.2)
        tile = os.path.join(args.out, f"{section}-{args.suffix}.png")
        inputs = sum((["-i", p] for p in shots), [])
        subprocess.run(["ffmpeg", "-v", "error", "-y", *inputs, "-filter_complex",
                        "".join(f"[{i}:v]scale=432:-1[s{i}];" for i in range(len(shots)))
                        + "".join(f"[s{i}]" for i in range(len(shots))) + f"hstack=inputs={len(shots)}"
                        if len(shots) > 1 else "[0:v]scale=432:-1", tile], check=True)
        print(f"{section}: {len(shots)} shots -> {tile}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Where the moving screen's edge is in each frame of a record.sh push/pop/gesture_pop folder.

    python3 cp1_edge.py <record dir> push|pop [--resp 0.344]

Reads frames/f_NNNN.png and frames.json from the folder. On row y = 75 of the 720 x 1600 frames the detail
screen's top bar is white (>= 248) and the covered list's header is not, so the edge is the left end of the white
run that reaches the right side of the screen. Prints, per frame: time after the first recorded change, edge (px of
720), travel done, and with --resp the spring time that travel corresponds to and how far the spring's clock runs
ahead of the frame ("lead"; about 0 when the clock starts after the composing frame).
"""
import argparse
import json
import math
import os
import subprocess


def row(path, y=75, h=5):
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-vf", f"crop=720:{h}:0:{y},format=gray",
                          "-f", "rawvideo", "pipe:"], capture_output=True, check=True).stdout
    return [sorted(raw[r * 720 + x] for r in range(h))[h // 2] for x in range(720)]


def white_edge(r):
    if r[719] < 248 and r[715] < 248:
        return 720
    x, misses = 719, 0
    while x > 0:
        if r[x - 1] < 248:
            misses += 1
            if misses > 3:
                break
        else:
            misses = 0
        x -= 1
    return x + misses


def travel(t, response):
    w = 2 * math.pi / response
    return 1 - math.exp(-w * t) * (1 + w * t)


def spring_time(p, response):
    lo, hi = 0.0, 3.0
    for _ in range(80):
        mid = (lo + hi) / 2
        lo, hi = (mid, hi) if travel(mid, response) < p else (lo, mid)
    return lo


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("folder")
    ap.add_argument("kind", choices=["push", "pop"])
    ap.add_argument("--resp", type=float)
    a = ap.parse_args()
    times = json.load(open(os.path.join(a.folder, "frames.json")))["times"]
    for i in range(1, len(times) + 1):
        e = white_edge(row(os.path.join(a.folder, "frames", f"f_{i:04d}.png")))
        p = 1 - e / 720 if a.kind == "push" else e / 720
        ms = (times[i - 1] - times[1]) * 1000
        extra = ""
        if a.resp and 0.005 < p < 0.995:
            ts = spring_time(p, a.resp) * 1000
            extra = f"  spring_t={ts:6.1f} ms  lead={ts - ms:6.1f} ms"
        print(f"f{i:<4d}{ms:8.1f} ms  edge={e:4d}  travel={p:.3f}{extra}")


if __name__ == "__main__":
    main()

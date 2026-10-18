"""
Is the catalog's first frame already its settled one? No text swapping font, nothing moving. A cold
start recorded with window animations off, so the window appears at full size in one frame; every
frame after the first that shows the index is compared with the last one over the title and the
first rows.

    font_swap.py --serial 9a7c74f4 --package com.example.wp03.look.catalog --out DIR

Run it under the adb lock with the phone awake and unlocked (with_phone.sh). The two animation
scales are put back as they were. Writes DIR/font_swap.mp4 and DIR/font_swap.txt; exits 1 when a
frame after the first differs.
"""

import argparse
import os
import subprocess
import sys
import time

WIDTH, HEIGHT = 720, 1600
# The large title, its subtitle and the first rows of the index, in recording pixels.
BAND = (0, 100, 720, 520)
# Mean absolute difference (0-255 levels) above which a frame is not the settled one. Encoder noise
# on a still screen stays well under one level.
CHANGED = 1.5


def adb(serial, *args):
    return subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, check=True).stdout.strip()


def frames(video):
    x, y, w, h = BAND
    raw = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", video, "-fps_mode", "passthrough", "-vf", f"crop={w}:{h}:{x}:{y},format=gray",
         "-f", "rawvideo", "-"],
        capture_output=True, check=True,
    ).stdout
    size = w * h
    return [raw[i:i + size] for i in range(0, len(raw) - size + 1, size)]


def textured(shot):
    """Text on a plain background: some of the band's pixels stand well apart from its median."""
    sample = shot[::7]
    median = sorted(sample)[len(sample) // 2]
    return sum(1 for p in sample if abs(p - median) > 40) > len(sample) * 0.005


def difference(a, b):
    return sum(abs(p - q) for p, q in zip(a[::7], b[::7])) / len(a[::7])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    serial = args.serial
    activity = adb(serial, "shell", "cmd package resolve-activity --brief -a android.intent.action.MAIN "
                   f"-c android.intent.category.LAUNCHER {args.package}").splitlines()[-1].strip()
    scales = {key: adb(serial, "shell", f"settings get global {key}")
              for key in ("window_animation_scale", "transition_animation_scale")}
    remote = "/sdcard/font_swap.mp4"
    video = os.path.join(args.out, "font_swap.mp4")
    try:
        for key in scales:
            adb(serial, "shell", f"settings put global {key} 0")
        adb(serial, "shell", f"am force-stop {args.package}")
        adb(serial, "shell", f"rm -f {remote}")
        recorder = subprocess.Popen(
            ["adb", "-s", serial, "shell", f"screenrecord --time-limit 5 --size {WIDTH}x{HEIGHT} --bit-rate 16000000 {remote}"],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        time.sleep(1.0)
        adb(serial, "shell", f"am start -n {activity} --es theme light")
        recorder.wait(timeout=40)
        time.sleep(1.0)
        adb(serial, "pull", remote, video)
        adb(serial, "shell", f"rm -f {remote}")
    finally:
        for key, value in scales.items():
            if value == "null":
                adb(serial, "shell", f"settings delete global {key}")
            else:
                adb(serial, "shell", f"settings put global {key} {value}")

    shots = frames(video)
    settled = shots[-1]
    diffs = [difference(shot, settled) for shot in shots]
    # The catalog's first frame: the band no longer shows what was in front before the launch, and it
    # holds text rather than a splash screen's flat colour. Every frame after it must look settled.
    first = next((i for i, shot in enumerate(shots) if difference(shot, shots[0]) > CHANGED and textured(shot)), None)
    lines = [f"{len(shots)} frames; band {BAND}; differences to the last frame: "
             + " ".join(f"{d:.1f}" for d in diffs)]
    if first is None:
        lines.append("FONT SWAP CHECK FAILED: the catalog never showed its index")
        ok = False
    else:
        later = [(i + 1, round(d, 1)) for i, d in enumerate(diffs[first:], start=first) if d > CHANGED]
        lines.append(f"first catalog frame: {first + 1}; frames after it that differ: {later}")
        ok = not later
        lines.append("FIRST FRAME SETTLED" if ok else "CHANGED AFTER THE FIRST FRAME: a font swap or a layout jump")
    with open(os.path.join(args.out, "font_swap.txt"), "w") as handle:
        handle.write("\n".join(lines) + "\n")
    print("\n".join(lines[1:]))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())

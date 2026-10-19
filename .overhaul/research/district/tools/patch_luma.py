"""Mean grey level of one rectangle of a screen recording, per frame: how a dim (scrim) fades in and out.

Standard library only; ffmpeg and ffprobe on PATH.

    python -I patch_luma.py rec.mp4 --rect 0.05:0.05:0.30:0.12 --scrim 13 > dim.csv

--rect is left:top:width:height as fractions of the frame; pick a patch the sheet never covers and whose content does
not change (a flat part of the screen behind). With --scrim (the scrim colour's grey level, 0 for black), the CSV also
estimates the scrim's alpha against the first frame: alpha = (L0 - L) / (L0 - scrim). Android blends in gamma space,
so this holds on the recorded grey levels.
"""

import argparse
import csv
import os
import subprocess
import sys

sys.dont_write_bytecode = True  # no __pycache__ beside the tools
# python -I leaves the script's own folder off the path; the tools folder holds only these scripts, never recordings.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from edge_track import frame_times, video_size  # noqa: E402


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("video")
    parser.add_argument("--rect", required=True, help="left:top:width:height as fractions of the frame")
    parser.add_argument("--scrim", type=float, help="the scrim colour's grey level (0 = black)")
    args = parser.parse_args()

    left, top, width, height = (float(part) for part in args.rect.split(":"))
    frame_width, frame_height = video_size(args.video)
    crop = (f"crop={max(1, int(frame_width * width))}:{max(1, int(frame_height * height))}:"
            f"{int(frame_width * left)}:{int(frame_height * top)},scale=1:1:flags=area,format=gray")
    times = frame_times(args.video)
    raw = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", args.video, "-fps_mode", "passthrough", "-vf", crop, "-f", "rawvideo", "-"],
        check=True, capture_output=True,
    ).stdout
    writer = csv.writer(sys.stdout, lineterminator="\n")
    writer.writerow(["frame", "t", "luma", "alpha"])
    baseline = raw[0] if raw else 0
    for index, level in enumerate(raw):
        t = times[index] if index < len(times) else float("nan")
        alpha = ""
        if args.scrim is not None and baseline != args.scrim:
            alpha = f"{(baseline - level) / (baseline - args.scrim):.3f}"
        writer.writerow([index, f"{t:.6f}", level, alpha])


if __name__ == "__main__":
    main()

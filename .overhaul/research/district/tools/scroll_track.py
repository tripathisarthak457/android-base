"""Scroll position of a list through a screen recording, from how far its content shifts between frames.

Standard library only; ffmpeg and ffprobe on PATH. For a fling or an overscroll, where no single edge stays on screen.

    python -I scroll_track.py rec.mp4 --strip 0.40:0.60 --window 0.12:0.88 > scroll.csv
    python -I fit_motion.py scroll.csv --swipe-ms 100 --decay

Between each pair of frames the strip's column of grey levels is matched against itself shifted by every whole number
of pixels within --max-shift (coarse to fine), and the best match, refined to a sub-pixel, is that frame's move.
--window must hold only content that scrolls rigidly: leave out the status bar, a sticky header, the tab bar and
anything that fades. Positions grow as content moves up the screen (a scroll toward the end of the list).
"""

import argparse
import csv
import os
import sys

sys.dont_write_bytecode = True  # no __pycache__ beside the tools
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from edge_track import frame_times, strip_profiles, video_size  # noqa: E402


def mismatch(previous, current, shift, low, high, stride):
    """Mean absolute difference between current[y] and previous[y + shift] over the window."""
    total, count = 0, 0
    start = max(low, low - shift)
    end = min(high, high - shift)
    for y in range(start, end, stride):
        total += abs(current[y] - previous[y + shift])
        count += 1
    return total / count if count else float("inf")


def best_shift(previous, current, low, high, reach):
    coarse = min(range(-reach, reach + 1, 4), key=lambda s: mismatch(previous, current, s, low, high, 3))
    fine = range(max(-reach, coarse - 4), min(reach, coarse + 4) + 1)
    scores = {s: mismatch(previous, current, s, low, high, 1) for s in fine}
    shift = min(scores, key=scores.get)
    before, at, after = scores.get(shift - 1), scores[shift], scores.get(shift + 1)
    if before is None or after is None:
        return float(shift), at
    denominator = before - 2 * at + after
    offset = 0.5 * (before - after) / denominator if denominator > 0 else 0.0
    return shift + max(-0.5, min(0.5, offset)), at


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("video")
    parser.add_argument("--strip", default="0.40:0.60", help="columns to average, as fractions of the width")
    parser.add_argument("--window", default="0.12:0.88", help="rows that scroll rigidly, as fractions of the height")
    parser.add_argument("--max-shift", type=int, default=220, help="largest move between two frames, in px")
    args = parser.parse_args()

    strip_start, strip_end = (float(part) for part in args.strip.split(":"))
    window_start, window_end = (float(part) for part in args.window.split(":"))
    size = video_size(args.video)
    times = frame_times(args.video)
    low, high = int(size[1] * window_start), int(size[1] * window_end)

    writer = csv.writer(sys.stdout, lineterminator="\n")
    writer.writerow(["frame", "t", "pos", "shift", "mismatch"])
    position, previous = 0.0, None
    for index, profile in enumerate(strip_profiles(args.video, "y", strip_start, strip_end, size)):
        t = times[index] if index < len(times) else float("nan")
        if previous is None:
            writer.writerow([index, f"{t:.6f}", "0.00", "0.00", ""])
        else:
            shift, score = best_shift(previous, profile, low, high, args.max_shift)
            position += shift
            writer.writerow([index, f"{t:.6f}", f"{position:.2f}", f"{shift:.2f}", f"{score:.2f}"])
        previous = profile


if __name__ == "__main__":
    main()

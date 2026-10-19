"""Tracks a horizontal edge (a sheet's top, a page's leading edge turned on its side) through a screen recording.

Standard library only; the pixels come from ffmpeg and the frame times from ffprobe, both on PATH.

For every frame, a narrow vertical strip of the video (or a horizontal one with --axis x) is averaged to one column of
grey levels. The edge is the strongest step in that column with the requested polarity, searched near where it was in
the previous frame, so a sheet is followed even when other edges (a header, a card) are stronger elsewhere.

    python -I edge_track.py rec.mp4 --strip 0.70:0.80 --polarity rise --start 1400 > track.csv

Output CSV: frame index, presentation time in seconds, edge position in recorded pixels (empty when lost), step size.
Use frame times, never frame indices: screenrecord writes a variable frame rate.
"""

import argparse
import csv
import subprocess
import sys


def frame_times(video):
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries",
         "frame=best_effort_timestamp_time,pts_time", "-of", "csv=p=0", video],
        check=True, capture_output=True, text=True,
    ).stdout
    times = []
    for line in out.splitlines():
        value = next((field for field in line.split(",") if field and field != "N/A"), None)
        times.append(float(value) if value is not None else float("nan"))
    return times


def video_size(video):
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height",
         "-of", "csv=p=0", video],
        check=True, capture_output=True, text=True,
    ).stdout.strip().splitlines()[0]
    width, height = (int(part) for part in out.split(",")[:2])
    return width, height


def strip_profiles(video, axis, start, end, size):
    """Yields one list of grey levels per frame: the strip averaged across its width."""
    width, height = size
    if axis == "y":
        left, span = int(width * start), max(1, int(width * (end - start)))
        crop = f"crop={span}:{height}:{left}:0,scale=1:{height}:flags=area"
        length = height
    else:
        top, span = int(height * start), max(1, int(height * (end - start)))
        crop = f"crop={width}:{span}:0:{top},scale={width}:1:flags=area"
        length = width
    process = subprocess.Popen(
        ["ffmpeg", "-v", "error", "-i", video, "-fps_mode", "passthrough", "-vf", f"{crop},format=gray",
         "-f", "rawvideo", "-"],
        stdout=subprocess.PIPE,
    )
    try:
        while True:
            chunk = process.stdout.read(length)
            if len(chunk) < length:
                break
            yield list(chunk)
    finally:
        process.stdout.close()
        process.wait()


def smooth(values, radius):
    if radius <= 0:
        return [float(value) for value in values]
    total = 0.0
    window = []
    result = []
    padded = [values[0]] * radius + list(values) + [values[-1]] * radius
    for index, value in enumerate(padded):
        window.append(value)
        total += value
        if len(window) > 2 * radius + 1:
            total -= window.pop(0)
        if index >= 2 * radius:
            result.append(total / len(window))
    return result


def steps(profile, reach):
    """Step at each position: mean of the `reach` cells after it minus the `reach` cells before it."""
    length = len(profile)
    prefix = [0.0]
    for value in profile:
        prefix.append(prefix[-1] + value)
    result = [0.0] * length
    for index in range(reach, length - reach):
        after = (prefix[index + reach] - prefix[index]) / reach
        before = (prefix[index] - prefix[index - reach]) / reach
        result[index] = after - before
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("video")
    parser.add_argument("--axis", choices=("y", "x"), default="y",
                        help="y: find a horizontal edge moving up or down (a sheet); x: a vertical edge (a page)")
    parser.add_argument("--strip", default="0.70:0.80",
                        help="the strip to average, as fractions across the other axis; keep it clear of text")
    parser.add_argument("--window", default="0.0:1.0", help="where along the axis the edge may be, as fractions")
    parser.add_argument("--polarity", choices=("rise", "fall", "any"), default="any",
                        help="rise: brighter after the edge (further down or right); fall: darker after it")
    parser.add_argument("--start", type=float, help="edge position in the first frame, in recorded px")
    parser.add_argument("--max-step", type=float, default=260.0,
                        help="largest move between two frames, in px (a fast fling moves ~150 px per 8 ms frame)")
    parser.add_argument("--min-step", type=float, default=6.0, help="weakest step that still counts as the edge")
    parser.add_argument("--smooth", type=int, default=2, help="box radius applied along the axis before the search")
    parser.add_argument("--reach", type=int, default=4, help="cells compared either side of a candidate edge")
    args = parser.parse_args()

    strip_start, strip_end = (float(part) for part in args.strip.split(":"))
    window_start, window_end = (float(part) for part in args.window.split(":"))
    size = video_size(args.video)
    times = frame_times(args.video)
    length = size[1] if args.axis == "y" else size[0]
    low, high = int(length * window_start), int(length * window_end)

    writer = csv.writer(sys.stdout, lineterminator="\n")
    writer.writerow(["frame", "t", "pos", "step"])
    previous = args.start
    for index, profile in enumerate(strip_profiles(args.video, args.axis, strip_start, strip_end, size)):
        grades = steps(smooth(profile, args.smooth), args.reach)
        if previous is None:
            lo, hi = low, high
        else:
            lo = max(low, int(previous - args.max_step))
            hi = min(high, int(previous + args.max_step) + 1)
        best, best_score = None, args.min_step
        for position in range(max(lo, args.reach), min(hi, length - args.reach)):
            grade = grades[position]
            score = grade if args.polarity == "rise" else -grade if args.polarity == "fall" else abs(grade)
            if score > best_score:
                best, best_score = position, score
        t = times[index] if index < len(times) else float("nan")
        if best is None:
            writer.writerow([index, f"{t:.6f}", "", ""])
            continue
        # Sub-pixel: the vertex of the parabola through the peak and its neighbours.
        before, peak, after = abs(grades[best - 1]), abs(grades[best]), abs(grades[best + 1])
        denominator = before - 2 * peak + after
        offset = 0.5 * (before - after) / denominator if denominator else 0.0
        position = best + max(-0.5, min(0.5, offset))
        previous = position
        writer.writerow([index, f"{t:.6f}", f"{position:.2f}", f"{grades[best]:.1f}"])


if __name__ == "__main__":
    main()

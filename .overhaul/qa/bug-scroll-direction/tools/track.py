"""
Which way, and how far, content moves in a screen recording: each frame's shift against the frame before,
along one axis, inside one box, found by matching the box's intensity profile.

    python -I track.py VIDEO --axis x|y --box X0,Y0,X1,Y1 [--max 120] [--tsv OUT.tsv]

The box is in the recording's pixels (720x1600 for a 1080x2400 phone); distances are reported in device pixels
(x1.5). A positive x shift is content moving right, a positive y shift content moving down. Prints the runs of
motion: each stretch of frames moving the same way, with its start and end time and distance, so a reversal
shows as a run of the other sign.
"""

import argparse
import subprocess

DEVICE_SCALE = 1080 / 720
COARSE = 4


def profiles(video, axis, box):
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    size = "%d:1" % w if axis == "x" else "1:%d" % h
    raw = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", video, "-fps_mode", "passthrough", "-vf",
         f"crop={w}:{h}:{x0}:{y0},format=gray,scale={size}:flags=area", "-f", "rawvideo", "-pix_fmt", "gray", "-"],
        capture_output=True, check=True,
    ).stdout
    n = w if axis == "x" else h
    frames = [list(raw[i:i + n]) for i in range(0, len(raw) - n + 1, n)]
    times = [
        float(v) for v in subprocess.run(
            ["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "frame=pts_time", "-of", "csv=p=0",
             video], capture_output=True, text=True, check=True,
        ).stdout.split() if v.strip()
    ]
    return frames, times[:len(frames)]


def cost(a, b, shift, lo, hi):
    total = 0
    for i in range(lo, hi):
        total += abs(a[i] - b[i + shift])
    return total / (hi - lo)


def best_shift(a, b, limit, candidates):
    n = len(a)
    lo, hi = limit, n - limit
    best, best_cost = 0, cost(a, b, 0, lo, hi)
    for s in sorted(candidates, key=abs):
        if s == 0 or abs(s) > limit:
            continue
        c = cost(a, b, s, lo, hi)
        if c < best_cost - 0.05:
            best, best_cost = s, c
    return best, best_cost


def shift(a, b, limit):
    """b's content is a's moved by the returned number of pixels."""
    small_a = [sum(a[i:i + COARSE]) / COARSE for i in range(0, len(a) - COARSE + 1, COARSE)]
    small_b = [sum(b[i:i + COARSE]) / COARSE for i in range(0, len(b) - COARSE + 1, COARSE)]
    small_limit = limit // COARSE + 1
    coarse, _ = best_shift(small_a, small_b, small_limit, range(-small_limit, small_limit + 1))
    centre = coarse * COARSE
    fine, fine_cost = best_shift(a, b, limit + COARSE, range(centre - COARSE, centre + COARSE + 1))
    if fine == 0 and centre != 0:
        fine, fine_cost = best_shift(a, b, limit + COARSE, [centre - 1, centre, centre + 1])
    return fine, fine_cost


def runs(steps, times, quiet=1):
    """Stretches of consecutive moving frames with one sign; still frames end a run."""
    out, current = [], None
    for index, step in enumerate(steps):
        sign = 0 if abs(step) <= quiet else (1 if step > 0 else -1)
        if sign == 0:
            if current:
                out.append(current)
                current = None
            continue
        if current and current["sign"] == sign:
            current["end"] = times[index]
            current["distance"] += step
            current["frames"] += 1
        else:
            if current:
                out.append(current)
            current = {"sign": sign, "start": times[index], "end": times[index], "distance": step, "frames": 1}
    if current:
        out.append(current)
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("video")
    parser.add_argument("--axis", choices=["x", "y"], required=True)
    parser.add_argument("--box", required=True)
    parser.add_argument("--max", type=int, default=120)
    parser.add_argument("--tsv")
    args = parser.parse_args()
    box = tuple(int(v) for v in args.box.split(","))
    frames, times = profiles(args.video, args.axis, box)
    steps, position, rows = [0.0], 0.0, []
    for index in range(1, len(frames)):
        s, c = shift(frames[index - 1], frames[index], args.max)
        step = s * DEVICE_SCALE
        steps.append(step)
        position += step
        rows.append((index, times[index], step, position, c))
    if args.tsv:
        with open(args.tsv, "w", encoding="utf-8", newline="\n") as handle:
            handle.write("frame\ttime_s\tstep_px\tposition_px\tmatch_cost\n")
            handle.writelines(f"{i}\t{t:.4f}\t{s:.1f}\t{p:.1f}\t{c:.2f}\n" for i, t, s, p, c in rows)
    positive = "right" if args.axis == "x" else "down"
    negative = "left" if args.axis == "x" else "up"
    print(f"{len(frames)} frames; runs of motion (device px; + is {positive}):")
    for run in runs(steps, times):
        way = positive if run["sign"] > 0 else negative
        print(f"  {run['start']:7.3f}-{run['end']:7.3f} s  {way:5s} {abs(run['distance']):7.0f} px  {run['frames']:3d} frames")


if __name__ == "__main__":
    main()

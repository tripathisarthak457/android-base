"""Frame statistics from `dumpsys gfxinfo <package> [framestats]`.

Only the app's own block is read (the one before "Profile data"), so per-window sections and the
memory report that follow it never leak into the numbers. "Janky" is the FrameTimeline count: frames
that missed their present deadline. The legacy count flags every frame over 16 ms and means nothing at
120 Hz, so it is kept only for reference.

Several dumps of one scenario (one per transition, reset in between) add up exactly: frame counts and
the two histograms are summed, and percentiles are taken from the summed histograms the same way the
platform takes them from one.
"""

import json
import re
import sys

_PERCENTILES = (50, 90, 95, 99)


def parse(text):
    """The numbers of one dump, as a dict. Histograms are {bucket_ms: count}."""
    head = text.split("Profile data in ms:", 1)[0]

    def number(pattern, cast=int):
        match = re.search(pattern, head, re.MULTILINE)
        return cast(match.group(1)) if match else None

    janky = re.search(r"^Janky frames: (\d+) \(([\d.]+)%\)", head, re.MULTILINE)
    out = {
        "frames": number(r"^Total frames rendered: (\d+)") or 0,
        "janky": int(janky.group(1)) if janky else 0,
        "janky_pct_reported": float(janky.group(2)) if janky else None,
        "slow_ui_thread": number(r"^Number Slow UI thread: (\d+)"),
        "deadline_missed": number(r"^Number Frame deadline missed: (\d+)"),
        "histogram": _histogram(head, "HISTOGRAM"),
        "gpu_histogram": _histogram(head, "GPU HISTOGRAM"),
    }
    for p in _PERCENTILES:
        out[f"p{p}_reported"] = number(rf"^{p}th percentile: (\d+)ms")
        out[f"gpu_p{p}_reported"] = number(rf"^{p}th gpu percentile: (\d+)ms")
    return out


def _histogram(head, name):
    match = re.search(rf"^{name}: (.*)$", head, re.MULTILINE)
    if not match:
        return {}
    buckets = {}
    for pair in match.group(1).split():
        bucket, count = pair.split("=")
        buckets[int(bucket.rstrip("ms"))] = int(count)
    return buckets


def percentile(histogram, p):
    """The bucket holding the p-th percentile, as the platform reports it (None when empty)."""
    total = sum(histogram.values())
    if total == 0:
        return None
    # HWUI's ProfileData::findPercentile: walk down from the slowest bucket until the frames above
    # the percentile's position are used up. Integer division, as there.
    remaining = total - p * total // 100
    for bucket in sorted(histogram, reverse=True):
        remaining -= histogram[bucket]
        if remaining <= 0:
            return bucket
    return min(histogram)


def combine(dumps):
    """One summary for several parsed dumps of the same scenario."""
    frames = sum(d["frames"] for d in dumps)
    janky = sum(d["janky"] for d in dumps)
    histogram, gpu = {}, {}
    for d in dumps:
        for bucket, count in d["histogram"].items():
            histogram[bucket] = histogram.get(bucket, 0) + count
        for bucket, count in d["gpu_histogram"].items():
            gpu[bucket] = gpu.get(bucket, 0) + count
    out = {
        "frames": frames,
        "janky": janky,
        "janky_pct": round(100 * janky / frames, 2) if frames else 0.0,
    }
    for p in _PERCENTILES:
        out[f"p{p}_ms"] = percentile(histogram, p)
        out[f"gpu_p{p}_ms"] = percentile(gpu, p)
    return out


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit("usage: parse_gfx.py <gfxinfo dump>... (several dumps are combined)")
    parsed = []
    for path in sys.argv[1:]:
        with open(path, errors="ignore") as handle:
            parsed.append(parse(handle.read()))
    print(json.dumps(combine(parsed), indent=2))

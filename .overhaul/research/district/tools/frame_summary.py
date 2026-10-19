"""Frame timing of one scene from `dumpsys gfxinfo <package> framestats`, the same way for District and the catalog.

Standard library only. Each argument is one dump taken right after one run of a scene (gfxinfo reset before it).

    python -I frame_summary.py district/sheet_fling_r1.txt district/sheet_fling_r2.txt ...

Per dump and pooled over all dumps given:
  - frames, janky % (frames that missed their FrameTimeline deadline, which is what gfxinfo counts), frame time
    p50/p90/p99 (IntendedVsync to FrameCompleted) and GPU p90;
  - hitch ratio: ms per second of motion by which presented frames came later than one vsync after the frame before
    (Apple's measure; under 5 ms/s is good), from the display's own present times;
  - the refresh interval the frames were scheduled at (8.33 ms at 120 Hz, 16.67 ms at 60 Hz);
  - gfxinfo's own summary lines, echoed for the record.
Only rows with Flags 0 count (no window changes, render-thread-only or skipped frames), as in the template's
tools/perf/parse_framestats.py, whose definitions these follow.
"""

import argparse
import json

NS_PER_MS = 1_000_000
SUMMARY_KEYS = ("Total frames rendered", "Janky frames", "50th percentile", "90th percentile", "99th percentile",
                "90th gpu percentile", "Number Frame deadline missed")
MAX_HITCH_INTERVALS = 12


def parse(text):
    rows, summary, header, inside = [], {}, None, False
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("---PROFILEDATA---"):
            inside, header = not inside, None
            continue
        if not inside:
            key, _, value = stripped.partition(":")
            if key in SUMMARY_KEYS and key not in summary:
                summary[key] = value.strip()
            continue
        fields = stripped.rstrip(",").split(",")
        if fields[0] == "Flags":
            header = fields
            continue
        if header is None or len(fields) < len(header):
            continue
        try:
            row = dict(zip(header, (int(value) for value in fields[: len(header)])))
        except ValueError:
            continue
        if row.get("Flags") == 0:
            rows.append(row)
    rows.sort(key=lambda row: row["IntendedVsync"])
    return rows, summary


def percentile(values, share):
    if not values:
        return None
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, round(share * (len(ordered) - 1))))
    return round(ordered[index], 2)


def interval(row):
    return row.get("FrameInterval") or 16_666_667


def missed(row):
    deadline = row.get("FrameDeadline")
    return bool(deadline) and max(row["FrameCompleted"], row.get("GpuCompleted", 0)) > deadline


def hitch(rows):
    presented = sorted(row["DisplayPresentTime"] for row in rows if row.get("DisplayPresentTime", 0) > 0)
    if len(presented) < 2:
        return 0.0, 0.0
    period = interval(rows[0])
    late = motion = 0.0
    for earlier, later in zip(presented, presented[1:]):
        gap = later - earlier
        if gap > MAX_HITCH_INTERVALS * period:
            continue
        motion += gap
        if gap > 1.5 * period:
            late += gap - period
    return late / NS_PER_MS, motion / NS_PER_MS


def describe(rows):
    durations = [(row["FrameCompleted"] - row["IntendedVsync"]) / NS_PER_MS for row in rows]
    gpu = [(row["GpuCompleted"] - row["CommandSubmissionCompleted"]) / NS_PER_MS
           for row in rows if row.get("GpuCompleted", 0) > row.get("CommandSubmissionCompleted", 0) > 0]
    late, motion = hitch(rows)
    return {
        "frames": len(rows),
        "janky_pct": round(100 * sum(missed(row) for row in rows) / len(rows), 2) if rows else None,
        "frame_ms_p50": percentile(durations, 0.5),
        "frame_ms_p90": percentile(durations, 0.9),
        "frame_ms_p99": percentile(durations, 0.99),
        "gpu_ms_p90": percentile(gpu, 0.9),
        "hitch_ms_per_s": round(1000 * late / motion, 2) if motion else None,
        "interval_ms": percentile([interval(row) / NS_PER_MS for row in rows], 0.5),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dumps", nargs="+")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    pooled, results = [], []
    for path in args.dumps:
        with open(path, errors="ignore") as handle:
            rows, summary = parse(handle.read())
        pooled.extend(rows)
        results.append({"dump": path, **describe(rows), "gfxinfo": summary})
    combined = {"dump": "all", **describe(pooled)} if len(args.dumps) > 1 else None
    if args.json:
        print(json.dumps({"runs": results, "pooled": combined}, indent=2))
        return
    for result in results + ([combined] if combined else []):
        print(f"{result['dump']}: {result['frames']} frames, janky {result['janky_pct']} %, frame p50/p90/p99 "
              f"{result['frame_ms_p50']}/{result['frame_ms_p90']}/{result['frame_ms_p99']} ms, GPU p90 "
              f"{result['gpu_ms_p90']} ms, hitch {result['hitch_ms_per_s']} ms/s, interval {result['interval_ms']} ms")
        if result.get("gfxinfo"):
            print("   gfxinfo: " + "; ".join(f"{key} {value}" for key, value in result["gfxinfo"].items()))


if __name__ == "__main__":
    main()

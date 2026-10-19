"""Frame timing of one scene from `dumpsys gfxinfo <package> framestats`, per window, the same way for District
and the template.

Standard library only. Each argument is one dump taken right after one run of a scene (gfxinfo reset before it).

    python -I frame_summary.py timing/S1_r1.txt timing/S1_r2.txt timing/S1_r3.txt [--json]

A dump holds one PROFILEDATA block per window of the app: its activity, and a dialog window when a sheet is one,
as Material 3's ModalBottomSheet and Views' BottomSheetDialog are (the template's sheets live in the activity's
window). Windows are kept apart: merging them, or a playing video's frames, scrambles the series.

Per window, from its rows with Flags 0 (as the template's tools/perf/parse_framestats.py counts them): frames,
janky % (frames that missed their FrameTimeline deadline, which is what gfxinfo counts), frame time p50/p90/p99
(IntendedVsync to FrameCompleted), GPU p90, hitch ms per second of motion (presented later than one vsync after
the frame before, Apple's measure; under 5 ms/s is good) and the refresh interval.

Per window, from all its rows, flagged ones included:
  - first: the window's first frame in the dump, its flags by name, its UI-thread ms and when it was presented. A
    window that appeared during the dump (a sheet's own dialog window) carries flag 1 on that frame: it is the
    frame that composed the sheet, and the Flags-0 statistics above leave it out;
  - opening: the slowest UI-thread frame among the window's first three (perf.sh's opening frame, without
    dropping a new window's first frame);
  - tap_to_present_ms: for a window that appeared, from the vsync of the last frame of another window that
    handled input before it to the present of its first frame; otherwise from the window's last input frame up
    to its opening frame to the present of the opening frame. Compare these, never the Flags-0 numbers, when one
    app opens a sheet in a new window and the other does not.
Pooled over all dumps, per window, a window being named by its title without the instance hash, plus its rank
among windows of that title in the dump.
"""

import argparse
import json
import re

NS_PER_MS = 1_000_000
SUMMARY_KEYS = ("Total frames rendered", "Janky frames", "50th percentile", "90th percentile", "99th percentile",
                "90th gpu percentile", "Number Frame deadline missed")
MAX_HITCH_INTERVALS = 12
FLAG_NAMES = {1: "window-changed", 2: "render-thread-animation", 4: "surface-canvas", 8: "skipped"}
INSTANCE = re.compile(r"@[0-9a-fA-F]+")


def window_name(line):
    name = INSTANCE.sub("", line.strip())
    return re.sub(r"\s*\(visibility=\d+\)\s*$", "", name)


def parse(text):
    """[(window name, rows)] in dump order, and gfxinfo's own summary lines (process-wide)."""
    windows, summary, header, inside, current, rows = [], {}, None, False, None, None
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("---PROFILEDATA---"):
            if not inside:
                rows = []
                windows.append((current or f"window {len(windows) + 1}", rows))
                current = None
            inside, header = not inside, None
            continue
        if not inside:
            if "ViewRootImpl" in stripped or stripped.startswith("Window:"):
                current = window_name(stripped)
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
        rows.append(row)
    ranked, seen = [], {}
    for name, window_rows in windows:
        seen[name] = seen.get(name, 0) + 1
        window_rows.sort(key=lambda row: row["IntendedVsync"])
        ranked.append((f"{name} #{seen[name]}", window_rows))
    return ranked, summary


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


def ui_ms(row):
    start, end = row.get("AnimationStart", 0), row.get("SyncQueued", 0)
    return round((end - start) / NS_PER_MS, 2) if start > 0 and end >= start else None


def presented(row):
    return row.get("DisplayPresentTime") or row.get("FrameCompleted") or 0


def flag_names(flags):
    names = [name for bit, name in FLAG_NAMES.items() if flags & bit]
    other = flags & ~sum(FLAG_NAMES)
    return names + ([f"0x{other:x}"] if other else [])


def hitch(rows):
    times = sorted(row["DisplayPresentTime"] for row in rows if row.get("DisplayPresentTime", 0) > 0)
    if len(times) < 2:
        return 0.0, 0.0
    period = interval(rows[0])
    late = motion = 0.0
    for earlier, later in zip(times, times[1:]):
        gap = later - earlier
        if gap > MAX_HITCH_INTERVALS * period:
            continue
        motion += gap
        if gap > 1.5 * period:
            late += gap - period
    return late / NS_PER_MS, motion / NS_PER_MS


def regular(rows):
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


def opening(windows, name, rows):
    """The window's first frame, its opening frame and the time from the tap to its first present."""
    if not rows:
        return {}
    origin = min(row["IntendedVsync"] for _, window_rows in windows for row in window_rows)
    first = rows[0]
    timed = [row for row in rows[:3] if ui_ms(row) is not None]
    slowest = max(timed, key=ui_ms) if timed else first
    appeared = bool(first["Flags"] & 1)
    has_input = any("InputEventId" in row for _, window_rows in windows for row in window_rows)
    if appeared:
        inputs = [row for other, window_rows in windows if other != name for row in window_rows
                  if row.get("InputEventId") and row["IntendedVsync"] <= first["IntendedVsync"]]
        shown = first
    else:
        inputs = [row for row in rows if row.get("InputEventId") and row["IntendedVsync"] <= slowest["IntendedVsync"]]
        shown = slowest
    tap = max(inputs, key=lambda row: row["IntendedVsync"]) if inputs else None
    return {
        "first": {
            "flags": flag_names(first["Flags"]),
            "ui_ms": ui_ms(first),
            "at_ms": round((first["IntendedVsync"] - origin) / NS_PER_MS, 2),
            "presented_at_ms": round((presented(first) - origin) / NS_PER_MS, 2) if presented(first) else None,
        },
        "appeared": appeared,
        "opening_ui_ms": ui_ms(slowest),
        "opening_flags": flag_names(slowest["Flags"]),
        "tap_to_present_ms": round((presented(shown) - tap["IntendedVsync"]) / NS_PER_MS, 2)
        if tap and presented(shown) else None,
        "tap_note": None if tap else ("no InputEventId column" if not has_input else "no input frame before it"),
    }


def describe(windows):
    return [{"window": name, **regular([row for row in rows if row["Flags"] == 0]), **opening(windows, name, rows)}
            for name, rows in windows]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dumps", nargs="+")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    runs, pooled = [], {}
    for path in args.dumps:
        with open(path, errors="ignore") as handle:
            windows, summary = parse(handle.read())
        for name, rows in windows:
            pooled.setdefault(name, []).extend(row for row in rows if row["Flags"] == 0)
        runs.append({"dump": path, "windows": describe(windows), "gfxinfo": summary})
    combined = [{"window": name, **regular(rows)} for name, rows in pooled.items()] if len(args.dumps) > 1 else None
    if args.json:
        print(json.dumps({"runs": runs, "pooled": combined}, indent=2))
        return
    for run in runs:
        print(f"{run['dump']}:")
        for window in run["windows"]:
            line = (f"  {window['window']}: {window['frames']} regular frames, janky {window['janky_pct']} %, frame "
                    f"p50/p90/p99 {window['frame_ms_p50']}/{window['frame_ms_p90']}/{window['frame_ms_p99']} ms, "
                    f"GPU p90 {window['gpu_ms_p90']} ms, hitch {window['hitch_ms_per_s']} ms/s, "
                    f"interval {window['interval_ms']} ms")
            print(line)
            if window.get("first"):
                first = window["first"]
                print(f"    first frame at {first['at_ms']} ms, flags {first['flags'] or 'none'}, UI "
                      f"{first['ui_ms']} ms, presented at {first['presented_at_ms']} ms; appeared in this dump: "
                      f"{window['appeared']}; opening UI {window['opening_ui_ms']} ms (flags "
                      f"{window['opening_flags'] or 'none'}); tap to present {window['tap_to_present_ms']} ms"
                      + (f" ({window['tap_note']})" if window["tap_note"] else ""))
        if run["gfxinfo"]:
            print("  gfxinfo (whole process): " + "; ".join(f"{key} {value}" for key, value in run["gfxinfo"].items()))
    for window in combined or []:
        print(f"pooled {window['window']}: {window['frames']} regular frames, janky {window['janky_pct']} %, frame "
              f"p50/p90/p99 {window['frame_ms_p50']}/{window['frame_ms_p90']}/{window['frame_ms_p99']} ms, GPU p90 "
              f"{window['gpu_ms_p90']} ms, hitch {window['hitch_ms_per_s']} ms/s")


if __name__ == "__main__":
    main()

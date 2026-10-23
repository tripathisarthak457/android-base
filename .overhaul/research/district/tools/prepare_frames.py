"""District P3's phone check (lists-images.md 5.4): what a held press on a list sheet's trigger costs, and what the
opening after it costs, from `dumpsys gfxinfo <package> framestats` dumps (gfxinfo reset before each press).

Standard library only, on frame_summary.py's parser. Each argument is one dump of one open; `media-phone.win.sh
sheets` writes them as <sheet>-<arm>-<run>.txt.

    python -I prepare_frames.py perf/media/sheets/<session>/filters-B-*.txt [--json]

Per dump, in the activity's window (the template's sheets live in it):
  - press: the frames from the first frame that handled input (the down) to the last one (the up): their slowest
    UI-thread time, and how many missed their deadline;
  - opening: the slowest UI-thread frame among the three after the up;
  - tap_to_present_ms: the up frame's vsync to the present of that opening frame (the study's definition);
  - janky frames from the down to the end of the dump, split as the study split them: UI-thread time over 5 ms is
    the app's own work, under it is a start-up after idle.
Then, over the dumps given, the counts lists-images 5.4's pass rule reads: openings at most 8.33 ms, the median
opening, presses whose frames all stayed at most 7 ms with no app-work miss, and the median tap to present.
"""

import argparse
import json
import os
import statistics
import sys

# Run with -I, which leaves this folder off the path: frame_summary.py sits beside this file.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frame_summary as fs  # noqa: E402

BUDGET_MS = 8.33
PRESS_LIMIT_MS = 7.0
APP_WORK_MS = 5.0


def activity_rows(windows):
    """The rows of the window with the most frames: the activity's, where the template draws its sheets."""
    return max((rows for _, rows in windows), key=len, default=[])


def press_and_open(rows):
    inputs = [row for row in rows if row.get("InputEventId")]
    if not inputs:
        return {"note": "no frame handled input: was the dump reset before the press?"}
    down, up = inputs[0], inputs[-1]
    press = [row for row in rows if down["IntendedVsync"] <= row["IntendedVsync"] <= up["IntendedVsync"]]
    after = [row for row in rows if row["IntendedVsync"] > up["IntendedVsync"]][:3]
    timed = [row for row in after if fs.ui_ms(row) is not None]
    opening = max(timed, key=fs.ui_ms) if timed else None
    tail = [row for row in rows if row["IntendedVsync"] >= down["IntendedVsync"]]
    press_ui = [fs.ui_ms(row) for row in press if fs.ui_ms(row) is not None]
    return {
        "press_frames": len(press),
        "press_ui_max_ms": max(press_ui) if press_ui else None,
        "press_missed": sum(fs.missed(row) for row in press),
        "opening_ui_ms": fs.ui_ms(opening) if opening else None,
        "tap_to_present_ms": round((fs.presented(opening) - up["IntendedVsync"]) / fs.NS_PER_MS, 2)
        if opening and fs.presented(opening) else None,
        "missed_app_work": sum(fs.missed(row) and (fs.ui_ms(row) or 0) > APP_WORK_MS for row in tail),
        "missed_after_idle": sum(fs.missed(row) and (fs.ui_ms(row) or 0) <= APP_WORK_MS for row in tail),
    }


def summary(results):
    openings = [r["opening_ui_ms"] for r in results if r.get("opening_ui_ms") is not None]
    taps = [r["tap_to_present_ms"] for r in results if r.get("tap_to_present_ms") is not None]
    clean_presses = sum(1 for r in results if r.get("press_ui_max_ms") is not None
                        and r["press_ui_max_ms"] <= PRESS_LIMIT_MS and r["missed_app_work"] == 0)
    return {
        "opens": len(results),
        "openings_within_budget": sum(1 for value in openings if value <= BUDGET_MS),
        "opening_median_ms": round(statistics.median(openings), 2) if openings else None,
        "clean_presses": clean_presses,
        "tap_to_present_median_ms": round(statistics.median(taps), 2) if taps else None,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dumps", nargs="+")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    results = []
    for path in args.dumps:
        with open(path, errors="ignore") as handle:
            windows, _ = fs.parse(handle.read())
        results.append({"dump": path, **press_and_open(activity_rows(windows))})
    report = {"opens": results, "summary": summary(results)}
    if args.json:
        print(json.dumps(report, indent=2))
        return
    for result in results:
        print(f"{result['dump']}: " + ", ".join(f"{key} {value}" for key, value in result.items() if key != "dump"))
    print("summary: " + ", ".join(f"{key} {value}" for key, value in report["summary"].items()))


if __name__ == "__main__":
    main()

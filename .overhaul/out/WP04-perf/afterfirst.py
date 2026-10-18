#!/usr/bin/env python3
"""Janky frames other than each transition's first, from the segment dumps a perf.sh run kept, for runs made before
perf.py reported it (20d134a): the same function perf.py now records, applied to the same gfxinfo dumps.

  afterfirst.py <perf.sh out dir>...    (medians of the runs, per scenario)
"""
import os
import re
import statistics
import sys

PERF = "D:/projects/tools/.overhaul/wt/WP04-perf/templates/android/tools/perf"
sys.path.insert(0, PERF)
import parse_framestats  # noqa: E402
import parse_gfx  # noqa: E402
from perf import janky_after_first_pct  # noqa: E402

SEGMENTS = {
    "tabs": [r"tabs_\d+\.txt"],
    "tabs_tap": [r"tabs_tap_\d+\.txt"],
    "pushpop": [r"push_\d+\.txt", r"pop_\d+\.txt"],
    "pushpop_gesture": [r"gesture_push_\d+\.txt", r"gesture_pop_\d+\.txt"],
    "rapid_back": [r"rapid_back_\d+\.txt"],
}


def run_values(run_dir):
    names = sorted(os.listdir(run_dir))
    values = {}
    for scenario, patterns in SEGMENTS.items():
        files = [name for name in names if any(re.fullmatch(pattern, name) for pattern in patterns)]
        if not files:
            continue
        dumps, frames = [], []
        for name in sorted(files, key=lambda n: int(re.search(r"(\d+)\.txt$", n).group(1))):
            raw = open(os.path.join(run_dir, name), encoding="utf-8", errors="replace").read()
            dumps.append(parse_gfx.parse(raw))
            frames.append(parse_framestats.load(raw))
        values[scenario] = janky_after_first_pct(dumps, frames)
    return values


def main():
    for out in sys.argv[1:]:
        runs = [os.path.join(out, d) for d in sorted(os.listdir(out)) if re.fullmatch(r"run\d+", d)]
        per_run = [run_values(run) for run in runs]
        print(out)
        for scenario in SEGMENTS:
            vals = [values[scenario] for values in per_run if scenario in values]
            if vals:
                print(f"  {scenario}.janky_after_first_pct median {statistics.median(vals):.2f}  runs {vals}")


if __name__ == "__main__":
    main()

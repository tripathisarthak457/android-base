#!/usr/bin/env python3
"""The handoff's gap table: one column per perf.sh output directory (medians of its runs, as summary.json reports
them), with janky frames other than each transition's first taken from summary.json where perf.py reported it and
computed by afterfirst.py from the kept gfxinfo dumps where it did not.

  gap-table.py "Label=perf dir"... [--limits]
"""
import json
import os
import statistics
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from afterfirst import SEGMENTS, run_values  # noqa: E402

ROWS = [
    ("Tab switch, janky (%) / after the first (%)", ["tabs.janky_pct", "tabs.janky_after_first_pct"], "1"),
    ("Tab switch, late first frames / switches", ["tabs.late_first_frames", "tabs.transitions"], ""),
    ("Tab switch, hitch (ms/s)", ["tabs.hitch_ms_per_s"], "ratchet"),
    ("Tab switch, first frame UI median / max (ms)", ["tabs.first_frame_ms.median", "tabs.first_frame_ms.max"], "8 / 16"),
    ("Tab switch, p99 / GPU p90 (ms)", ["tabs.p99_ms", "tabs.gpu_p90_ms"], ""),
    ("Plain taps, janky (%) / after the first (%)", ["tabs_tap.janky_pct", "tabs_tap.janky_after_first_pct"], "1"),
    ("First visit to a tab, first frame median / max (ms)", ["tab1.first_frame_ms.median", "tab1.first_frame_ms.max"], "ratchet"),
    ("Push, composing frame median / max (ms)", ["pushpop.push_first_frame_ms.median", "pushpop.push_first_frame_ms.max"], "ratchet"),
    ("Pop, first frame median / max (ms)", ["pushpop.pop_first_frame_ms.median", "pushpop.pop_first_frame_ms.max"], "8 / 16"),
    ("Push/pop (Back key), janky (%) / after the first (%) / p99 (ms)",
     ["pushpop.janky_pct", "pushpop.janky_after_first_pct", "pushpop.p99_ms"], "2 / – / 25"),
    ("Push/pop, hitch (ms/s) / GPU p90 (ms)", ["pushpop.hitch_ms_per_s", "pushpop.gpu_p90_ms"], ""),
    ("Back gesture, janky (%) / after the first (%) / p99 (ms)",
     ["pushpop_gesture.janky_pct", "pushpop_gesture.janky_after_first_pct", "pushpop_gesture.p99_ms"], "2 / – / –"),
    ("Back gesture, hitch (ms/s) / GPU p90 (ms)", ["pushpop_gesture.hitch_ms_per_s", "pushpop_gesture.gpu_p90_ms"], ""),
    ("Rapid Back, janky (%) / after the first (%) / p99 (ms)",
     ["rapid_back.janky_pct", "rapid_back.janky_after_first_pct", "rapid_back.p99_ms"], "4"),
    ("Rapid Back, coalesced pop slowest frame median / max (ms)", ["rapid_back.pop_frame_ms.median", "rapid_back.pop_frame_ms.max"], "8 / 16"),
    ("Scroll root, janky (%) / GPU p90 (ms)", ["scroll_root.janky_pct", "scroll_root.gpu_p90_ms"], "0.5 / 7"),
    ("Scroll with minimize, janky (%) / GPU p90 (ms)", ["scroll_minimize.janky_pct", "scroll_minimize.gpu_p90_ms"], "0.5 / 7"),
    ("Recompositions per switch / push / pop", ["recompose.tab_switch.max", "recompose.push.max", "recompose.pop.max"], "2 / – / 2"),
    ("Cold start, speed-profile / as installed (ms)", ["coldstart.median_ms", "coldstart_installed.median_ms"], "380 / 400"),
    ("PSS app / total (MB)", ["meminfo.pss_app_mb", "meminfo.pss_mb"], "48 / –"),
]


def load(out):
    path = os.path.join(out, "summary.json")
    if not os.path.exists(path):
        return {}
    metrics = dict(json.load(open(path, encoding="utf-8"))["metrics"])
    runs = [os.path.join(out, d) for d in sorted(os.listdir(out)) if d.startswith("run")]
    computed = [run_values(run) for run in runs]
    for scenario in SEGMENTS:
        key = f"{scenario}.janky_after_first_pct"
        values = [c[scenario] for c in computed if scenario in c]
        if key not in metrics and values:
            metrics[key] = round(statistics.median(values), 2)
    return metrics


def show(value):
    if isinstance(value, dict):
        value = value.get("value")
    if value is None:
        return "-"
    if isinstance(value, float):
        return f"{value:.2f}".rstrip("0").rstrip(".") if value != int(value) else str(int(value))
    return str(value)


def main():
    columns = [arg.split("=", 1) for arg in sys.argv[1:] if "=" in arg]
    data = [(label, load(path)) for label, path in columns]
    print("| Row | " + " | ".join(label for label, _ in data) + " | `solid` limit |")
    print("|---" * (len(data) + 2) + "|")
    for name, keys, limit in ROWS:
        cells = [" / ".join(show(metrics.get(k)) for k in keys) for _, metrics in data]
        print(f"| {name} | " + " | ".join(cells) + f" | {limit} |")


if __name__ == "__main__":
    main()

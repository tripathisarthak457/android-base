"""Prints the handoff's before/after table: one column per perf.sh output directory, one row per measured metric
(medians of the per-run values, as summary.json reports them; "-" where a run did not measure it).

    python3 final-table.py Old=perf/WP04/baseline "Phase 2=perf/WP04/phase2" Final=perf/WP04/final
"""
import json
import os
import sys

ROWS = [
    ("Tab switch, janky frames (%)", ["tabs.janky_pct"]),
    ("Tab switch, janky frames other than each switch's first (%)", ["tabs.janky_after_first_pct"]),
    ("Tab switch, late first frames / switches", ["tabs.late_first_frames", "tabs.transitions"]),
    ("Tab switch, p99 / GPU p90 (ms)", ["tabs.p99_ms", "tabs.gpu_p90_ms"]),
    ("Tab switch to a visited tab, first frame median / max (ms)", ["tabs.first_frame_ms.median", "tabs.first_frame_ms.max"]),
    ("Tab switch with plain taps, janky (%) / after the first (%)", ["tabs_tap.janky_pct", "tabs_tap.janky_after_first_pct"]),
    ("First visit to a tab, first frame median / max (ms)", ["tab1.first_frame_ms.median", "tab1.first_frame_ms.max"]),
    ("Push, first frame median / max (ms)", ["pushpop.push_first_frame_ms.median", "pushpop.push_first_frame_ms.max"]),
    ("Pop, first frame median / max (ms)", ["pushpop.pop_first_frame_ms.median", "pushpop.pop_first_frame_ms.max"]),
    ("Push/pop with the Back key, janky (%) / p99 (ms)", ["pushpop.janky_pct", "pushpop.p99_ms"]),
    ("Push/pop with the Back key, GPU p90 (ms)", ["pushpop.gpu_p90_ms"]),
    ("Push/pop with the back gesture, janky (%) / p99 (ms)", ["pushpop_gesture.janky_pct", "pushpop_gesture.p99_ms"]),
    ("Back swipe during a push: opaque / lands on the list", ["back_during_push.opaque", "back_during_push.lands_on_list"]),
    ("Rapid Back: final screen / alive", ["rapid_back.final_screen", "rapid_back.alive"]),
    ("Rapid Back, janky (%) / p99 (ms)", ["rapid_back.janky_pct", "rapid_back.p99_ms"]),
    ("Rapid Back, coalesced pop slowest frame median / max (ms)", ["rapid_back.pop_frame_ms.median", "rapid_back.pop_frame_ms.max"]),
    ("Scroll a root list, janky (%) / GPU p90 (ms)", ["scroll_root.janky_pct", "scroll_root.gpu_p90_ms"]),
    ("Scroll with the bar minimizing, janky (%) / GPU p90 (ms)", ["scroll_minimize.janky_pct", "scroll_minimize.gpu_p90_ms"]),
    ("Navigation stress, janky (%) / invariants", ["nav_stress.janky_pct", "nav_stress.invariants"]),
    ("Idle 5 s on every tab, frames rendered", ["idle.frames"]),
    ("Recompositions per tab switch / push / pop (max)", ["recompose.tab_switch.max", "recompose.push.max", "recompose.pop.max"]),
    ("Restore after process death: restored", ["restore.restored"]),
    ("Cold start, speed-profile / as installed, median (ms)", ["coldstart.median_ms", "coldstart_installed.median_ms"]),
    ("PSS after the journey, total / app (MB)", ["meminfo.pss_mb", "meminfo.pss_app_mb"]),
]


def load(path):
    summary = os.path.join(path, "summary.json")
    if not os.path.exists(summary):
        return {}
    with open(summary, encoding="utf-8") as handle:
        return json.load(handle)["metrics"]


def cell(metrics, keys):
    values = []
    for key in keys:
        entry = metrics.get(key)
        value = entry.get("value") if isinstance(entry, dict) else entry
        if value is None:
            values.append("-")
        elif isinstance(value, bool):
            values.append("yes" if value else "**no**")
        elif isinstance(value, float):
            values.append(f"{value:g}")
        else:
            values.append(str(value))
    return " / ".join(values)


def main(columns):
    named = [column.split("=", 1) for column in columns]
    loaded = [(name, load(path)) for name, path in named]
    print("| Row | " + " | ".join(name for name, _ in loaded) + " |")
    print("|---|" + "---|" * len(loaded))
    for label, keys in ROWS:
        cells = [cell(metrics, keys) for _, metrics in loaded]
        if all(set(text) <= set("-/ ") for text in cells):
            continue
        print(f"| {label} | " + " | ".join(cells) + " |")


if __name__ == "__main__":
    main(sys.argv[1:])

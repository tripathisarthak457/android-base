#!/usr/bin/env python3
"""The gap rows side by side from perf.sh summary.json files:  compare.py <label=dir>..."""
import json, os, sys

ROWS = [
    ("tabs.janky_pct", "Tab switch janky %"), ("tabs.late_first_frames", "  late first frames"), ("tabs.transitions", "  of transitions"),
    ("tabs.first_frame_ms.median", "Tab switch first frame UI median ms"), ("tabs.first_frame_ms.max", "  max"),
    ("tabs.first3_gpu_ms.median", "  first-3 GPU median ms"), ("tabs.hitch_ms_per_s", "  hitch ms/s"), ("tabs.p99_ms", "  p99 ms"),
    ("tabs_tap.janky_pct", "Tab switch (plain tap) janky %"), ("tabs_tap.late_first_frames", "  late first frames"),
    ("tab1.first_frame_ms.median", "tab1 first frame median"), ("tab1.first_frame_ms.max", "  max"),
    ("pushpop.janky_pct", "Push/pop janky %"), ("pushpop.p99_ms", "  p99 ms"), ("pushpop.p90_ms", "  p90 ms"),
    ("pushpop.late_first_frames", "  late first frames"), ("pushpop.transitions", "  of transitions"),
    ("pushpop.pop_first_frame_ms.median", "Pop first frame median ms"), ("pushpop.pop_first_frame_ms.max", "  max"),
    ("pushpop.push_first_frame_ms.median", "Push composing frame median ms"), ("pushpop.push_first_frame_ms.max", "  max"),
    ("pushpop.hitch_ms_per_s", "  push/pop hitch ms/s"), ("pushpop.gpu_p90_ms", "  GPU p90"),
    ("pushpop_gesture.janky_pct", "Back gesture janky %"), ("pushpop_gesture.late_first_frames", "  late first frames"), ("pushpop_gesture.p99_ms", "  p99"),
    ("rapid_back.janky_pct", "Rapid back janky %"), ("rapid_back.pop_frame_ms.median", "  coalesced pop frame median"), ("rapid_back.pop_frame_ms.max", "  max"),
    ("scroll_root.janky_pct", "Scroll root janky %"), ("scroll_root.gpu_p90_ms", "  GPU p90"), ("scroll_root.p99_ms", "  p99"),
    ("scroll_minimize.janky_pct", "Scroll minimize janky %"), ("scroll_minimize.gpu_p90_ms", "  GPU p90"),
    ("coldstart.median_ms", "Cold start speed-profile median ms"), ("coldstart_installed.median_ms", "Cold start installed median ms"),
    ("meminfo.pss_app_mb", "PSS app MB"), ("meminfo.pss_mb", "PSS total MB"), ("meminfo.graphics_mb", "  graphics"),
    ("recompose.tab_switch.max", "Recompose frames: tab switch max"), ("recompose.pop.max", "  pop max"), ("recompose.push.max", "  push max"),
    ("recompose.tab_switch.runs", "  tab switch runs"), ("recompose.pop.runs", "  pop runs"), ("recompose.push.runs", "  push runs"),
]

def load(d):
    p = os.path.join(d, "summary.json")
    if not os.path.exists(p):
        return {}
    m = json.load(open(p))
    metrics = m.get("metrics", m)
    out = {}
    for k, v in metrics.items():
        out[k] = v.get("value") if isinstance(v, dict) and "value" in v else v
    return out

def main():
    cols = [a.split("=", 1) for a in sys.argv[1:]]
    data = [(label, load(d)) for label, d in cols]
    print(f"{'row':44}" + "".join(f"{label:>18}" for label, _ in data))
    for key, label in ROWS:
        vals = [str(m.get(key, "-"))[:17] for _, m in data]
        if any(v != "-" for v in vals):
            print(f"{label:44}" + "".join(f"{v:>18}" for v in vals))

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""First-frame rows of one perf.sh run directory, straight from its framestats dumps:
   peek.py <run-dir> [prefix...]   e.g. peek.py $P/everything/run1 tabs_ pop_ push_"""
import glob, os, sys, statistics
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP04-perf/templates/android/tools/perf")
import parse_framestats as pf, parse_gfx

run = sys.argv[1]
prefixes = sys.argv[2:] or ["tabs_", "tabs_tap_", "tab1_", "push_", "pop_", "gesture_push_", "gesture_pop_", "rapid_back_"]
for prefix in prefixes:
    files = sorted(f for f in glob.glob(os.path.join(run, prefix + "[0-9]*.txt")))
    if not files:
        continue
    firsts, lates, totals, uis, rts, gpus, janky, frames = [], 0, [], [], [], [], 0, 0
    for f in files:
        text = open(f).read()
        rows = pf.load(text)
        g = parse_gfx.parse(text)
        janky += g.get("janky", 0); frames += g.get("frames", 0)
        if not rows:
            continue
        first = rows[0]
        firsts.append(pf.ui_ms(first))
        if pf.missed_deadline(first): lates += 1
        ph = pf.phases(first)
        totals.append(ph["total"]); uis.append(ph["animation_and_composition"] + ph["layout_and_draw"]); rts.append(ph["render_thread"])
        st = pf.stages(rows, 1)[0]
        if st["gpu"] is not None: gpus.append(st["gpu"])
    def med(v): return round(statistics.median(v), 2) if v else None
    print(f"{prefix:14} n={len(files):2} first ui med/max {med(firsts)}/{round(max(firsts),2) if firsts else None}  "
          f"rt med {med(rts)} gpu med {med(gpus)} total med/max {med(totals)}/{round(max(totals),2) if totals else None}  "
          f"late {lates}/{len(firsts)}  janky {janky}/{frames} = {round(100*janky/frames,2) if frames else 0}%")

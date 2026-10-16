#!/usr/bin/env python3
"""Every frame of one framestats dump: frames.py <dump.txt> [--all]  (default: the first 12 and every late one)"""
import sys
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP04-perf/templates/android/tools/perf")
import parse_framestats as pf
text = open(sys.argv[1]).read()
rows = pf.load(text)
show_all = "--all" in sys.argv
t0 = rows[0]["IntendedVsync"] if rows else 0
print("i    t_ms   gap   ui   anim  lay   rt_issue  gpu   deq   total  late")
for i, r in enumerate(rows):
    ph = pf.phases(r); st = pf.stages([r], 1)[0]
    late = pf.missed_deadline(r)
    gap = (r["IntendedVsync"] - rows[i-1]["IntendedVsync"]) / 1e6 if i else 0
    if show_all or i < 12 or late:
        print(f"{i:3} {(r['IntendedVsync']-t0)/1e6:7.1f} {gap:5.1f} {pf.ui_ms(r):5.2f} {ph['animation_and_composition']:5.2f} {ph['layout_and_draw']:5.2f}  {st['issue'] or 0:6.2f} {st['gpu'] or 0:6.2f} {st['dequeue']:5.2f} {ph['total']:6.2f}  {'LATE' if late else ''}")

"""latestage.py <run dir> <regex> [all]: stages of late frames (or all frames): sync, issue (RT), gpu, from vsync."""
import os, re, sys
sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04-perf/templates/android/tools/perf")
import parse_framestats as pf
d, pattern = sys.argv[1], sys.argv[2]
show_all = len(sys.argv) > 3
ms = lambda a, b: (b - a) / 1e6
for name in sorted(os.listdir(d)):
    if not re.fullmatch(pattern, name):
        continue
    rows = pf.load(open(os.path.join(d, name), encoding="utf-8", errors="replace").read())
    out = []
    for i, r in enumerate(rows):
        if show_all or pf.missed_deadline(r):
            v = r["IntendedVsync"]
            out.append(f"#{i} start+{ms(v, r['HandleInputStart']):.1f} ui {pf.ui_ms(r):.1f} rt {ms(r['SyncStart'], r['SwapBuffers']):.1f} "
                       f"gpu {ms(r['SwapBuffers'], r.get('GpuCompleted', r['SwapBuffers'])):.1f} done {ms(v, max(r['FrameCompleted'], r.get('GpuCompleted', 0))):.1f} "
                       f"dl {ms(v, r['FrameDeadline']):.1f}")
    print(name); [print("   " + o) for o in out]

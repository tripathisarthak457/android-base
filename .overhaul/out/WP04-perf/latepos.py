"""latepos.py <run dir> <regex>: per segment, the frames that missed their deadline: index, UI ms, total ms."""
import os, re, sys
sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04-perf/templates/android/tools/perf")
import parse_framestats as pf
d, pattern = sys.argv[1], sys.argv[2]
for name in sorted(os.listdir(d)):
    if not re.fullmatch(pattern, name):
        continue
    rows = pf.load(open(os.path.join(d, name), encoding="utf-8", errors="replace").read())
    late = []
    for i, r in enumerate(rows):
        if pf.missed_deadline(r):
            total = (max(r["FrameCompleted"], r.get("GpuCompleted", 0)) - r["IntendedVsync"]) / 1e6
            over = (max(r["FrameCompleted"], r.get("GpuCompleted", 0)) - r["FrameDeadline"]) / 1e6
            late.append(f"#{i} ui {pf.ui_ms(r):.1f} total {total:.1f} over {over:.1f}")
    print(f"{name}: {len(rows)} frames; late: {'; '.join(late)}")

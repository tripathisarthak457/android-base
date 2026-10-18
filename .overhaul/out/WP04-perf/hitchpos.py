"""hitchpos.py <run dir>... <regex>: where in each transition the display hitched (a present more than 1.5 vsyncs after
the one before), with that frame's stages: the hitch ms it cost, its index, UI, RenderThread, GPU, from vsync to done."""
import os, re, sys, collections
sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04-perf/templates/android/tools/perf")
import parse_framestats as pf
*dirs, pattern = sys.argv[1:]
ms = lambda a, b: (b - a) / 1e6
total = collections.Counter(); count = collections.Counter(); frames = 0
for d in dirs:
    for name in sorted(os.listdir(d)):
        if not re.fullmatch(pattern, name):
            continue
        rows = pf.load(open(os.path.join(d, name), encoding="utf-8", errors="replace").read())
        rows = [r for r in rows if r.get("DisplayPresentTime", 0) > 0]
        if len(rows) < 2:
            continue
        interval = pf._interval(rows[0]); frames += len(rows)
        out = []
        for i in range(1, len(rows)):
            gap = rows[i]["DisplayPresentTime"] - rows[i - 1]["DisplayPresentTime"]
            if 1.5 * interval < gap <= 12 * interval:
                r = rows[i]; v = r["IntendedVsync"]
                cost = (gap - interval) / 1e6
                bucket = "first3" if i <= 3 else ("last15" if i >= len(rows) - 15 else "middle")
                total[bucket] += cost; count[bucket] += 1
                out.append(f"#{i}/{len(rows)} +{cost:.1f}ms ui {pf.ui_ms(r):.1f} rt {ms(r['SyncStart'], r['SwapBuffers']):.1f} "
                           f"gpu {ms(r['SwapBuffers'], r.get('GpuCompleted', r['SwapBuffers'])):.1f} "
                           f"done {ms(v, max(r['FrameCompleted'], r.get('GpuCompleted', 0))):.1f}")
        print(os.path.basename(d) + "/" + name, " | ".join(out))
print("hitch ms by position:", {k: round(v, 1) for k, v in total.items()}, "count:", dict(count), "frames:", frames)

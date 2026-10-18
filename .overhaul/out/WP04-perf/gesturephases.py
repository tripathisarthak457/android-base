"""gesturephases.py <perf dir>...: the back gesture's frames (gesture_pop dumps, all runs pooled) split by where
they fall: the first frame (from idle), the gesture (the drag and the commit's fade) and the last 14 frames (the list
alone once the pop is done). Medians and p90 of RenderThread and GPU time, and late frames, per part."""
import os, re, statistics, sys
sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04-perf/templates/android/tools/perf")
import parse_framestats as pf

TAIL = 14
ms = lambda a, b: (b - a) / 1e6
pct = lambda v, q: sorted(v)[min(len(v) - 1, int(q * len(v)))]
for out in sys.argv[1:]:
    parts = {"first": [], "gesture": [], "tail": []}
    for run in sorted(d for d in os.listdir(out) if re.fullmatch(r"run\d+", d)):
        for name in sorted(os.listdir(os.path.join(out, run))):
            if not re.fullmatch(r"gesture_pop_\d+\.txt", name):
                continue
            rows = pf.load(open(os.path.join(out, run, name), encoding="utf-8", errors="replace").read())
            for i, r in enumerate(rows):
                part = "first" if i == 0 else ("tail" if i >= len(rows) - TAIL else "gesture")
                parts[part].append((ms(r["SyncStart"], r["SwapBuffers"]),
                                    ms(r["SwapBuffers"], r.get("GpuCompleted", r["SwapBuffers"])), pf.missed_deadline(r)))
    line = []
    for part, v in parts.items():
        rt = [a for a, _, _ in v]; gpu = [b for _, b, _ in v]; late = sum(c for _, _, c in v)
        line.append(f"{part} n{len(v)} rt {statistics.median(rt):.2f}/{pct(rt, .9):.2f} "
                    f"gpu {statistics.median(gpu):.2f}/{pct(gpu, .9):.2f} late {late}")
    print(f"{os.path.basename(out):<12} " + " | ".join(line))

"""gesturestats.py <perf dir>...: per-frame stages of the back gesture's frames (gesture_pop dumps), all runs pooled.

Reports the median and p90 of GPU time (SwapBuffers to GpuCompleted), RenderThread time (SyncStart to SwapBuffers)
and UI time, over frames 2.. of each gesture (the first frame starts from idle and is reported apart).
"""
import os, re, statistics, sys
sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04-perf/templates/android/tools/perf")
import parse_framestats as pf

def ms(a, b):
    return (b - a) / 1e6

def pct(values, q):
    values = sorted(values)
    return values[min(len(values) - 1, int(q * len(values)))]

for out in sys.argv[1:]:
    gpu, rt, ui, late, total = [], [], [], 0, 0
    for run in sorted(d for d in os.listdir(out) if re.fullmatch(r"run\d+", d)):
        for name in sorted(os.listdir(os.path.join(out, run))):
            if not re.fullmatch(r"gesture_pop_\d+\.txt", name):
                continue
            rows = pf.load(open(os.path.join(out, run, name), encoding="utf-8", errors="replace").read())
            for r in rows[1:]:
                total += 1
                late += pf.missed_deadline(r)
                gpu.append(ms(r["SwapBuffers"], r.get("GpuCompleted", r["SwapBuffers"])))
                rt.append(ms(r["SyncStart"], r["SwapBuffers"]))
                ui.append(pf.ui_ms(r))
    print(f"{out}: frames {total}, late {late} ({100*late/max(total,1):.2f} %)")
    for label, v in (("gpu", gpu), ("rt", rt), ("ui", ui)):
        print(f"  {label}: median {statistics.median(v):.2f}  p90 {pct(v, .9):.2f}  p99 {pct(v, .99):.2f}  mean {statistics.mean(v):.2f}")

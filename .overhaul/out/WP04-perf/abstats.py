"""abstats.py <regex> <perf dir>...: for each directory, the frames of the dumps matching <regex> in its run*/
folders, frame 1 onward of each (frame 0 starts from idle): late frames (HWUI's deadline), RenderThread frames of
6 ms or more (a little core's frame, at this phone's clocks), GPU frames over one 120 Hz vsync, and the display's
hitches (a present more than 1.5 vsyncs after the one before: count, and ms per second of motion)."""
import os, re, statistics, sys
sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04-perf/templates/android/tools/perf")
import parse_framestats as pf

ms = lambda a, b: (b - a) / 1e6
pattern, dirs = sys.argv[1], sys.argv[2:]
for out in dirs:
    frames = late = rt_spikes = gpu_over = hitches = 0
    dumps, rts, gpus, uis = [], [], [], []
    for run in sorted(d for d in os.listdir(out) if re.fullmatch(r"run\d+", d)):
        for name in sorted(os.listdir(os.path.join(out, run))):
            if not re.fullmatch(pattern, name):
                continue
            rows = pf.load(open(os.path.join(out, run, name), encoding="utf-8", errors="replace").read())
            dumps.append(rows)
            interval = pf._interval(rows[0]) if rows else 0
            presented = [r["DisplayPresentTime"] for r in rows if r.get("DisplayPresentTime", 0) > 0]
            hitches += sum(1 for a, b in zip(presented, presented[1:]) if 1.5 * interval < b - a <= 12 * interval)
            for r in rows[1:]:
                frames += 1
                late += pf.missed_deadline(r)
                rt = ms(r["SyncStart"], r["SwapBuffers"]); gpu = ms(r["SwapBuffers"], r.get("GpuCompleted", r["SwapBuffers"]))
                rts.append(rt); gpus.append(gpu); uis.append(pf.ui_ms(r))
                rt_spikes += rt >= 6
                gpu_over += gpu > 8.33
    if not frames:
        print(f"{out}: no dumps"); continue
    p90 = lambda v: sorted(v)[int(0.9 * len(v))]
    print(f"{os.path.basename(out):24s} dumps {len(dumps):3d} frames {frames:5d} late {late:3d} ({100 * late / frames:.2f} %) "
          f"rt>=6ms {rt_spikes:3d} ({100 * rt_spikes / frames:.2f} %) gpu>8.3 {gpu_over:3d} hitches {hitches:3d} "
          f"hitch {pf.hitch_ratio(dumps)} ms/s | ui med {statistics.median(uis):.2f} rt med/p90 {statistics.median(rts):.2f}/{p90(rts):.2f} "
          f"gpu med/p90 {statistics.median(gpus):.2f}/{p90(gpus):.2f}")

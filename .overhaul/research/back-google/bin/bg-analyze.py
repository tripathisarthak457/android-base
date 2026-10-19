"""bg-analyze.py [builds...]: the Perfetto half of DESIGN.md §5.5 over back-trace.win.sh's captures.

For each perf/WP04-perf/bg-<build>-t1 and -t2 capture it runs research/back-google/sql/{backlat,layerts,
releaseframes,presentlat,startframes}.sql once (output cached next to the trace as <query>.csv), puts every row in
the bgsteps.sh step it falls in (steps.txt: device uptime, the trace's CLOCK_BOOTTIME), and prints one table per
question with both captures of a build pooled: fast swipes, slow starts and cancels apart, edge touches and edge
scrolls apart.
  python -I bg-analyze.py [bg-head bg-tex bg-c1 bg-c2]"""
import csv
import io
import os
import re
import statistics
import subprocess
import sys

OV = "D:/projects/tools/.overhaul"
P = f"{OV}/perf/WP04-perf"
SQL = f"{OV}/research/back-google/sql"
TP = ("C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills/profilers/android-profiler/references/"
      "perfetto/bin/trace_processor")
QUERIES = ("backlat", "layerts", "releaseframes", "presentlat", "startframes")
TAGS = tuple(os.environ.get("BG_TAGS", "t1 t2").split())


def query(capture, name):
    cached = os.path.join(capture, f"{name}.csv")
    if not os.path.exists(cached):
        env = dict(os.environ, USERPROFILE="D:\\.caches\\perfetto")
        out = subprocess.run([sys.executable, TP, "-q", f"{SQL}/{name}.sql", os.path.join(capture, "trace.perfetto-trace")],
                             capture_output=True, text=True, env=env, check=True).stdout
        start = out.find('"')
        with open(cached, "w", encoding="utf-8", newline="") as f:
            f.write(out[start:] if start >= 0 else "")
    with open(cached, encoding="utf-8") as f:
        return [{k: (None if v == "[NULL]" else v) for k, v in row.items()} for row in csv.DictReader(f)]


def steps(capture):
    marks = []
    for line in open(os.path.join(capture, "steps.txt"), encoding="utf-8"):
        name, at = line.split()
        marks.append((name, float(at)))
    begins = [(n, t) for n, t in marks if not n.endswith("_end")]
    return [(n, t, begins[i + 1][1] if i + 1 < len(begins) else t + 5) for i, (n, t) in enumerate(begins)]


def kind(step):
    return re.sub(r"\d+$", "", step)


def step_of(windows, uptime):
    for name, start, end in windows:
        if start <= uptime < end:
            return name
    return None


def num(v):
    return None if v is None or v == "" else float(v)


def med(values):
    values = [v for v in values if v is not None]
    return round(statistics.median(values), 1) if values else None


def fmt(v):
    return "–" if v is None else (f"{v:g}" if isinstance(v, float) else str(v))


def main(builds):
    pooled = {}
    for build in builds:
        rows = {q: [] for q in QUERIES}
        rows["windows"] = []
        for tag in TAGS:
            capture = f"{P}/{build}-{tag}"
            if not os.path.exists(os.path.join(capture, "trace.perfetto-trace")):
                print(f"missing {capture}")
                continue
            windows = steps(capture)
            rows["windows"] += [(tag, n, s, e) for n, s, e in windows]
            for q in QUERIES:
                for r in query(capture, q):
                    at = num(r.get("uptime_s"))
                    r["tag"] = tag
                    r["step"] = step_of(windows, at) if at is not None else None
                    rows[q].append(r)
        pooled[build] = rows

    print("\n## Texture renders (drawLayer) per step\n")
    print("| build | fast swipe | slow start | cancel | push | edge touch | edge scroll (ms after its start) |")
    print("|---|---|---|---|---|---|---|")
    for build, rows in pooled.items():
        counts = {}
        for tag, name, start, end in rows["windows"]:
            hits = [num(r["uptime_s"]) for r in rows["layerts"] if r["tag"] == tag and r["step"] == name]
            counts.setdefault(kind(name), []).append((len(hits), [round((h - start) * 1000) for h in hits]))
        cell = lambda k: " ".join(str(n) for n, _ in counts.get(k, [])) or "–"
        scroll = " ".join(f"{n}{'@' + ','.join(map(str, at)) if at else ''}" for n, at in counts.get("edgescroll", []))
        pushes = sum(n for k in ("push", "push_slow", "push_cancel", "push_edge") for n, _ in counts.get(k, []))
        print(f"| {build} | {cell('fast')} | {cell('slow')} | {cell('cancel')} | {pushes} in all | {cell('edgetouch')} | {scroll or '–'} |")

    print("\n## Progress to screen, start, release, cancel tail (backlat.sql; medians over gestures)\n")
    print("| build | kind | n | start_to_shrink | write_to_trav | write_to_present | late_draws / writes | release_to_fade | frames_after_settle |")
    print("|---|---|---|---|---|---|---|---|---|")
    for build, rows in pooled.items():
        for k in ("fast", "slow", "cancel"):
            g = [r for r in rows["backlat"] if r["step"] and kind(r["step"]) == k]
            if not g:
                continue
            late = sum(int(num(r["late_draws"]) or 0) for r in g)
            writes = sum(int(num(r["writes"]) or 0) for r in g)
            print(f"| {build} | {k} | {len(g)} | {fmt(med(num(r['start_to_shrink_ms']) for r in g))} | "
                  f"{fmt(med(num(r['write_to_trav_med']) for r in g))} | {fmt(med(num(r['write_to_present_med']) for r in g))} | "
                  f"{late} / {writes} | {fmt(med(num(r['release_to_fade_ms']) for r in g))} | "
                  f"{fmt(med(num(r['frames_after_settle']) for r in g))} ({' '.join(fmt(num(r['frames_after_settle'])) for r in g)}) |")

    print("\n## The gesture's first frames (startframes.sql; medians)\n")
    print("| build | kind | n | frame 1: after start | RT | GPU | layer renders | frame 2 GPU | renders in the 400 ms before |")
    print("|---|---|---|---|---|---|---|---|---|")
    for build, rows in pooled.items():
        for k in ("fast", "slow", "cancel"):
            f1 = [r for r in rows["startframes"] if r["step"] and kind(r["step"]) == k and r["frame"] == "1"]
            f2 = [r for r in rows["startframes"] if r["step"] and kind(r["step"]) == k and r["frame"] == "2"]
            if not f1:
                continue
            print(f"| {build} | {k} | {len(f1)} | {fmt(med(num(r['after_start_ms']) for r in f1))} | "
                  f"{fmt(med(num(r['rt_ms']) for r in f1))} | {fmt(med(num(r['gpu_ms']) for r in f1))} "
                  f"({' '.join(fmt(num(r['gpu_ms'])) for r in f1)}) | {sum(int(r['layers']) for r in f1)} | "
                  f"{fmt(med(num(r['gpu_ms']) for r in f2))} | {sum(int(r['layers_before']) for r in f1)} |")

    print("\n## Vsync to present per gesture frame (presentlat.sql; medians over gestures)\n")
    print("| build | kind | n | avg frames 1-10 | avg frames 11-30 | skipped vsyncs 1-30 |")
    print("|---|---|---|---|---|---|")
    for build, rows in pooled.items():
        for k in ("fast", "slow", "cancel"):
            g = [r for r in rows["presentlat"] if r["step"] and kind(r["step"]) == k]
            if not g:
                continue
            print(f"| {build} | {k} | {len(g)} | {fmt(med(num(r['avg_1_10']) for r in g))} "
                  f"({' '.join(fmt(num(r['avg_1_10'])) for r in g)}) | {fmt(med(num(r['avg_11_30']) for r in g))} | "
                  f"{fmt(med(num(r['skips_1_30']) for r in g))} |")

    print("\n## The release's first frames (releaseframes.sql; frame 1 and 2 of each release)\n")
    print("| build | kind | n | frame 1: after release | RT | actual | layer renders | frame 2 RT | frame 2 actual |")
    print("|---|---|---|---|---|---|---|---|---|")
    for build, rows in pooled.items():
        for k in ("commit", "cancel"):
            f1 = [r for r in rows["releaseframes"] if r["kind"] == k and r["frame"] == "1"]
            f2 = [r for r in rows["releaseframes"] if r["kind"] == k and r["frame"] == "2"]
            if not f1:
                continue
            print(f"| {build} | {k} | {len(f1)} | {fmt(med(num(r['after_release_ms']) for r in f1))} | "
                  f"{fmt(med(num(r['rt_ms']) for r in f1))} | {fmt(med(num(r['actual_ms']) for r in f1))} | "
                  f"{sum(int(r['layer_renders']) for r in f1)} | {fmt(med(num(r['rt_ms']) for r in f2))} | "
                  f"{fmt(med(num(r['actual_ms']) for r in f2))} |")


if __name__ == "__main__":
    main(sys.argv[1:] or ["bg-head", "bg-tex", "bg-c1", "bg-c2"])

#!/usr/bin/env python3
"""What the first frame after idle spends its UI-thread and RenderThread time on: for each Choreographer#doFrame that
follows a gap of more than --gap ms, the app's slices inside it summed by name (the deepest named work: Compose's
recompose, measure, layout and draw sections, RV/View sections, class loading), and the RenderThread's slices
inside the DrawFrames of the same vsync. Medians over all first frames come last.

  framebreak.win.py <trace> [--package com.example.wp04.everything.dev] [--gap 300] [--top 14]

trace_processor runs with USERPROFILE at D:/.caches/perfetto, where its binary was fetched.
"""
import argparse
import csv
import io
import os
import statistics
import subprocess
import sys
import tempfile
from collections import defaultdict

TP = "C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills/profilers/android-profiler/references/perfetto/bin/trace_processor"
TP_ENV = dict(os.environ, USERPROFILE=r"D:\.caches\perfetto")

SQL = """
with proc as (select upid, pid from process where name = '{pkg}' order by upid desc limit 1),
main as (select utid from thread where tid = (select pid from proc)),
rt as (select utid from thread where upid = (select upid from proc) and name = 'RenderThread'),
df as (
  select s.id, s.ts, s.dur, cast(substr(s.name, instr(s.name, ' ') + 1) as int) as vsync,
    (s.ts - lag(s.ts) over (order by s.ts)) as gap
  from slice s join thread_track tt on s.track_id = tt.id
  where tt.utid = (select utid from main) and s.name like 'Choreographer#doFrame %'
),
firsts as (select * from df where gap > {gap} * 1e6),
dfr as (
  select s.id, cast(substr(s.name, instr(s.name, ' ') + 1) as int) as vsync, s.dur
  from slice s join thread_track tt on s.track_id = tt.id
  where tt.utid = (select utid from rt) and s.name like 'DrawFrames %'
)
select 'ui' as thread, f.vsync, round(f.dur / 1e6, 2) as frame_ms, d.name, d.depth - p.depth as rel_depth,
  round(sum(d.dur) / 1e6, 3) as ms, count(*) as n
from firsts f join slice p on p.id = f.id join descendant_slice(f.id) d
group by f.vsync, d.name, rel_depth
union all
select 'rt', f.vsync, round(r.dur / 1e6, 2), d.name, d.depth - p.depth, round(sum(d.dur) / 1e6, 3), count(*)
from firsts f join dfr r on r.vsync = f.vsync join slice p on p.id = r.id join descendant_slice(r.id) d
group by f.vsync, d.name, d.depth - p.depth
order by 2, 1, 7 desc
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("trace")
    ap.add_argument("--package", default="com.example.wp04.everything.dev")
    ap.add_argument("--gap", type=float, default=300.0)
    ap.add_argument("--top", type=int, default=14)
    args = ap.parse_args()
    with tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False) as f:
        f.write(SQL.format(pkg=args.package, gap=args.gap))
        path = f.name
    try:
        result = subprocess.run([sys.executable, TP, "-q", path, args.trace], capture_output=True, text=True, env=TP_ENV)
    finally:
        os.unlink(path)
    lines = [line for line in result.stdout.splitlines() if line.startswith(('"thread"', '"ui"', '"rt"'))]
    if not lines:
        sys.exit(result.stderr[-2000:] or "no first frames")
    rows = list(csv.DictReader(io.StringIO("\n".join(lines))))
    frames = defaultdict(lambda: defaultdict(list))
    totals = {}
    for row in rows:
        key = (row["vsync"], row["thread"])
        totals[key] = float(row["frame_ms"])
        frames[row["vsync"]][row["thread"]].append((float(row["ms"]), int(row["n"]), int(row["rel_depth"]), row["name"]))
    by_name = defaultdict(list)
    for vsync in sorted(frames, key=int):
        for thread in ("ui", "rt"):
            items = sorted(frames[vsync][thread], reverse=True)
            if not items:
                continue
            print(f"vsync {vsync} {thread} {totals[(vsync, thread)]} ms")
            for ms, n, depth, name in items[: args.top]:
                print(f"   {ms:7.3f} ms  x{n:<3d} d{depth}  {name[:110]}")
            for ms, n, depth, name in items:
                by_name[(thread, depth, name[:110])].append(ms)
    count = len(frames)
    print(f"\nmedians over {count} first frames (a frame without the slice counts 0):")
    medians = []
    for (thread, depth, name), values in by_name.items():
        padded = values + [0.0] * (count - len(values))
        medians.append((statistics.median(padded), thread, depth, name, len(values)))
    for median, thread, depth, name, seen in sorted(medians, reverse=True)[: args.top * 2]:
        print(f"   {thread} {median:7.3f} ms  in {seen}/{count}  d{depth}  {name}")


if __name__ == "__main__":
    main()

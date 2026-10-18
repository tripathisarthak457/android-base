#!/usr/bin/env python3
"""Where a cold start's time goes, from a startup-trace.win.sh capture: each startup Perfetto finds for the package
(android.startup.startups), the main thread's time by state and by CPU cluster inside it, and the main thread's
named slices summed by name (top --top), so the steps of two builds can be set side by side.

  startup.win.py <trace> [--package com.example.wp04.everything.dev] [--top 25]

trace_processor runs with USERPROFILE at D:/.caches/perfetto, where its binary was fetched.
"""
import argparse
import csv
import io
import os
import subprocess
import sys
import tempfile

TP = "C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills/profilers/android-profiler/references/perfetto/bin/trace_processor"
TP_ENV = dict(os.environ, USERPROFILE=r"D:\.caches\perfetto")

QUERIES = {
    "startups": """
include perfetto module android.startup.startups;
select startup_id, round(dur / 1e6, 1) as ms, startup_type, package from android_startups
where package = '{pkg}' order by ts
""",
    "states": """
include perfetto module android.startup.startups;
with s as (select startup_id, ts, ts + dur as te from android_startups where package = '{pkg}'),
p as (select upid from process where name = '{pkg}'),
m as (select utid, upid from thread where is_main_thread = 1 and upid in (select upid from p))
select s.startup_id, ts.state, ifnull(ts.io_wait, '') as io_wait,
  case when ts.cpu between 6 and 7 then 'big' when ts.cpu is null then '' else 'little' end as cluster,
  round(sum(min(ts.ts + ts.dur, s.te) - max(ts.ts, s.ts)) / 1e6, 1) as ms
from s join thread_state ts on ts.ts < s.te and ts.ts + ts.dur > s.ts
join m on m.utid = ts.utid
join thread t on t.utid = m.utid join process pr on pr.upid = t.upid and pr.start_ts <= s.te
group by s.startup_id, ts.state, io_wait, cluster order by s.startup_id, ms desc
""",
    "slices": """
include perfetto module android.startup.startups;
with s as (select startup_id, ts, ts + dur as te from android_startups where package = '{pkg}'),
p as (select upid from process where name = '{pkg}'),
m as (select utid from thread where is_main_thread = 1 and upid in (select upid from p))
select sl.name, sl.depth, count(*) as n, round(sum(sl.dur) / 1e6 / (select count(*) from s), 2) as ms_per_start
from s join slice sl on sl.ts >= s.ts and sl.ts < s.te
join thread_track tt on sl.track_id = tt.id
where tt.utid in (select utid from m) and sl.depth <= 3
group by sl.name, sl.depth order by ms_per_start desc limit {top}
""",
}


def run(trace, sql):
    with tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False) as f:
        f.write(sql)
        path = f.name
    try:
        result = subprocess.run([sys.executable, TP, "-q", path, trace], capture_output=True, text=True, env=TP_ENV)
    finally:
        os.unlink(path)
    lines = [line for line in result.stdout.splitlines() if line.startswith('"') or line[:1].isdigit()]
    if not lines:
        return [], result.stderr[-1500:]
    return list(csv.reader(io.StringIO("\n".join(lines)))), ""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("trace")
    ap.add_argument("--package", default="com.example.wp04.everything.dev")
    ap.add_argument("--top", type=int, default=25)
    args = ap.parse_args()
    for name, sql in QUERIES.items():
        rows, error = run(args.trace, sql.format(pkg=args.package, top=args.top))
        print(f"== {name}")
        if error:
            print(error)
        for row in rows:
            print("   " + " | ".join(row))


if __name__ == "__main__":
    main()

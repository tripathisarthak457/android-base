#!/usr/bin/env python3
"""The first frames after idle in a Perfetto capture of the app: UI, RenderThread and GPU time of the first
frame of each transition and the frames after it, the main thread's cpu, and FrameTimeline's verdict.

  firstframes.win.py <trace> [--package com.example.wp04.everything.dev] [--gap 300] [--follow 3]

Windows port of firstframes.py: trace_processor runs with USERPROFILE at D:/.caches/perfetto, where its binary was
fetched, so it never downloads itself into the user profile again.
"""
import argparse, os, subprocess, sys, tempfile

TP = "C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills/profilers/android-profiler/references/perfetto/bin/trace_processor"
TP_ENV = dict(os.environ, USERPROFILE=r"D:\.caches\perfetto")

SQL = """
with proc as (select upid, pid from process where name = '{pkg}' order by upid desc limit 1),
main as (select utid from thread where tid = (select pid from proc)),
df as (
  select s.ts, s.dur, cast(substr(s.name, instr(s.name, ' ') + 1) as int) as vsync, s.id,
    (s.ts - lag(s.ts) over (order by s.ts)) as gap
  from slice s join thread_track tt on s.track_id = tt.id
  where tt.utid = (select utid from main) and s.name like 'Choreographer#doFrame %'
),
anim as (select parent_id, sum(dur) as dur from slice where name = 'animation' group by parent_id),
trav as (select parent_id, sum(dur) as dur from slice where name = 'traversal' group by parent_id),
rt as (
  select cast(substr(s.name, instr(s.name, ' ') + 1) as int) as vsync, s.ts, s.dur
  from slice s join thread_track tt on s.track_id = tt.id join thread t using(utid)
  where t.upid = (select upid from proc) and t.name = 'RenderThread' and s.name like 'DrawFrames %'
),
gpu as (
  select s.ts, s.dur from slice s join thread_track tt on s.track_id = tt.id join thread t using(utid)
  where t.upid = (select upid from proc) and t.name = 'GPU completion' and s.name like 'waiting for GPU completion%'
),
ftl as (
  select a.ts, a.dur, a.jank_type, e.ts as ets, e.dur as edur
  from actual_frame_timeline_slice a left join expected_frame_timeline_slice e
    on e.surface_frame_token = a.surface_frame_token and e.upid = a.upid
  where a.upid = (select upid from proc) and a.layer_name like '%MainActivity%'
)
select round((df.ts - (select min(ts) from df))/1e6,1) as t_ms, round(df.gap/1e6,1) as gap_ms,
  round(df.dur/1e6,2) as ui_ms, round(anim.dur/1e6,2) as anim_ms, round(trav.dur/1e6,2) as trav_ms,
  round(rt.dur/1e6,2) as rt_ms,
  round((select g.dur from gpu g where g.ts >= rt.ts order by g.ts limit 1)/1e6,2) as gpu_ms,
  round(((select g.ts + g.dur from gpu g where g.ts >= rt.ts order by g.ts limit 1) - df.ts)/1e6, 2) as total_ms,
  (select group_concat(distinct cpu) from thread_state ts where ts.utid = (select utid from main) and ts.state = 'Running' and ts.ts < df.ts + df.dur and ts.ts + ts.dur > df.ts) as main_cpu,
  (select f.jank_type || ' late ' || round(((f.ts + f.dur) - (f.ets + f.edur))/1e6, 1) from ftl f where f.ts between df.ts - 2000000 and df.ts + 4000000 limit 1) as frametimeline
from df left join anim on anim.parent_id = df.id left join trav on trav.parent_id = df.id left join rt on rt.vsync = df.vsync
order by df.ts;
"""

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("trace")
    ap.add_argument("--package", default="com.example.wp04.everything.dev")
    ap.add_argument("--gap", type=float, default=300.0)
    ap.add_argument("--follow", type=int, default=3)
    args = ap.parse_args()
    with tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False) as f:
        f.write(SQL.format(pkg=args.package)); path = f.name
    out = subprocess.run([sys.executable, TP, "-q", path, args.trace], capture_output=True, text=True, env=TP_ENV).stdout
    rows = [l for l in out.splitlines() if l and l[0].isdigit()]
    print("t_ms,gap_ms,ui,anim,trav,rt,gpu,total,main_cpu,frametimeline")
    firsts = []
    pending = 0
    for line in rows:
        cols = line.split(",")
        gap = float(cols[1]) if cols[1] not in ("", '"[NULL]"') else 0
        if gap > args.gap:
            pending = args.follow + 1
            firsts.append(cols)
        if pending:
            print(line); pending -= 1
            if pending == 0: print("...")
    def num(c):
        try: return float(c)
        except ValueError: return None
    for name, idx in (("ui", 2), ("rt", 5), ("gpu", 6), ("total", 7)):
        vals = sorted(v for v in (num(c[idx]) for c in firsts) if v is not None)
        if vals:
            print(f"first-frame {name}: median {vals[len(vals)//2]:.2f} max {vals[-1]:.2f} (n={len(vals)})")
    late = sum(1 for c in firsts if num(c[7]) is not None and num(c[7]) > 13.67)
    print(f"first frames over 13.67 ms: {late}/{len(firsts)}")

if __name__ == "__main__":
    main()

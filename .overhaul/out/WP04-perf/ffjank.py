"""ffjank.py <firstfling out-dir>...: the app's frames in firstfling.win.sh's capture from the finger's first move on
(the drag, the lift's fling and the two flings), from FrameTimeline: how many, how many SurfaceFlinger blamed on the
app (a jank type naming the app), and each of those with its step; also how long another app's GL runner ran in the
capture (Google Photos processes new images in shared storage on the GPU)."""
import csv, io, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
QUERY = """
with app as (select upid from process where name like 'com.example.wp04%')
select a.ts, a.dur, a.jank_type, a.present_type from actual_frame_timeline_slice a
where a.upid in (select upid from app) order by a.ts;
"""
OTHERS = """
select round(sum(s.dur)/1e6, 1) ms from sched s join thread t using(utid) where t.name like 'drishti%';
"""


def tp(query, trace, name):
    qpath = os.path.join(HERE, "sql", name)
    with open(qpath, "w", encoding="utf-8") as f:
        f.write(query)
    out = subprocess.run(["bash", os.path.join(HERE, "tp.sh"), qpath, trace], capture_output=True, text=True).stdout
    return list(csv.DictReader(io.StringIO(out.strip())))


for d in sys.argv[1:]:
    steps = [(float(up) * 1e9, name) for name, up in (l.split() for l in open(os.path.join(d, "steps.txt")))]
    lift = next(t for t, n in steps if n == "down")
    trace = os.path.join(d, "trace.perfetto-trace")
    rows = [r for r in tp(QUERY, trace, "ffjank.sql") if float(r["ts"]) >= lift - 0.2e9]
    janky = [r for r in rows if "App" in r["jank_type"]]
    longest = max((float(r["dur"]) for r in rows), default=0) / 1e6
    gl = tp(OTHERS, trace, "ffothers.sql")
    print(f"== {d}: {len(rows)} frames from the first move on, {len(janky)} the app's, longest {longest:.1f} ms; "
          f"another app's GL runner: {gl[0]['ms'] if gl else '?'} ms")
    for r in rows:
        if r in janky:
            at = float(r["ts"])
            where = max((n for t, n in steps if t <= at), default="start", key=lambda n: [s[1] for s in steps].index(n))
            print(f"   {float(r['dur'])/1e6:5.1f} ms  {r['jank_type']:<28} {r['present_type']:<18} after {where}")

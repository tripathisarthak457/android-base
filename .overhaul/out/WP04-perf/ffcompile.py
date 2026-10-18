"""ffcompile.py <firstfling out-dir>...: each GPU program HWUI compiled in firstfling.win.sh's capture, with the Skia op
that needed it, its length, and the step it fell in (steps.txt: the device's uptime after each step; Perfetto's
clock is the same boot clock). Also each RenderThread frame over 12 ms. Runs trace_processor through tp.sh."""
import csv, io, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
QUERY = """
select c.ts, round(c.dur/1e6, 1) ms,
  (select a.name from ancestor_slice(c.id) a where a.name like '%Op' order by a.depth desc limit 1) op,
  (select a.name from ancestor_slice(c.id) a where a.name like 'flush %' limit 1) pass
from slice c where c.name = 'shader_compile'
union all
select sl.ts, round(sl.dur/1e6, 1), 'RenderThread frame', '' from slice sl where sl.name like 'DrawFrames%' and sl.dur > 12e6
order by 1;
"""


def run(trace):
    qpath = os.path.join(HERE, "sql", "ffcompile.sql")
    with open(qpath, "w", encoding="utf-8") as f:
        f.write(QUERY)
    out = subprocess.run(["bash", os.path.join(HERE, "tp.sh"), qpath, trace], capture_output=True, text=True).stdout
    return [r for r in csv.DictReader(io.StringIO(out.strip()))]


for d in sys.argv[1:]:
    steps = []
    for line in open(os.path.join(d, "steps.txt"), encoding="utf-8"):
        name, up = line.split()
        steps.append((float(up), name))
    print(f"== {d}")
    for r in run(os.path.join(d, "trace.perfetto-trace")):
        t = int(r["ts"]) / 1e9
        before = [n for u, n in steps if u <= t]
        after = [n for u, n in steps if u > t]
        where = f"after {before[-1] if before else 'start'}, before {after[0] if after else 'end'}"
        print(f"  {t:12.3f}  {r['ms']:>6} ms  {r['op']:<22} {r['pass']:<16} {where}")

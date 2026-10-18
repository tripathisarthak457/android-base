"""binderphases.py < mainbinder2.sql output: the main thread's synchronous binder calls per cold start, by the phase
they were made in (bindApplication, activityStart, activityResume, a frame, other) and the process that served them."""
import sys, csv, collections
rows = [r for r in csv.reader(sys.stdin) if len(r) == 6][1:]
agg = collections.defaultdict(lambda: [0, 0.0])
starts = set()
for sid, at, dur, server, inside, served in rows:
    starts.add(sid)
    phase = next((p for p in ("bindApplication", "activityStart", "activityResume", "doFrame") if p in inside), "other")
    agg[(phase, server)][0] += 1
    agg[(phase, server)][1] += float(dur)
n = len(starts)
total = sum(v[1] for v in agg.values()) / n
for k in sorted(agg):
    print(f"{k[0]:16s} {k[1]:24s} calls/start {agg[k][0] / n:5.1f}  ms/start {agg[k][1] / n:6.1f}")
print(f"total ms/start {total:.1f} over {n} starts")

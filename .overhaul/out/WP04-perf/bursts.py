"""bursts.py <rtframes.csv>: groups the RenderThread frames of a nav capture into bursts (a gap of 250 ms or more
between frames starts a new one) and prints each burst's length, frame count and medians of its RT stages."""
import csv, statistics, sys
rows = list(csv.DictReader(open(sys.argv[1])))
f = lambda r, k: float(r[k]) if r[k] not in ("", "[NULL]") else 0.0
bursts, cur, last = [], [], None
for r in rows:
    t = f(r, "t_ms")
    if last is not None and t - last >= 250:
        bursts.append(cur); cur = []
    cur.append(r); last = t
bursts.append(cur)
keys = ["rt_ms", "sync", "prep", "exec", "swap", "deq"]
for i, b in enumerate(bursts):
    med = {k: statistics.median(f(r, k) for r in b) for k in keys}
    run = [f(r, "rt_ms") - f(r, "swap") - f(r, "deq") for r in b]
    big = statistics.mean(f(r, "big") for r in b)
    print(f"{i:2d} t {f(b[0],'t_ms'):7.0f} n {len(b):3d} dur {f(b[-1],'t_ms')-f(b[0],'t_ms'):5.0f} | " +
          " ".join(f"{k} {v:.2f}" for k, v in med.items()) + f" | rt-wait med {statistics.median(run):.2f} big {big:.0f}%")

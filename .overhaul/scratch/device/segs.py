import re, sys
base="/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/results"
for v in sys.argv[1:]:
    t=open(f"{base}/fr_{v}/yavg.txt").read()
    fr=re.findall(r"frame:(\d+)\s+pts:\d+\s+pts_time:([\d.]+)\s*\nlavfi.signalstats.YAVG=([\d.]+)", t)
    fr=[(int(a)+2, float(b), float(c)) for a,b,c in fr]
    seg=[]; cur=None
    for n,ts,y in fr:
        if y>0.25:
            if cur is None: cur=[n,ts,n,ts,y]
            else: cur[2]=n; cur[3]=ts; cur[4]=max(cur[4],y)
        elif cur and n-cur[2]>4:
            seg.append(cur); cur=None
    if cur: seg.append(cur)
    print(v, len(fr))
    for s in seg: print(f"  f_{s[0]:04d}-f_{s[2]:04d}  t={s[1]:.3f}-{s[3]:.3f}s ({(s[3]-s[1])*1000:.0f}ms)  maxdiff={s[4]:.1f}")

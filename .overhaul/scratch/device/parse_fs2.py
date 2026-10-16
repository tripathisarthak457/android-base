# Per-frame phases from framestats, plus on-screen present cadence (DisplayPresentTime deltas).
import sys
def load(path):
    rows = []; hdr = None; inblk = False
    for ln in open(path, errors="ignore").read().splitlines():
        if ln.startswith("---PROFILEDATA---"): inblk = not inblk; hdr = None; continue
        if not inblk: continue
        p = ln.strip().rstrip(",").split(",")
        if p[0] == "Flags": hdr = p; continue
        if hdr and len(p) >= len(hdr):
            d = dict(zip(hdr, map(int, p[:len(hdr)])))
            if d["Flags"] == 0: rows.append(d)
    rows.sort(key=lambda d: d["IntendedVsync"]); return rows
def summary(rows, verbose=True):
    ms = lambda a, b, d: (d[b] - d[a]) / 1e6
    t0 = rows[0]["IntendedVsync"]; prevp = None; gaps = []
    for d in rows:
        pres = d.get("DisplayPresentTime", 0)
        dp = (pres - prevp) / 1e6 if prevp and pres > 0 else None
        if pres > 0: prevp = pres
        if dp: gaps.append(dp)
        if verbose:
            print(f'{(d["IntendedVsync"]-t0)/1e6:7.1f} anim/compose={ms("AnimationStart","PerformTraversalsStart",d):6.2f} layout+draw={ms("PerformTraversalsStart","SyncQueued",d):6.2f} '
                  f'rt={ms("SyncStart","FrameCompleted",d):6.2f} gpuDone-issue={ms("IssueDrawCommandsStart","GpuCompleted",d):6.2f} total={ms("IntendedVsync","FrameCompleted",d):6.2f} present_dt={dp if dp is None else round(dp,1)}')
    hitches = []
    pr = [d for d in rows if d.get("DisplayPresentTime", 0) > 0]
    for a, b in zip(pr, pr[1:]):
        dI = (b["IntendedVsync"] - a["IntendedVsync"]) / 1e6
        dP = (b["DisplayPresentTime"] - a["DisplayPresentTime"]) / 1e6
        if dI < 100 and dI + 4 < dP < 100:   # same burst, shown later than its cadence asked for
            hitches.append(round(dP - dI, 1))
    skipped = []
    for a, b in zip(rows, rows[1:]):
        dI = (b["IntendedVsync"] - a["IntendedVsync"]) / 1e6
        if 12.5 < dI < 100: skipped.append(round(dI / 8.333) - 1)
    fr = sorted((d["FrameCompleted"] - d["IntendedVsync"]) / 1e6 for d in rows)
    n = len(fr)
    print(f"frames={n} p50={fr[n//2]:.1f} p90={fr[int(n*.9)]:.1f} max={fr[-1]:.1f}ms | on-screen stalls: {len(hitches)} extra-delay(ms)={hitches} ~late_vsyncs={sum(round(h/8.333) for h in hitches)} | skipped vsyncs inside bursts: {sum(skipped)} in {len(skipped)} gaps")
if __name__ == "__main__":
    v = "-q" not in sys.argv
    for p in [a for a in sys.argv[1:] if a != "-q"]:
        print("===", p.split("/")[-1]); summary(load(p), v)

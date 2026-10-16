# Parses `dumpsys gfxinfo <pkg> framestats` PROFILEDATA blocks; prints per-frame ms breakdown.
import sys
def frames(path):
    lines = open(path, errors="ignore").read().splitlines()
    res = []; hdr = None; inblk = False
    for ln in lines:
        if ln.startswith("---PROFILEDATA---"):
            inblk = not inblk; hdr = None; continue
        if not inblk: continue
        parts = ln.strip().rstrip(",").split(",")
        if parts[0] == "Flags": hdr = parts; continue
        if hdr is None or len(parts) < len(hdr): continue
        d = dict(zip(hdr, [int(x) for x in parts[:len(hdr)]]))
        if d["Flags"] != 0: continue
        iv = d["IntendedVsync"]
        f = lambda a, b: (d[b] - d[a]) / 1e6
        start = d["HandleInputStart"] if d["HandleInputStart"] > 0 else d["AnimationStart"]
        res.append({
            "t": iv,
            "total": f("IntendedVsync", "FrameCompleted"),
            "vsync_delay": f("IntendedVsync", "Vsync"),
            "ui": (d["SyncStart"] - d["AnimationStart"]) / 1e6 if d["AnimationStart"] else None,
            "anim+layout+draw": (d["SyncQueued"] - d["AnimationStart"]) / 1e6,
            "rt": f("SyncStart", "FrameCompleted"),
            "gpu": (d["GpuCompleted"] - d["IssueDrawCommandsStart"]) / 1e6 if d.get("GpuCompleted", 0) > 0 else None,
            "deadline": (d["FrameDeadline"] - d["IntendedVsync"]) / 1e6 if "FrameDeadline" in d else None,
            "interval": d.get("FrameInterval", 0) / 1e6,
        })
    res.sort(key=lambda r: r["t"])
    return res
if __name__ == "__main__":
    fr = frames(sys.argv[1])
    if not fr: print("no frames"); sys.exit()
    t0 = fr[0]["t"]
    over = 0
    for r in fr:
        late = r["total"] > (r["deadline"] or 8.33)
        over += late
        print(f'{(r["t"]-t0)/1e6:8.1f}ms total={r["total"]:6.2f} ui={r["ui"]:6.2f} rt={r["rt"]:6.2f} gpu={(r["gpu"] or 0):6.2f} deadline={r["deadline"]:.2f} {"LATE" if late else ""}')
    tot = sorted(r["total"] for r in fr)
    n = len(tot)
    print(f"frames={n} late={over} p50={tot[n//2]:.2f} p90={tot[int(n*0.9)]:.2f} p99={tot[min(n-1,int(n*0.99))]:.2f} max={tot[-1]:.2f}")

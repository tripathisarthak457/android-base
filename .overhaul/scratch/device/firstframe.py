import sys, glob, statistics as st
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device")
from parse_fs2 import load
def first(path):
    rows = load(path)
    if not rows: return None
    d = rows[0]
    ui = (d["SyncQueued"] - d["AnimationStart"]) / 1e6
    total = (d["FrameCompleted"] - d["IntendedVsync"]) / 1e6
    # vsyncs skipped right after the first frame
    gap = (rows[1]["IntendedVsync"] - d["IntendedVsync"]) / 1e6 if len(rows) > 1 else 0
    gap2 = (rows[2]["IntendedVsync"] - rows[1]["IntendedVsync"]) / 1e6 if len(rows) > 2 else 0
    skipped = max(0, round(gap / 8.333) - 1) + max(0, round(gap2 / 8.333) - 1)
    ui2 = (rows[1]["SyncQueued"] - rows[1]["AnimationStart"]) / 1e6 if len(rows) > 1 else 0
    return ui, ui2, total, skipped, len(rows)
for tag in sys.argv[1:]:
    for side in "ab":
        res = [first(p) for p in sorted(glob.glob(f"{tag}/{side}_*.txt"))]
        res = [r for r in res if r]
        if not res: continue
        u = [r[0] for r in res]; u2 = [r[1] for r in res]; sk = [r[3] for r in res]
        print(f"{tag}/{side}: n={len(res)} firstFrame UI ms median={st.median(u):.1f} max={max(u):.1f} | 2nd frame UI median={st.median(u2):.1f} | skipped vsyncs per transition median={st.median(sk)} max={max(sk)} | frames/transition median={st.median([r[4] for r in res])}")

"""bg-perfrows.py: the perf.sh rows of DESIGN.md §5.5 for the ladder, one line per build and block (bg1, bg2 and
back_during_push's bgp), medians of three runs with the runs in brackets where they decide.
  python -I bg-perfrows.py"""
import json
import os

P = "D:/projects/tools/.overhaul/perf/WP04-perf"
BUILDS = ("bg-head", "bg-tex", "bg-c1", "bg-c2")
ROWS = [
    ("janky %", "pushpop_gesture.janky_pct"),
    ("after first %", "pushpop_gesture.janky_after_first_pct"),
    ("hitch ms/s", "pushpop_gesture.hitch_ms_per_s"),
    ("p99 ms", "pushpop_gesture.p99_ms"),
    ("late first", "pushpop_gesture.late_first_frames"),
    ("first 3 GPU med", "pushpop_gesture.first3_gpu_ms.median"),
    ("gesture first frame UI med", "pushpop_gesture.gesture_first_frame_ms.median"),
    ("PSS MB", "meminfo.pss_mb"),
    ("PSS less graphics MB", "meminfo.pss_app_mb"),
    ("graphics MB", "meminfo.graphics_mb"),
]


def load(folder):
    path = os.path.join(P, folder, "summary.json")
    if not os.path.exists(path):
        retry = os.path.join(P, folder + "-retry", "summary.json")
        path = retry if os.path.exists(retry) else path
    return json.load(open(path, encoding="utf-8"))["metrics"] if os.path.exists(path) else None


print("| build | block | " + " | ".join(name for name, _ in ROWS) + " |")
print("|---|---|" + "---|" * len(ROWS))
for block in ("bg1", "bg2"):
    for build in BUILDS:
        m = load(f"{build}-{block}")
        if m is None:
            print(f"| {build} | {block} | missing |")
            continue
        cells = []
        for _, key in ROWS:
            row = m.get(key)
            cells.append("–" if row is None else f"{row['value']} ({', '.join(str(v) for v in row['runs'])})"
                         if key.endswith(("hitch_ms_per_s", "janky_after_first_pct")) else str(row["value"]))
        print(f"| {build} | {block} | " + " | ".join(cells) + " |")

print("\n| build | back_during_push opaque | lands on list | difference ratios (run 1) |")
print("|---|---|---|---|")
for build in BUILDS:
    m = load(f"{build}-bgp")
    if m is None:
        print(f"| {build} | missing | | |")
        continue
    get = lambda k: m.get(k, {}).get("runs")
    print(f"| {build} | {get('back_during_push.opaque')} | {get('back_during_push.lands_on_list')} | "
          f"{(get('back_during_push.difference_ratios') or ['–'])[0]} |")

"""Recomputes janky_after_first_pct and late first frames from the raw segment dumps of earlier phone runs."""
import glob, os, re, statistics, sys
sys.path.insert(0, "D:/projects/tools/.overhaul/wt/WP04/templates/android/tools/perf")
import parse_gfx, parse_framestats, perf
groups = {"tabs": ["tabs"], "tabs_tap": ["tabs_tap"], "pushpop": ["push", "pop"],
          "pushpop_gesture": ["gesture_push", "gesture_pop"], "rapid_back": ["rapid_back"]}
for base in sys.argv[1:]:
    print("==", base)
    for scenario, names in groups.items():
        per_run = []
        for run in sorted(glob.glob(os.path.join(base, "run*"))):
            files = sorted(f for f in glob.glob(os.path.join(run, "*.txt"))
                           if re.fullmatch(r"(%s)_\d+\.txt" % "|".join(names), os.path.basename(f)))
            if not files:
                continue
            dumps, frames = [], []
            for f in files:
                raw = open(f, errors="ignore").read()
                dumps.append(parse_gfx.parse(raw)); frames.append(parse_framestats.load(raw))
            c = parse_gfx.combine(dumps)
            late, n = parse_framestats.late_first_frames(frames)
            per_run.append((c["janky_pct"], perf.janky_after_first_pct(dumps, frames), late, n, c["p99_ms"],
                            parse_framestats.hitch_ratio(frames)))
        if per_run:
            med = lambda i: statistics.median(r[i] for r in per_run)
            print(f"  {scenario:16} janky {med(0):5.2f}  after-first {med(1):5.2f}  late-first {per_run[0][2]}/{per_run[0][3]}  p99 {med(4)}  hitch {med(5)}  runs {[(r[0], r[1]) for r in per_run]}")

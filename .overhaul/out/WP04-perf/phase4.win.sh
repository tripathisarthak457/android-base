#!/usr/bin/env bash
# The memory bisect after phase3.win.sh: the phase 3d tree (c14848d, whose app PSS was 53 MB on Oct 14) built on this
# machine and measured with the same scenarios, two runs, so its meminfo is taken after the same journey.
#   ./phase4.win.sh > logs/phase4-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
until grep -q "PHASE3 DONE" $O/logs/phase3-win.log 2>/dev/null; do sleep 20; done
until grep -qE "^BUILD OK|rror|FAIL" $O/logs/build-p3d.log 2>/dev/null; do sleep 20; done
grep -q "^BUILD OK" $O/logs/build-p3d.log || { echo "p3d did not build"; exit 1; }
echo "== $(date '+%F %H:%M:%S') phase 4"
$L adb bash -c "RUNS=2 $O/measure.win.sh p3d"
echo "== $(date '+%F %H:%M:%S') PHASE4 DONE"

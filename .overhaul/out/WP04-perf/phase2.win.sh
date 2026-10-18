#!/usr/bin/env bash
# After pipeline.win.sh: the merged tip measured the same way, then a push/pop capture of each build, in one adb hold.
#   ./phase2.win.sh > logs/phase2-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
until grep -q "PIPELINE DONE" $O/logs/pipeline-win.log; do sleep 20; done
echo "== $(date '+%F %H:%M:%S') phase 2"
$L adb bash -c "$O/measure.win.sh tip; $O/nav-traces.win.sh tip head fixed everything"
echo "== $(date '+%F %H:%M:%S') PHASE2 DONE"

#!/usr/bin/env bash
# After phase 10: tab switches alone (tabs, tabs_tap), cardclip against layer and layer against cardclip, three runs
# each, so order and drift fall on both builds: phase 7 saw more slow RenderThread frames on layer's tab switches in
# its one order, though a switch moves nothing the layer wraps.
#   ./phase11.win.sh > logs/phase11-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
until grep -q "PHASE10 DONE" $O/logs/phase10-win.log 2>/dev/null; do sleep 20; done
echo "== $(date '+%F %H:%M:%S') phase 11"
$L adb bash -c "
NOTRACE=1 TAG=-t1 SCENARIOS='tabs tabs_tap' $O/measure.win.sh cardclip layer
NOTRACE=1 TAG=-t2 SCENARIOS='tabs tabs_tap' $O/measure.win.sh layer cardclip
"
echo "== $(date '+%F %H:%M:%S') PHASE11 DONE"

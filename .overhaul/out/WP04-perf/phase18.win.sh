#!/usr/bin/env bash
# HEAD's numbers in one place, with perf.py at 2b91187f (back_during_push's screenshots in /data/local/tmp, which
# this run also checks on the phone): dlplay (HEAD's tree, 9ed16d25) with every measure.win.sh scenario plus
# back_during_push in perf.py's own order, three runs, no trace.
#   ./phase18.win.sh > logs/phase18-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
echo "== $(date '+%F %H:%M:%S') phase 18"
$L adb bash -c "
NOTRACE=1 TAG=-head SCENARIOS='tabs tabs_tap tab1 pushpop pushpop_gesture back_during_push rapid_back scroll_root scroll_minimize meminfo recompose coldstart coldstart_installed' $O/measure.win.sh dlplay
"
echo "== $(date '+%F %H:%M:%S') PHASE18 DONE"

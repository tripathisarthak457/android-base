#!/usr/bin/env bash
# After phase4.win.sh: Perfetto captures of three cold starts of tip and of the before build, for reading where
# the start's time goes (startup-apk.win.sh installs each as perf.sh does).
#   ./phase5.win.sh > logs/phase5-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
until grep -qE "PHASE4 DONE|did not build" $O/logs/phase4-win.log 2>/dev/null; do sleep 20; done
echo "== $(date '+%F %H:%M:%S') phase 5"
$L adb $O/startup-apk.win.sh tip everything
echo "== $(date '+%F %H:%M:%S') PHASE5 DONE"

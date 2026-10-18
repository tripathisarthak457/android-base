#!/usr/bin/env bash
# After phase5.win.sh: the push/pop/gesture captures phase 2 could not take (onboarding after pm clear), for each
# build, in one adb hold.   ./phase6.win.sh > logs/phase6-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
until grep -qE "PHASE5 DONE" $O/logs/phase5-win.log 2>/dev/null; do sleep 20; done
echo "== $(date '+%F %H:%M:%S') phase 6"
$L adb $O/nav-traces.win.sh tip head fixed everything
echo "== $(date '+%F %H:%M:%S') PHASE6 DONE"

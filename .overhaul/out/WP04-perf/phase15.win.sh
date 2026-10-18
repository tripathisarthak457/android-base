#!/usr/bin/env bash
# The first scroll after `pm clear` on wp/WP03's head (`wp03`, c55a8fce: its cards are not clipped to their shape, its
# pull to refresh acts on user input only, its lists have the rubber band): firstfling.win.sh twice, then
# scroll-trace.win.sh (perf.py's flings after `pm clear`, down and back up into the top edge, then again cached).
#   ./phase15.win.sh > logs/phase15-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
APK=app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
until grep -q "^BUILD OK" $O/logs/build-wp03.log 2>/dev/null; do
  grep -qE "BUILD FAILED|^Error|error:" $O/logs/build-wp03.log 2>/dev/null && { echo "wp03 did not build"; exit 1; }
  sleep 20
done
echo "== $(date '+%F %H:%M:%S') phase 15"
$L adb bash -c "
adb -s 9a7c74f4 install -r -t -d $O/wp03/$APK > /dev/null && echo 'wp03 installed'
$O/firstfling.win.sh $P/firstfling-wp03-1
$O/firstfling.win.sh $P/firstfling-wp03-2
$O/scroll-trace.win.sh $P/wp03-scroll
"
echo "== $(date '+%F %H:%M:%S') PHASE15 DONE"

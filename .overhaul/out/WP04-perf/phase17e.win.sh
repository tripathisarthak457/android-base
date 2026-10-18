#!/usr/bin/env bash
# After phase 17d: firstfling.win.sh with the drag run on the phone (ffdrag.sh), once on noclip and once on dl, so
# the drag, the lift's fling and both flings are in one capture whatever adb's latency on the host.
#   ./phase17e.win.sh > logs/phase17e-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
APK=app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
until grep -q "PHASE17D DONE" $O/logs/phase17d-win.log 2>/dev/null; do sleep 15; done
echo "== $(date '+%F %H:%M:%S') phase 17e"
$L adb bash -c "
adb -s 9a7c74f4 install -r -t -d $O/noclip/$APK > /dev/null && echo 'noclip installed'
$O/firstfling.win.sh $P/firstfling4-noclip
adb -s 9a7c74f4 install -r -t -d $O/dl/$APK > /dev/null && echo 'dl installed'
$O/firstfling.win.sh $P/firstfling4-dl
"
echo "== $(date '+%F %H:%M:%S') PHASE17E DONE"

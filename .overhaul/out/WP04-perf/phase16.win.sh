#!/usr/bin/env bash
# The card-clip reading of §2i tested directly: `noclip` is HEAD's everything project with one change made in the
# generated copy only (AppClickableSurface draws the card's fill and border before its clip, so the rounded clip no
# longer covers them); firstfling.win.sh twice on it.
#   ./phase16.win.sh > logs/phase16-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
APK=app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
until grep -q "^BUILT noclip" $O/logs/build-noclip.log 2>/dev/null; do
  grep -qE "BUILD FAILED" $O/logs/build-noclip.log 2>/dev/null && { echo "noclip did not build"; exit 1; }
  sleep 20
done
echo "== $(date '+%F %H:%M:%S') phase 16"
$L adb bash -c "
adb -s 9a7c74f4 install -r -t -d $O/noclip/$APK > /dev/null && echo 'noclip installed'
$O/firstfling.win.sh $P/firstfling-noclip-1
$O/firstfling.win.sh $P/firstfling-noclip-2
adb -s 9a7c74f4 install -r -t -d $O/dl/$APK > /dev/null && echo 'dl installed again'
"
echo "== $(date '+%F %H:%M:%S') PHASE16 DONE"

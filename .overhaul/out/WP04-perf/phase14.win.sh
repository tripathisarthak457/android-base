#!/usr/bin/env bash
# The first scroll after `pm clear` on dl (HEAD e2fdb08's tree): firstfling.win.sh, twice, so a compile seen once is
# seen again.
#   ./phase14.win.sh > logs/phase14-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
APK=app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
echo "== $(date '+%F %H:%M:%S') phase 14"
$L adb bash -c "
adb -s 9a7c74f4 install -r -t -d $O/dl/$APK > /dev/null && echo 'dl installed'
$O/firstfling.win.sh $P/firstfling-dl-1
$O/firstfling.win.sh $P/firstfling-dl-2
"
echo "== $(date '+%F %H:%M:%S') PHASE14 DONE"

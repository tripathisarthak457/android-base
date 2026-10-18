#!/usr/bin/env bash
# After phase 17c: the back card held at 45 %, full resolution, on dlplay (HEAD) and on cardtex.
#   ./phase17d.win.sh > logs/phase17d-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
APK=app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
until grep -q "PHASE17C DONE" $O/logs/phase17c-win.log 2>/dev/null; do sleep 15; done
echo "== $(date '+%F %H:%M:%S') phase 17d"
mkdir -p $P/cardtex-held
$L adb bash -c "
adb -s 9a7c74f4 install -r -t -d $O/cardtex/$APK > /dev/null && echo 'cardtex installed'
$O/heldshot.win.sh $P/cardtex-held/cardtex.png com.example.wp04.everything.dev
$O/heldshot.win.sh $P/cardtex-held/dlplay.png com.example.wp04.dlplay.dev
"
echo "== $(date '+%F %H:%M:%S') PHASE17D DONE"

#!/usr/bin/env bash
# §2i again with the harness's screenshots in /data/local/tmp (phase 16's captures had Google Photos processing
# them on the GPU and four cores in the middle of the flings): firstfling.win.sh twice on noclip and twice on dl,
# alternated, so the only difference left is the card's clip.
#   ./phase17a.win.sh > logs/phase17a-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
APK=app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
echo "== $(date '+%F %H:%M:%S') phase 17a"
$L adb bash -c "
for i in 1 2; do
  adb -s 9a7c74f4 install -r -t -d $O/noclip/$APK > /dev/null && echo 'noclip installed'
  $O/firstfling.win.sh $P/firstfling2-noclip-\$i
  adb -s 9a7c74f4 install -r -t -d $O/dl/$APK > /dev/null && echo 'dl installed'
  $O/firstfling.win.sh $P/firstfling2-dl-\$i
done
"
echo "== $(date '+%F %H:%M:%S') PHASE17A DONE"

#!/usr/bin/env bash
# After phase 17b: firstfling.win.sh once more on noclip and on dl with a 50 s capture (phase 17a's 28 s ended
# before the flings while a Gradle build slowed adb down), so the lift's fling and both flings are in the capture
# with nothing else on the GPU. Puts dl back.
#   ./phase17c.win.sh > logs/phase17c-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
APK=app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
until grep -qE "PHASE17B DONE|did not build" $O/logs/phase17b-win.log 2>/dev/null; do sleep 20; done
echo "== $(date '+%F %H:%M:%S') phase 17c"
$L adb bash -c "
adb -s 9a7c74f4 install -r -t -d $O/noclip/$APK > /dev/null && echo 'noclip installed'
$O/firstfling.win.sh $P/firstfling3-noclip
adb -s 9a7c74f4 install -r -t -d $O/dl/$APK > /dev/null && echo 'dl installed'
$O/firstfling.win.sh $P/firstfling3-dl
"
echo "== $(date '+%F %H:%M:%S') PHASE17C DONE"

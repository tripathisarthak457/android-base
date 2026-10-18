#!/usr/bin/env bash
# After phase 9:
#  1. build `dl` from the worktree: layer's tree plus DevLaunchOptions checking for a stored session without
#     decrypting it (Gradle lock);
#  2. in one adb hold: the scroll scenario's first flings after `pm clear` on layer, traced (scroll-trace.win.sh);
#     perf.sh's cold start on layer and dl, in both orders, three runs each; a startup capture of dl.
#   ./phase10.win.sh > logs/phase10-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
echo "== $(date '+%F %H:%M:%S') phase 10: building dl"
$O/build-wt.win.sh dl > $O/logs/build-dl.log 2>&1 || { echo "dl did not build (logs/build-dl.log)"; exit 1; }
tail -1 $O/logs/build-dl.log
until grep -q "PHASE9 DONE" $O/logs/phase9-win.log 2>/dev/null; do sleep 20; done
echo "== $(date '+%F %H:%M:%S') phase 10: phone"
$L adb bash -c "
adb -s 9a7c74f4 install -r -t -d $O/layer/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk > /dev/null && echo 'layer installed'
$O/scroll-trace.win.sh $P/layer-scroll
NOTRACE=1 TAG=-c1 SCENARIOS=coldstart $O/measure.win.sh layer dl
NOTRACE=1 TAG=-c2 SCENARIOS=coldstart $O/measure.win.sh dl layer
$O/startup-trace.win.sh $P/dl-startup com.example.wp04.everything.dev
"
echo "== $(date '+%F %H:%M:%S') PHASE10 DONE"

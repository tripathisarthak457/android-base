#!/usr/bin/env bash
# The back-card A/B after phase2.win.sh: cardclip (the page beneath drawn only around the card) measured on the
# gesture, push/pop and rapid Back scenarios, then recordings of a slow swipe on cardclip and on tip.
#   ./phase3.win.sh > logs/phase3-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
until grep -q "PHASE2 DONE" $O/logs/phase2-win.log 2>/dev/null; do sleep 20; done
until grep -qE "^BUILT cardclip|rror" $O/logs/build-cardclip.log 2>/dev/null; do sleep 20; done
grep -q "^BUILT cardclip" $O/logs/build-cardclip.log || { echo "cardclip did not build"; exit 1; }
echo "== $(date '+%F %H:%M:%S') phase 3"
$L adb bash -c "SCENARIOS='pushpop_gesture pushpop rapid_back' $O/measure.win.sh cardclip; $O/rec-gesture.win.sh $P/cardclip-rec; $O/nav-traces.win.sh cardclip"
echo "== $(date '+%F %H:%M:%S') PHASE3 DONE"

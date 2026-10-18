#!/usr/bin/env bash
# The one app-side lever left for the back gesture (handoff §2h), measured for the user's decision, not committed:
# `cardtex` is HEAD (9ed16d25) with the card's pose layer composited offscreen for the whole gesture
# (backcard-texture.patch), against `dlplay` (the same tree without it). pushpop_gesture alone, dlplay then
# cardtex, then cardtex then dlplay, three runs each, so drift and order fall on both; then a nav capture of
# cardtex (layer-nav is the baseline capture) and a recording of a slow swipe held at 45 % for the text and the fade.
#   ./phase17b.win.sh > logs/phase17b-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
until grep -q "^BUILT cardtex" $O/logs/build-cardtex.log 2>/dev/null; do
  grep -qE "BUILD FAILED" $O/logs/build-cardtex.log 2>/dev/null && { echo "cardtex did not build"; exit 1; }
  sleep 20
done
echo "== $(date '+%F %H:%M:%S') phase 17b"
$L adb bash -c "
NOTRACE=1 TAG=-g1 SCENARIOS=pushpop_gesture $O/measure.win.sh dlplay cardtex
NOTRACE=1 TAG=-g2 SCENARIOS=pushpop_gesture $O/measure.win.sh cardtex dlplay
$O/nav-traces.win.sh cardtex
$O/rec-gesture.win.sh $P/cardtex-rec
"
echo "== $(date '+%F %H:%M:%S') PHASE17B DONE"

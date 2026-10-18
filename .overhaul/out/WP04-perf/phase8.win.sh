#!/usr/bin/env bash
# One adb hold: cold starts of the old template (Oct 9's base build, as the phone has it) against layer, interleaved;
# then the back gesture alone, cardclip against layer and layer against cardclip, three runs each, so order and drift
# fall on both builds.   ./phase8.win.sh > logs/phase8-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
echo "== $(date '+%F %H:%M:%S') phase 8"
$L adb bash -c "
KEEP=base $O/coldstart-ab.win.sh base-vs-layer 15 base=com.example.wp04.base.dev layer=com.example.wp04.everything.dev
NOTRACE=1 TAG=-g1 SCENARIOS=pushpop_gesture $O/measure.win.sh cardclip layer
NOTRACE=1 TAG=-g2 SCENARIOS=pushpop_gesture $O/measure.win.sh layer cardclip
"
echo "== $(date '+%F %H:%M:%S') PHASE8 DONE"

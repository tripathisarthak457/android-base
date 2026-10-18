#!/usr/bin/env bash
# Phase 8 again (it never reached the phone: it was still waiting for the adb lock when the session that queued it
# ended), with startup captures of the old template and layer, and layer's overdraw, in one adb hold:
#  1. cold starts of the old template (Oct 9's base build, as the phone has it) against layer, interleaved;
#  2. a Perfetto capture of three cold starts of each (layer is the one coldstart-ab installed last);
#  3. layer's overdraw: list, detail, back card held mid-swipe;
#  4. the back gesture alone, cardclip against layer and layer against cardclip, three runs each, so order and drift
#     fall on both builds.
#   ./phase9.win.sh > logs/phase9-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
echo "== $(date '+%F %H:%M:%S') phase 9"
$L adb bash -c "
KEEP=base $O/coldstart-ab.win.sh base-vs-layer 15 base=com.example.wp04.base.dev layer=com.example.wp04.everything.dev
$O/startup-trace.win.sh $P/layer-startup com.example.wp04.everything.dev
$O/startup-trace.win.sh $P/base-startup com.example.wp04.base.dev
$O/overdraw.win.sh $P/layer-overdraw
NOTRACE=1 TAG=-g1 SCENARIOS=pushpop_gesture $O/measure.win.sh cardclip layer
NOTRACE=1 TAG=-g2 SCENARIOS=pushpop_gesture $O/measure.win.sh layer cardclip
"
echo "== $(date '+%F %H:%M:%S') PHASE9 DONE"

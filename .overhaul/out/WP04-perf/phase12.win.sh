#!/usr/bin/env bash
# Cold start against the old template, after phases 10 and 11 (all on the phone in one adb hold):
#  1. dl (HEAD e2fdb08's tree) installed and prepared as perf.sh leaves an app, then cold starts of the old template
#     and dl interleaved, 15 rounds (coldstart-ab.win.sh; the old template as the phone has it from Oct 9);
#  2. one capture of six cold starts of each, alternated, with CPU callstack samples of both apps
#     (startup-ab-perf.win.sh), to put the new app's extra main-thread time on code.
#   ./phase12.win.sh > logs/phase12-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
echo "== $(date '+%F %H:%M:%S') phase 12"
$L adb bash -c "
KEEP=base $O/coldstart-ab.win.sh base-vs-dl 15 base=com.example.wp04.base.dev dl=com.example.wp04.everything.dev
$O/startup-ab-perf.win.sh $P/startabperf-base-dl 6 com.example.wp04.base.dev com.example.wp04.everything.dev 500
"
echo "== $(date '+%F %H:%M:%S') PHASE12 DONE"

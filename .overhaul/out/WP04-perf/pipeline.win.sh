#!/usr/bin/env bash
# Round 1's unfinished comparison on this machine: build before (3c9c403), fixed (730f8e0), floor and head
# (fb866b3, KeptTabs), then measure all four in one adb hold.
#   ./pipeline.win.sh > logs/pipeline-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
echo "== $(date '+%F %H:%M:%S') build"
$O/build.win.sh everything=3c9c403 fixed=730f8e0 floor=730f8e0 head=fb866b3 || { echo "BUILD FAILED $?"; exit 1; }
echo "== $(date '+%F %H:%M:%S') measure"
$L adb $O/measure.win.sh everything fixed floor head
echo "== $(date '+%F %H:%M:%S') PIPELINE DONE"

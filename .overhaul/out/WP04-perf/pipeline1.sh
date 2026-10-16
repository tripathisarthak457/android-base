#!/usr/bin/env bash
# Round 1: once the fixed build is green, build the floor variant, then measure HEAD, fixed and floor in one adb hold.
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
cd $O
until grep -q -E 'TEST-THEN-BUILD OK|GENERATE FAILED|BUILD FAILED|FAILURE:' logs/test-then-build-fixed.log; do sleep 10; done
grep -q 'TEST-THEN-BUILD OK' logs/test-then-build-fixed.log || { echo "fixed build not green"; exit 1; }
./floor-build.sh > logs/floor-build.log 2>&1 || { echo "floor build failed"; exit 1; }
echo "== $(date +%H:%M:%S) floor built"
$L adb ./measure.sh everything fixed floor > logs/measure1.log 2>&1
echo "== $(date +%H:%M:%S) measure exit $?"
echo PIPELINE1 DONE

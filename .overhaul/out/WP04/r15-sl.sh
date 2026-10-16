#!/usr/bin/env bash
# Run 15: standard and lean at HEAD (perf iteration 1 and the split profiles), then their gates on the phone.
#   ./r15-sl.sh > logs/r15-sl.log 2>&1
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04; L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
cd $O
./r14-build.sh standard lean > logs/r15-build-sl.log 2>&1 || { echo "build failed: logs/r15-build-sl.log"; exit 1; }
echo "== $(date +%H:%M:%S) built"
$L adb ./phone-gates.sh phase3c standard lean > logs/r15-gates-sl.log 2>&1
echo "== $(date +%H:%M:%S) gates exit $?"
echo R15-SL DONE

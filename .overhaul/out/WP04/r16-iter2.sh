#!/usr/bin/env bash
# Run 16, perf iteration 2: standard and lean at HEAD (everything was built by the first pass), then the
# three gates on the phone while the template's local acceptance runs under the Gradle lock.
#   ./r16-iter2.sh > logs/r16-iter2.log 2>&1
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04; L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
cd $O
./r14-build.sh standard lean > logs/r16-build3.log 2>&1 || { echo "build failed: logs/r16-build3.log"; exit 1; }
echo "== $(date +%H:%M:%S) built $(cat final-everything/BUILT_FROM) $(cat final-standard/BUILT_FROM) $(cat final-lean/BUILT_FROM)"
$L adb ./phone-gates.sh phase3e everything standard lean > logs/r16-gates2.log 2>&1 &
gates=$!
./accept-local.sh > logs/r16-accept-local.log 2>&1
echo "== $(date +%H:%M:%S) accept-local exit $?"
wait $gates
echo "== $(date +%H:%M:%S) gates done"
echo R16-ITER2 DONE

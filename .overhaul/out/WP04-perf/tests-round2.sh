#!/usr/bin/env bash
# The tests that cover round 2's uncommitted fixes (the hidden tab's lifecycle timing, the sample
# detail's related rows), under the Gradle lock.   ./tests-round2.sh > logs/tests-round2.log 2>&1
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04-perf; L=$OV/bin/locked
$L gradle bash -c '
set -euo pipefail
cd '"$W"'/templates/android
./gradlew :core:navigation:testDebugUnitTest :feature:sample:testDebugUnitTest --console=plain
echo TESTS OK'
echo "== exit $?"

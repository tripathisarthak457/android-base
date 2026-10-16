#!/usr/bin/env bash
# Run 14, perf iteration 1 (lower page clipped during slides, no tab veil, no window background
# after the first frame, screen bars by content key): the navigation and app unit tests, then the
# three presets generated from the worktree and built, all in one Gradle lock hold.
#   ./r14-iter1.sh > logs/r14-iter1.log 2>&1
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
$L gradle bash -c '
set -uo pipefail
cd '"$W"'/templates/android
./gradlew :core:navigation:testDebugUnitTest :app:testDevDebugUnitTest :catalog:compileDebugKotlin --console=plain --continue
echo "== unit tests exit $?"
'

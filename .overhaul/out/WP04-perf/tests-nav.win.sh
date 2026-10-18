#!/usr/bin/env bash
# The navigation module's unit tests and the design system's compile, run in place in the worktree's template
# under the Gradle lock: what covers a change to the shell, the stack or the bar before it is committed.
#   ./tests-nav.win.sh > logs/<step>.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP04-perf; L=$OV/bin/locked
$L gradle bash -c '
set -euo pipefail
cd '"$W"'/templates/android
./gradlew :core:navigation:testDebugUnitTest :core:designsystem:compileDebugKotlin --console=plain
echo TESTS OK'
echo "== exit $?"

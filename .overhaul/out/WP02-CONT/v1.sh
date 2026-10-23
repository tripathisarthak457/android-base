#!/usr/bin/env bash
# Run 1, verification batch 1: build-logic tests; the new tests red on staging's code and green on the branch;
# H-8 (an incremental compile must not let checkComposeStability pass on a partial report).
set -uo pipefail
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
GW="bash $OV/out/WP02/gw.win.sh"
STG="C:/Users/CRAZYH~1/AppData/Local/Temp/claude/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/scratchpad/stg"
step() { echo; echo "== $(date +%H:%M:%S) $*"; }

step "a. build-logic tests on the branch"
(cd $W/templates/android && $L gradle $GW --offline :build-logic:convention:test --console=plain) ; echo "rc=$?"

step "b. AndroidLoggerTest before the move (expect a compile failure)"
(cd $O/logger-before && $L gradle $GW --offline :app:testDevDebugUnitTest --tests '*AndroidLoggerTest*' --console=plain) ; echo "rc=$?"

step "c. staging's standard preset with AuthFrameTest (expect the top-bar test to fail)"
rm -rf $O/std-before
(cd $STG/generator && python -B create_project.py --spec tests/fixtures/v2/standard.json --out $O/std-before --no-zip --force > /dev/null)
mkdir -p $O/std-before/feature/auth/src/test/kotlin/com/example/standard/feature/auth
sed 's/com\.base\.app/com.example.standard/g' $W/templates/android/feature/auth/src/test/kotlin/com/base/app/feature/auth/AuthFrameTest.kt > $O/std-before/feature/auth/src/test/kotlin/com/example/standard/feature/auth/AuthFrameTest.kt
(cd $O/std-before && $L gradle $GW --offline :feature:auth:testDebugUnitTest --tests '*AuthFrameTest*' --console=plain) ; echo "rc=$?"

h8() {
  local P=$1
  local DS=$P/core/designsystem/src/main/kotlin/com/example/standard/core/designsystem
  printf 'package com.example.standard.core.designsystem\n\nimport androidx.compose.runtime.Composable\n\n@Composable\nfun H8Planted(items: MutableList<String>) {\n    items.size\n}\n' > $DS/H8Planted.kt
  (cd $P && $L gradle $GW --offline :core:designsystem:compileDebugKotlin --console=plain | tail -3)
  local b=$(ls $DS/component/button/*.kt | head -1)
  printf '\nprivate const val H8_PROBE = 1\n' >> "$b"
  (cd $P && $L gradle $GW --offline :core:designsystem:compileDebugKotlin --console=plain | tail -3)
  echo "raw report lines: $(wc -l < $P/core/designsystem/build/compose_reports/designsystem-composables.txt)"
  (cd $P && $L gradle $GW --offline :core:designsystem:checkComposeStability --console=plain) > $O/logs/v1-h8-$(basename $P).log 2>&1; local rc=$?
  echo "check rc=$rc"; grep -n 'H8Planted\|BUILD' $O/logs/v1-h8-$(basename $P).log | head -5
  rm -f $DS/H8Planted.kt
}

step "c2. H-8 on staging's code (expect the check to pass on a partial report)"
h8 $O/std-before

step "d. the branch's standard preset: AuthFrameTest and AndroidLoggerTest"
rm -rf $O/std-after
(cd $W/generator && python -B create_project.py --spec tests/fixtures/v2/standard.json --out $O/std-after --no-zip --force > /dev/null)
(cd $O/std-after && $L gradle $GW --offline :feature:auth:testDebugUnitTest --tests '*AuthFrameTest*' :app:testDevDebugUnitTest --tests '*AndroidLoggerTest*' --console=plain) ; echo "rc=$?"

step "d2. H-8 on the branch (expect the check to fail on H8Planted)"
h8 $O/std-after

echo; echo "V1 DONE $(date +%H:%M:%S)"

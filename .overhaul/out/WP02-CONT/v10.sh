#!/usr/bin/env bash
# Run 5, batch 10: `check` on the two fixtures the scaffold lands in (orders, trips, wallet; database on, so each module
# is OfflineFirst over its own table), which accept ran only as assemble + unit tests. The projects are accept's,
# generated at 32b86b70 (this head); the guard step restored trips' build file and the rules step deleted its plant.
#   bash v10.sh > logs/v10.log 2>&1      (in the background)
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
GW="bash $OV/out/WP02/gw.win.sh"
step() { echo; echo "== $(date +%H:%M:%S) $*"; }
step "branch $(git -C $W rev-parse --short HEAD) with $(git -C $W status --porcelain | wc -l) changed paths"

for p in modules-compact modules-modular; do
  P=$O/accept/$p
  step "$p: generated at $(python -I -c "import json,sys; print(json.load(open(sys.argv[1]))['generator']['commit'])" $P/generator-spec.json 2>/dev/null)"
  diff -q $O/logs/accept/trips.build.gradle.kts.orig $P/feature/trips/build.gradle.kts >/dev/null 2>&1 && echo "trips build file restored" || echo "trips build file: CHECK"
  (cd $P && $L gradle $GW --offline check --continue --console=plain) > $O/logs/v10-check-$p.log 2>&1; echo "rc=$?"
  grep -E "^BUILD|FAILED|^\[locked|actionable tasks" $O/logs/v10-check-$p.log | head -20
  grep -E "^> Task :(feature|data):(orders|trips|wallet):(lint|verifyCodeRules|verifyModuleDependencies|testDebugUnitTest)" $O/logs/v10-check-$p.log | sort | uniq | head -40
done

echo; echo "V10 DONE $(date +%H:%M:%S)"

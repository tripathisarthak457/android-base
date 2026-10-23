#!/usr/bin/env bash
# Run 2, batch 3: the scaffold compiled and tested at six module-name lengths (3 to 30) in each data shape:
# offline-first (network + database), network only, no source. The app assembles, and each module's unit tests and
# code rules pass. Then the full generator suite.
#   bash v3.sh > logs/v3.log 2>&1
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
GW="bash $OV/out/WP02/gw.win.sh"
step() { echo; echo "== $(date +%H:%M:%S) $*"; }

# All three first, so the tree can change while the builds run.
for s in c-netdb c-net c-local; do
  step "$s: generate at $(git -C $W rev-parse --short HEAD) with $(git -C $W status --porcelain | wc -l) changed paths"
  rm -rf $O/$s
  (cd $W/generator && python -B create_project.py --spec $O/specs/$s.json --out $O/$s --no-zip --force > $O/logs/v3-gen-$s.log 2>&1) || echo "GEN FAILED $s"
done
echo "GENERATED $(date +%H:%M:%S)"

for s in c-netdb c-net c-local; do
  tasks=":app:assembleDevDebug"
  for m in $(python -c "import json; print(' '.join(json.load(open('$O/specs/$s.json'))['feature_modules']))"); do
    tasks="$tasks :feature:$m:testDebugUnitTest :feature:$m:verifyCodeRules :data:$m:testDebugUnitTest :data:$m:verifyCodeRules"
  done
  step "$s: build"
  (cd $O/$s && $L gradle $GW --offline $tasks --continue --console=plain) > $O/logs/v3-$s.log 2>&1; echo "rc=$?"
  grep -E "FAILED|BUILD|tests completed|e: file" $O/logs/v3-$s.log | head -20
done

step "generator suite"
(cd $W/generator && $OV/bin/memwait python -B -m unittest discover -s tests -t .) > $O/logs/v3-gen-suite.log 2>&1; echo "rc=$?"
tail -3 $O/logs/v3-gen-suite.log

echo; echo "V3 DONE $(date +%H:%M:%S)"

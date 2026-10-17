#!/usr/bin/env bash
# Windows port of accept-local.sh: accept.sh's lines that need no device. The generator's suite is judged against
# the Windows host baseline (gen-tests.win.py) until package WIN merges.
#   ./accept-local.win.sh > logs/<step>.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
echo "== $(date +%H:%M:%S) at $(git -C $W rev-parse --short HEAD) $(git -C $W status --short | wc -l) dirty"
cd $W/templates/android
$L gradle bash -c './gradlew :core:navigation:testDebugUnitTest :app:testDevDebugUnitTest --console=plain && ./gradlew build --console=plain'
echo "== $(date +%H:%M:%S) gradle done"
python3 tools/perf/test_perf.py
python3 tools/adaptive/test_adaptive.py
cd $W/generator
python3 $O/gen-tests.win.py
python3 -m tests.consistency --quick
python3 -m genkit.core.catalogue --check
echo "== $(date +%H:%M:%S)"
echo ACCEPT-LOCAL OK

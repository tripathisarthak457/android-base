#!/usr/bin/env bash
# accept.sh's lines that need no device: the template's tests and full build, the perf tool tests, the
# generator's tests, consistency and catalogue checks. The presets are generated and built by
# r12-build.sh/emu-session.sh, and measured by accept.sh once the phone is back.
#   ./accept-local.sh > logs/<step>.log 2>&1
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked
echo "== at $(git -C $W rev-parse --short HEAD) $(git -C $W status --short | wc -l) dirty"
cd $W/templates/android
$L gradle bash -c './gradlew :core:navigation:testDebugUnitTest :app:testDevDebugUnitTest --console=plain && ./gradlew build --console=plain'
python3 tools/perf/test_perf.py
python3 tools/adaptive/test_adaptive.py
cd $W/generator && python3 -m unittest discover -s tests -t . && python3 -m tests.consistency --quick
python3 -m genkit.core.catalogue --check
echo ACCEPT-LOCAL OK

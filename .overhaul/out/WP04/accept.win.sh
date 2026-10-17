#!/usr/bin/env bash
# Windows port of accept.sh (WP04 acceptance, design/wp/WP04.md). Needs the phone attached and unlocked. The
# generator's suite is judged against the Windows host baseline (gen-tests.win.py) and its consistency matrix runs
# through consistency.win.py until package WIN merges. The Gradle steps share one lock hold and the device gates
# another (final-gates.win.sh), so another package's build cannot slip in between the steps of one acceptance.
#   ./accept.win.sh > logs/<step>.log 2>&1
set -euo pipefail
# The phone only: connected tasks use every attached device.
export ANDROID_SERIAL=9a7c74f4
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04; P=$OV/perf/WP04
echo "== $(date +%H:%M:%S) at $(git -C $W rev-parse --short HEAD) $(git -C $W status --short | wc -l) dirty"
cd $W/templates/android
python3 tools/perf/test_perf.py
python3 tools/adaptive/test_adaptive.py
cd $W/generator
python3 $O/gen-tests.win.py
# The runner's process pool cannot spawn workers from a package's __main__ on Windows (consistency.win.py says why).
python3 $O/consistency.win.py --quick
python3 -m genkit.core.catalogue --check
for n in everything standard lean; do
  python3 $O/rmtree-long.py $O/final-$n
  python3 create_project.py --spec tests/fixtures/v2/$n.json --set package_name=com.example.wp04.$n --out $O/final-$n --no-zip --force
  git -C $W rev-parse --short HEAD > $O/final-$n/BUILT_FROM
done
echo "== $(date +%H:%M:%S) generated"
# Four R8 tasks at once exhaust the 3 GB daemon heap in `build` (GC overhead limit, run 22); the heap stays as it is.
$L gradle bash -c '
set -euo pipefail
cd '"$W"'/templates/android
./gradlew :core:navigation:testDebugUnitTest :app:testDevDebugUnitTest --console=plain
echo "== $(date +%H:%M:%S) tests"
./gradlew build --max-workers=2 --console=plain
echo "== $(date +%H:%M:%S) template built"
cd '"$O"'/final-everything
./gradlew :app:assembleDevBenchmarkRelease :benchmark:assemble --console=plain
for n in standard lean; do (cd '"$O"'/final-$n && ./gradlew :app:assembleDevRelease --console=plain); done'
echo "== $(date +%H:%M:%S) built"
cd $O
$L adb ./final-gates.win.sh
cd $O/final-everything
$L adb $L gradle ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.wp04.everything.benchmark.rapidback.RapidBackBenchmark --console=plain
# Generated rules, not only the wildcard safety net: the shell's own classes are named.
test "$(grep -c '^HSPLcom/example/wp04/everything/core/navigation/AppShellKt;' core/navigation/src/main/baseline-prof.txt)" -gt 0
echo "== $(date +%H:%M:%S)"
echo ACCEPT OK

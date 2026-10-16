#!/usr/bin/env bash
# WP04 acceptance (design/wp/WP04.md). Needs WP01 and WP02-M1 merged into wp/WP04 and the phone unlocked.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
# The phone only: an emulator may be running for another agent, and connected tasks use every device.
export ANDROID_SERIAL=9a7c74f4
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04; P=$OV/perf/WP04
cd $W/templates/android
$L gradle ./gradlew :core:navigation:testDebugUnitTest :app:testDevDebugUnitTest --console=plain
$L gradle ./gradlew build --console=plain
python3 tools/perf/test_perf.py
cd $W/generator && python3 -m unittest discover -s tests -t . && python3 -m tests.consistency --quick
python3 -m genkit.core.catalogue --check
for n in everything standard lean; do
  python3 create_project.py --spec tests/fixtures/v2/$n.json --set package_name=com.example.wp04.$n --out $O/final-$n --no-zip --force
done
(cd $O/final-everything && $L gradle ./gradlew :app:assembleDevBenchmarkRelease :benchmark:assemble --console=plain)
for n in standard lean; do (cd $O/final-$n && $L gradle ./gradlew :app:assembleDevRelease --console=plain); done
cd $O/final-everything
$L adb tools/perf/perf.sh --package com.example.wp04.everything.dev --variant devBenchmarkRelease --runs 3 --out $P/final --gate solid \
  tabs tabs_tap tab1 pushpop pushpop_gesture back_during_push rapid_back scroll_root idle nav_stress restore meminfo \
  scroll_minimize recompose coldstart coldstart_installed
# standard and lean do not ship tools/perf; ABI splits mean --variant picks the phone's APK.
(cd $O/final-standard && $L adb $W/templates/android/tools/perf/perf.sh --package com.example.wp04.standard.dev --variant devRelease \
  --runs 3 --out $P/final-standard --gate standard tabs pushpop scroll_root idle coldstart meminfo coldstart_installed)
(cd $O/final-lean && $L adb $W/templates/android/tools/perf/perf.sh --package com.example.wp04.lean.dev --variant devRelease \
  --runs 3 --out $P/final-lean --gate lean tabs pushpop idle coldstart coldstart_installed)
# Task name to confirm once with `./gradlew :benchmark:tasks --all | grep -i connected` before the first run.
$L adb $L gradle ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.wp04.everything.benchmark.rapidback.RapidBackBenchmark --console=plain
# Generated rules, not only the wildcard safety net: the shell's own classes are named.
test "$(grep -c '^HSPLcom/example/wp04/everything/core/navigation/AppShellKt;' core/navigation/src/main/baseline-prof.txt)" -gt 0
echo ACCEPT OK

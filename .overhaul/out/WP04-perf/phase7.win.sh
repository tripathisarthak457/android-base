#!/usr/bin/env bash
# The content-layer fix (§2b) and the cold-start and memory questions, after the single SlideRecordingTest run:
#  1. under one Gradle hold: the test against the tree without the fix's layer (it must fail), the navigation tests
#     with it, then builds of `layer` (the worktree) and `base` (the old template, generator 6bc38e9, Oct 9's spec);
#  2. under one adb hold: cardclip (55e144d's tree) against layer on every motion scenario, three runs each; a push/pop
#     capture of each; cold starts of base and layer interleaved; the memory journey on layer.
#   ./phase7.win.sh > logs/phase7-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; W=$OV/wt/WP04-perf; L=$OV/bin/locked
until grep -qE "BUILD SUCCESSFUL|BUILD FAILED" $O/logs/test-slide-recording.log 2>/dev/null; do sleep 20; done
grep -q "BUILD SUCCESSFUL" $O/logs/test-slide-recording.log || { echo "SlideRecordingTest failed with the fix"; exit 1; }
echo "== $(date '+%F %H:%M:%S') phase 7"
$L gradle bash -c '
set -uo pipefail
O='"$O"'; W='"$W"'
cd $W/templates/android
f=core/navigation/src/main/kotlin/com/base/app/core/navigation/stack/StackLayout.kt
cp $f $O/StackLayout.kt.keep
trap "cp $O/StackLayout.kt.keep $f" EXIT
python3 - $f <<PY
import sys
p = sys.argv[1]
s = open(p, encoding="utf-8").read()
fixed = "            if (divided) drawPaneDivider(divider)\n        }\n        .graphicsLayer()\n"
assert s.count(fixed) == 1, "the layer line is not where the mutation expects it"
open(p, "w", encoding="utf-8", newline="\n").write(s.replace(fixed, "            if (divided) drawPaneDivider(divider)\n        }\n"))
PY
./gradlew :core:navigation:testDebugUnitTest --tests "com.base.app.core.navigation.stack.SlideRecordingTest" --console=plain > $O/logs/test-slide-mutation.log 2>&1
rc=$?
cp $O/StackLayout.kt.keep $f
trap - EXIT
if [ $rc = 0 ]; then echo "MUTATION NOT CAUGHT: the test passes without the layer"; else echo "mutation caught (exit $rc)"; fi
git -C $W diff --quiet -- $f && echo "StackLayout.kt: no change against HEAD (unexpected)" || echo "StackLayout.kt restored with the fix"
./gradlew :core:navigation:testDebugUnitTest :core:designsystem:compileDebugKotlin --console=plain > $O/logs/tests-nav-layer.log 2>&1 \
  && echo "navigation tests OK" || { echo "navigation tests FAILED"; exit 1; }
python3 $(dirname $O)/WP04/rmtree-long.py $O/layer
(cd $W/generator && python3 create_project.py --spec tests/fixtures/v2/everything.json \
  --set package_name=com.example.wp04.everything --out $O/layer --no-zip --force > $O/logs/gen-layer.log)
git -C $W rev-parse --short HEAD > $O/layer/BUILT_FROM
git -C $W status --short -- templates > $O/layer/BUILT_DIRTY
(cd $O/layer && ./gradlew :app:assembleDevBenchmarkRelease --console=plain > $O/logs/build-layer.log 2>&1) && echo "BUILT layer" || { echo "layer did not build"; exit 1; }
(cd $O/base && ./gradlew :app:assembleDevBenchmarkRelease --console=plain > $O/logs/build-base.log 2>&1) && echo "BUILT base" || echo "base did not build"
'
rc=$?
[ $rc = 0 ] || { echo "== gradle part exit $rc"; exit 1; }
echo "== $(date '+%F %H:%M:%S') builds done"
$L adb bash -c "
TAG=-p7 SCENARIOS='tabs tabs_tap tab1 pushpop pushpop_gesture rapid_back scroll_root scroll_minimize' $O/measure.win.sh cardclip layer
$O/nav-traces.win.sh layer cardclip
if [ -f $O/base/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk ]; then
  $O/coldstart-ab.win.sh base-vs-layer 15 base=com.example.wp04.base.dev layer=com.example.wp04.everything.dev
fi
$O/mem-ab.win.sh $P/layer-mem
"
echo "== $(date '+%F %H:%M:%S') PHASE7 DONE"

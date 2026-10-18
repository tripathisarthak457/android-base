#!/usr/bin/env bash
# SlideRecordingTest under one Gradle hold: with the content layer (it must pass), then with sceneDraw's layer line
# removed (it must fail), the file restored either way.   ./test-slide.win.sh > logs/test-slide2.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; W=$OV/wt/WP04-perf; L=$OV/bin/locked
$L gradle bash -c '
set -uo pipefail
O='"$O"'; W='"$W"'
cd $W/templates/android
rel=core/navigation/src/main/kotlin/com/base/app/core/navigation/stack/StackLayout.kt
test="com.base.app.core.navigation.stack.SlideRecordingTest"
./gradlew :core:navigation:testDebugUnitTest --tests "$test" --console=plain > $O/logs/test-slide-fixed.log 2>&1
echo "with the layer: exit $?"
cp $rel $O/StackLayout.kt.keep
trap "cp $O/StackLayout.kt.keep $rel" EXIT
python3 - $rel <<PY
import sys
p = sys.argv[1]
s = open(p, encoding="utf-8").read()
fixed = "            if (divided) drawPaneDivider(divider)\n        }\n        .graphicsLayer()\n"
assert s.count(fixed) == 1, "the layer line is not where the mutation expects it"
open(p, "w", encoding="utf-8", newline="\n").write(s.replace(fixed, "            if (divided) drawPaneDivider(divider)\n        }\n"))
print("layer line removed")
PY
./gradlew :core:navigation:testDebugUnitTest --tests "$test" --console=plain > $O/logs/test-slide-mutation2.log 2>&1
echo "without the layer: exit $?"
cp $O/StackLayout.kt.keep $rel
trap - EXIT
grep -c "^        .graphicsLayer()$" $rel | sed "s/^/layer lines after restore: /"
'
echo TEST-SLIDE DONE

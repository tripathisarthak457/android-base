#!/usr/bin/env bash
# Fix run 3: RedrawDuringFramesTest against each way the node could regress, one at a time (redraw on
# every pre-draw pass, redraw on a timer, keep listening after it leaves), the code restored from git
# after each:
#   bin/locked gradle bash out/HUD/f3_mutation.win.sh
set -uo pipefail
W=D:/projects/tools/.overhaul/wt/HUD
F=$W/templates/android/core/perfhud/src/main/kotlin/com/base/app/core/perfhud/overlay/RedrawDuringFrames.kt
R=$W/templates/android/core/perfhud/build/test-results/testDebugUnitTest/TEST-com.base.app.core.perfhud.overlay.RedrawDuringFramesTest.xml
cd $W/templates/android
for mutant in every-pre-draw timer keeps-listening; do
  python - "$F" "$mutant" <<'PY'
import sys
p, mutant = sys.argv[1], sys.argv[2]
s = open(p, encoding="utf-8").read()
if mutant == "every-pre-draw":
    s = s.replace("if (now - lastRedrawNanos >= periodMillis * NanosPerMilli) {", "if (now != 0L) {")
elif mutant == "timer":
    s = s.replace("        view = requireView().also { it.viewTreeObserver.addOnPreDrawListener(this) }\n",
                  "        view = requireView().also { it.viewTreeObserver.addOnPreDrawListener(this) }\n"
                  "        coroutineScope.launch { while (true) { kotlinx.coroutines.delay(periodMillis); invalidateDraw() } }\n")
    s = s.replace("import android.view.ViewTreeObserver\n", "import android.view.ViewTreeObserver\nimport kotlinx.coroutines.launch\n")
else:
    s = s.replace("        view?.viewTreeObserver?.removeOnPreDrawListener(this)\n", "")
open(p, "w", encoding="utf-8", newline="\n").write(s)
PY
  echo "=== mutant $mutant: $(git -C $W diff --shortstat -- "$F")"
  timeout --kill-after=2m 20m ./gradlew -Pandroid.builder.sdkDownload=false --console=plain :core:perfhud:testDebugUnitTest --tests '*RedrawDuringFramesTest' > /dev/null 2>&1
  echo "exit $?"
  python -c "
import re, sys
s = open(sys.argv[1], encoding='utf-8').read()
for m in re.finditer(r'<testcase name=\"(.*?)\"[^>]*?(/>|>\s*<failure)', s): print('  ', 'FAIL' if 'failure' in m.group(2) else 'pass', m.group(1))
" "$R"
  git -C $W checkout -- "$F"
done
git -C $W diff --stat -- "$F"
echo RESTORED

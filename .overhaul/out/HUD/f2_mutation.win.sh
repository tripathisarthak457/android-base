#!/usr/bin/env bash
# Fix run 2: the new tests fail against the old behaviour. Puts the two blocking bugs back (pooled
# main-thread percentiles over the newest frame's interval; the GC baseline moved only on written
# windows), runs the tests that guard them, expects failures, and restores the files from git:
#   bin/locked gradle bash out/HUD/f2_mutation.win.sh
set -uo pipefail
W=D:/projects/tools/.overhaul/wt/HUD
K=templates/android/core/perfhud/src/main/kotlin/com/base/app/core/perfhud
cd $W
python - <<'PY'
k = "templates/android/core/perfhud/src/main/kotlin/com/base/app/core/perfhud/"
def edit(path, old, new):
    text = open(k + path, encoding="utf-8", newline="").read()
    assert text.count(old) == 1, old
    open(k + path, "w", encoding="utf-8", newline="").write(text.replace(old, new))
edit("frame/FrameLog.kt", "uiP90BudgetShare = uiShareHistogram.percentile(P90),",
     "uiP90BudgetShare = uiHistogram.percentile(P90) / intervalMillis[slot(written - 1)],")
edit("frame/FrameLog.kt", "uiP99BudgetShare = uiShareHistogram.percentile(P99),",
     "uiP99BudgetShare = uiHistogram.percentile(P99) / intervalMillis[slot(written - 1)],")
edit("frame/FrameLog.kt", "MILLIS_PER_SECOND / shortestIntervalMillis,", "MILLIS_PER_SECOND / intervalMillis[slot(written - 1)],")
edit("log/PerfLogcat.kt", "if (current.frames > previous.frames) write(windowLine(memoryNow, screen))",
     "if (current.frames > previous.frames) {\n                write(windowLine(memoryNow, screen))\n                if (memoryNow != null) gcBefore = memoryNow.gcCount\n            }")
edit("log/PerfLogcat.kt", "            if (memoryNow != null) gcBefore = memoryNow.gcCount\n        }", "        }")
PY
cd templates/android
timeout --kill-after=2m 20m ./gradlew -Pandroid.builder.sdkDownload=false --console=plain :core:perfhud:testDebugUnitTest \
  --tests '*OptimisationScoreTest' --tests '*FrameLogTest' --tests '*PerfLogcatTest'
echo "MUTANT EXIT $?"
cd $W && git checkout -- $K/frame/FrameLog.kt $K/log/PerfLogcat.kt && git status --short
echo RESTORED

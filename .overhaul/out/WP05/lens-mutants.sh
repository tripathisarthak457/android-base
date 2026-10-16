#!/usr/bin/env bash
# Runs the lens tests against two mutants of the committed LiquidLens.kt inside one Gradle lock hold,
# restoring the file from HEAD after each and on any exit. Call it through `bin/locked gradle`.
set -u
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
WT=$OV/wt/WP05
A=$WT/templates/android
REL=templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass/LiquidLens.kt
SRC=$WT/$REL
RES=$A/core/designsystem/build/test-results/testDebugUnitTest
LOG=$OV/logs/lens
restore() { git -C "$WT" checkout -- "$REL"; }
trap restore EXIT
git -C "$WT" diff --quiet -- "$REL" || { echo "LiquidLens.kt has uncommitted changes; refusing" >&2; exit 2; }

run() {
  name=$1
  rm -f "$RES"/TEST-*LiquidLens*.xml
  (cd "$A" && ./gradlew :core:designsystem:testDebugUnitTest --tests '*LiquidLens*' --console=plain) \
    > "$LOG/run7-$name.log" 2>&1
  echo "EXIT $?" >> "$LOG/run7-$name.log"
  python3 - "$RES" >> "$LOG/run7-$name.log" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
for f in sorted(glob.glob(sys.argv[1] + "/TEST-*LiquidLens*.xml")):
    for case in ET.parse(f).getroot().iter("testcase"):
        fail = case.find("failure")
        status = "PASS" if fail is None else "FAIL"
        detail = "" if fail is None else " :: " + (fail.get("message") or "")[:160]
        print(f"{status} {case.get('classname').rsplit('.', 1)[-1]}: {case.get('name')}{detail}")
PY
}

# A: the user's report. The lens shows only under a finger, so a tap moves an invisible lens.
sed -i 's/alpha = if (state.samplesBackdrop) 1f else 0f/alpha = if (state.pressed) 1f else 0f/' "$SRC"
grep -c 'alpha = if (state.pressed) 1f else 0f' "$SRC" > "$LOG/run7-mutantA.applied"
run mutantA
restore

# B: the resting tint fades only for a press, so a tap's lens stays an opaque indicator.
sed -i 's/alpha = 1f - state.intensity)/alpha = 1f - state.pressure)/' "$SRC"
grep -c 'alpha = 1f - state.pressure)' "$SRC" > "$LOG/run7-mutantB.applied"
run mutantB
restore

echo DONE > "$LOG/run7-mutants.done"

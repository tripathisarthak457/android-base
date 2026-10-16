#!/usr/bin/env bash
# Runs the lens tests against one mutant of the committed LiquidLens.kt per behaviour the run 8 review asked to
# guard, inside one Gradle lock hold, restoring the file from HEAD after each and on any exit.
# Call it through `bin/locked gradle`.
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

# mutate NAME OLD NEW: replaces the one occurrence of OLD with NEW, or records that it could not.
mutate() {
  python3 - "$SRC" "$2" "$3" > "$LOG/run8-mutant$1.applied" <<'PY'
import sys
path, old, new = sys.argv[1:]
text = open(path).read()
print(text.count(old))
if text.count(old) == 1:
    open(path, "w").write(text.replace(old, new))
PY
}

run() {
  name=$1
  rm -f "$RES"/TEST-*LiquidLens*.xml
  (cd "$A" && ./gradlew :core:designsystem:testDebugUnitTest --tests '*LiquidLens*' --console=plain) \
    > "$LOG/run8-mutant$name.log" 2>&1
  echo "EXIT $?" >> "$LOG/run8-mutant$name.log"
  python3 - "$RES" >> "$LOG/run8-mutant$name.log" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
for f in sorted(glob.glob(sys.argv[1] + "/TEST-*LiquidLens*.xml")):
    for case in ET.parse(f).getroot().iter("testcase"):
        fail = case.find("failure")
        status = "PASS" if fail is None else "FAIL"
        detail = "" if fail is None else " :: " + (fail.get("message") or "")[:160]
        print(f"{status} {case.get('classname').rsplit('.', 1)[-1]}: {case.get('name')}{detail}")
PY
  restore
}

# A: travel read off the speed alone, as before the review: a reversed selection blinks.
mutate A '            val distance = target - position
            val motion = sqrt(velocity * velocity + spec.stiffness * distance * distance)' \
  '            val motion = abs(velocity)'
run A

# B: the layer neither swells nor stretches.
mutate B '            scaleX = factor.scaleX
            scaleY = factor.scaleY' '            check(factor.scaleX > 0f)'
run B

# C: the sample is drawn unmagnified.
mutate C 'scale(1f + LensMagnification * state.intensity, pivot = center) { drawSample() }' 'drawSample()'
run C

# D: samplesBackdrop read straight off the motion, so every settling frame invalidates the lens.
mutate D 'val samplesBackdrop: Boolean by derivedStateOf {' 'val samplesBackdrop: Boolean get() = run {'
run D

# E: every composition re-selects, so a stale composition pulls a released lens back.
mutate E 'state.followSelection(selectedSlot)' 'state.select(selectedSlot)'
run E

# F: the frame loop ignores the animator duration scale.
mutate F 'val durationScale = currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f' \
  'val durationScale = 1f'
run F

# G: the sub-millisecond rest of each frame is dropped.
mutate G 'unspentNanos -= elapsedMillis * NanosPerMilli' 'unspentNanos = 0L'
run G

echo DONE > "$LOG/run8-mutants.done"

#!/usr/bin/env bash
# Runs the design system's tests once fresh, then the lens tests against one mutant of the committed
# LiquidLens.kt per behaviour the lens promises (with an optional first argument, a commit whose lens
# tests run first against its own sources), all inside one Gradle lock hold. The file is restored
# from HEAD after each mutant and on any exit. Call it through `bin/locked gradle`.
set -u
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
WT=$OV/wt/WP05
A=$WT/templates/android
REL=templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass/LiquidLens.kt
SRC=$WT/$REL
RES=$A/core/designsystem/build/test-results/testDebugUnitTest
LOG=$OV/logs/lens
TEST=templates/android/core/designsystem/src/test/kotlin/com/base/app/core/designsystem/foundation/glass/LiquidLensStateTest.kt
PIXEL=templates/android/core/designsystem/src/test/kotlin/com/base/app/core/designsystem/foundation/glass/LiquidLensTest.kt
restore() { git -C "$WT" checkout -- "$REL" "$TEST" "$PIXEL"; }
trap restore EXIT
git -C "$WT" diff --quiet -- "$REL" "$TEST" "$PIXEL" || { echo "the lens or its tests have uncommitted changes; refusing" >&2; exit 2; }

summarise() {
  python3 - "$RES" "$1" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
for f in sorted(glob.glob(sys.argv[1] + "/TEST-*" + sys.argv[2] + "*.xml")):
    for case in ET.parse(f).getroot().iter("testcase"):
        fail = case.find("failure")
        status = "PASS" if fail is None else "FAIL"
        detail = "" if fail is None else " :: " + (fail.get("message") or "")[:160]
        print(f"{status} {case.get('classname').rsplit('.', 1)[-1]}: {case.get('name')}{detail}")
PY
}

# The first commit of run 10 adds tests only; check its lens tests against its own LiquidLens.kt.
if [ -n "${1:-}" ]; then
  for f in "$REL" "$TEST" "$PIXEL"; do git -C "$WT" show "$1:$f" > "$WT/$f"; done
  rm -f "$RES"/TEST-*LiquidLens*.xml
  (cd "$A" && ./gradlew :core:designsystem:testDebugUnitTest --tests '*LiquidLens*' --console=plain) \
    > "$LOG/run10-first-commit.log" 2>&1
  echo "EXIT $?" >> "$LOG/run10-first-commit.log"
  summarise LiquidLens >> "$LOG/run10-first-commit.log"
  restore
fi

# The whole module, executed rather than restored from the build cache.
rm -rf "$RES"
(cd "$A" && ./gradlew :core:designsystem:testDebugUnitTest --rerun --console=plain) > "$LOG/run10-test2.log" 2>&1
echo "EXIT $?" >> "$LOG/run10-test2.log"
summarise "" >> "$LOG/run10-test2.log"
rm -rf "$LOG/run10-results" && cp -r "$RES" "$LOG/run10-results"

# mutate NAME OLD NEW: replaces the one occurrence of OLD with NEW, or records that it could not.
mutate() {
  python3 - "$SRC" "$2" "$3" > "$LOG/run10-mutant$1.applied" <<'PY'
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
    > "$LOG/run10-mutant$name.log" 2>&1
  echo "EXIT $?" >> "$LOG/run10-mutant$name.log"
  summarise LiquidLens >> "$LOG/run10-mutant$name.log"
  restore
}

# U, the user's report: the lens shows only under a finger, so a tap moves an invisible lens.
mutate U '        if (state.samplesBackdrop) {
            val factor' '        if (state.pressed) {
            val factor'
run U

# T: the resting tint fades only for a press, so a tap carries an opaque indicator.
mutate T 'val glass = if (state.samplesBackdrop) state.intensity else 0f' \
  'val glass = if (state.samplesBackdrop) state.pressure else 0f'
run T

# A: travel read off the speed alone: a selection reversed in flight blinks at its turnaround.
mutate A '            val distance = target - position
            return sqrt(velocity * velocity + spec.stiffness * distance * distance)' \
  '            return abs(velocity)'
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

# H: travel ignores the tier, so LiveNoLens and below refract on a tap.
mutate H 'get() = if (refracts) glassOf(energy) * eased(departure / LiftDistance) else 0f' \
  'get() = glassOf(energy) * eased(departure / LiftDistance)'
run H

# I: Reduce motion is ignored. The value is still read, so the mutant compiles without a new warning.
mutate I 'LocalGlassQuality.current.refracts && !reduceMotion' \
  'LocalGlassQuality.current.refracts && (reduceMotion || !reduceMotion)'
run I

# J (the run-10 review's blocking finding): a press no longer swells the lens.
mutate J 'val swell = 1f + max(PressSwell * pressure, TravelSwell * travel)' \
  'val swell = 1f + TravelSwell * travel'
run J

# K: the layer stretches every lens along a bar, so a rail's lens stretches across the rail.
mutate K 'val factor = state.scale(orientation)' 'val factor = state.scale(Orientation.Horizontal)'
run K

# L: run 9's lift: full glass on the first moving frame.
mutate L 'get() = if (refracts) glassOf(energy) * eased(departure / LiftDistance) else 0f' \
  'get() = if (refracts && velocity != 0f) glassOf(energy) else 0f'
run L

# M: the lift is never restarted, so a tap made while the lens still settles pops to full glass.
mutate M '        if (glassOf(energy) < ShownThreshold) {
            departureOrigin = position
            departure = 0f
        } else if (departure < LiftDistance) {' '        if (departure < LiftDistance) {'
run M

# N: a jump with animations off keeps the lift of the flight it cut short.
mutate N '        pressureVelocity = 0f
        departureOrigin = position
        departure = 0f
    }' '        pressureVelocity = 0f
    }'
run N

echo DONE > "$LOG/run10-mutants.done"

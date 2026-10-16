#!/usr/bin/env bash
# Runs the design system's tests once fresh, then the lens tests against one mutant of the committed
# LiquidLens.kt per behaviour the lens promises (optional arguments below run first), all inside one
# Gradle lock hold. The files are restored from HEAD after each run and on any exit. Call it through
# `bin/locked gradle`.
set -u
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
WT=$OV/wt/WP05
A=$WT/templates/android
GLASS=templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass
REL=$GLASS/LiquidLens.kt
STATE_REL=$GLASS/LiquidLensState.kt
SRC=$WT/$REL
STATE=$WT/$STATE_REL
RES=$A/core/designsystem/build/test-results/testDebugUnitTest
LOG=$OV/logs/lens
TEST=templates/android/core/designsystem/src/test/kotlin/com/base/app/core/designsystem/foundation/glass/LiquidLensStateTest.kt
PIXEL=templates/android/core/designsystem/src/test/kotlin/com/base/app/core/designsystem/foundation/glass/LiquidLensTest.kt
restore() { git -C "$WT" checkout -- "$REL" "$STATE_REL" "$TEST" "$PIXEL"; }
git -C "$WT" diff --quiet HEAD -- "$REL" "$STATE_REL" "$TEST" "$PIXEL" || { echo "the lens or its tests have uncommitted changes; refusing" >&2; exit 2; }
trap restore EXIT

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

lens_tests() {
  rm -f "$RES"/TEST-*LiquidLens*.xml
  (cd "$A" && ./gradlew :core:designsystem:testDebugUnitTest --tests '*LiquidLens*' --console=plain) > "$1" 2>&1
  echo "EXIT $?" >> "$1"
  summarise LiquidLens >> "$1"
}

# $1: an older commit, from before the state had a file of its own, whose LiquidLens.kt the committed
# tests run against (run 10's lift). $2: such a commit whose lens tests run against its own sources.
if [ -n "${1:-}" ]; then
  git -C "$WT" show "$1:$REL" > "$SRC"
  echo "package com.base.app.core.designsystem.foundation.glass" > "$STATE"
  lens_tests "$LOG/run12-old-lens.log"
  restore
fi
if [ -n "${2:-}" ]; then
  for f in "$REL" "$TEST" "$PIXEL"; do git -C "$WT" show "$2:$f" > "$WT/$f"; done
  echo "package com.base.app.core.designsystem.foundation.glass" > "$STATE"
  lens_tests "$LOG/run12-first-commit.log"
  restore
fi

# The whole module, executed rather than restored from the build cache.
rm -rf "$RES"
(cd "$A" && ./gradlew :core:designsystem:testDebugUnitTest --rerun --console=plain) > "$LOG/run12-test2.log" 2>&1
echo "EXIT $?" >> "$LOG/run12-test2.log"
summarise "" >> "$LOG/run12-test2.log"
rm -rf "$LOG/run12-results" && cp -r "$RES" "$LOG/run12-results"

# mutate NAME OLD NEW: replaces the one occurrence of OLD with NEW in the lens modifier's file
# (U, T, B, C, K) or the state's (the rest), or records that it could not.
mutate() {
  case "$1" in U|T|B|C|K) file=$SRC ;; *) file=$STATE ;; esac
  python3 - "$file" "$2" "$3" > "$LOG/run12-mutant$1.applied" <<'PY'
import sys
path, old, new = sys.argv[1:]
text = open(path).read()
print(text.count(old))
if text.count(old) == 1:
    open(path, "w").write(text.replace(old, new))
PY
}

run() {
  lens_tests "$LOG/run12-mutant$1.log"
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
mutate D 'get() = (pressed && refracts) || glassShown' \
  'get() = (pressed && refracts) || intensity >= ShownThreshold'
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

# H: travel ignores the tier between frames, so a tier drop leaves a frame of glass.
mutate H 'get() = if (refracts) min(glassOf(energy), lift) else 0f' 'get() = min(glassOf(energy), lift)'
run H

# R: the lift is kept while the lens does not refract, so glass returning mid-flight pops up.
mutate R 'lift = if (refracts) min(glassOf(energy), lift + rise) else 0f' \
  'lift = min(glassOf(energy), lift + rise)'
run R

# I: Reduce motion is ignored. The value is still read, so the mutant compiles without a new warning.
mutate I 'LocalGlassQuality.current.refracts && !reduceMotion' \
  'LocalGlassQuality.current.refracts && (reduceMotion || !reduceMotion)'
run I

# J: a press no longer swells the lens.
mutate J 'val swell = 1f + max(PressSwell * pressure, TravelSwell * travel)' \
  'val swell = 1f + TravelSwell * travel'
run J

# K: the layer stretches every lens along a bar, so a rail's lens stretches across the rail.
mutate K 'val factor = state.scale(orientation)' 'val factor = state.scale(Orientation.Horizontal)'
run K

# L: run 9's lift: full glass on the first moving frame.
mutate L 'get() = if (refracts) min(glassOf(energy), lift) else 0f' \
  'get() = if (refracts && velocity != 0f) glassOf(energy) else 0f'
run L

# M, the run-10 review's blocking finding: the lift saturates and restarts only once the spring is
# calm, so a selection made while the glass calms pops it back to full.
mutate M 'lift = if (refracts) min(glassOf(energy), lift + rise) else 0f' \
  'lift = if (refracts && glassOf(energy) >= ShownThreshold) min(1f, lift + rise) else 0f'
run M

# N: a jump with animations off keeps the cut-short flight's lift.
mutate N '        pressureVelocity = 0f
        lift = 0f
    }' '        pressureVelocity = 0f
    }'
run N

# O: no time floor, so a tap far along the bar lifts within a frame or two.
mutate O 'val rise = min(abs(position - from) / LiftDistance, elapsedNanos.toFloat() / LiftNanos)' \
  'val rise = abs(position - from) / LiftDistance'
run O

# P: the lift follows the clock alone, so the glass no longer eases in with the motion.
mutate P 'val rise = min(abs(position - from) / LiftDistance, elapsedNanos.toFloat() / LiftNanos)' \
  'val rise = min(abs(position - from) / LiftDistance + 1f, elapsedNanos.toFloat() / LiftNanos)'
run P

echo DONE > "$LOG/run12-mutants.done"

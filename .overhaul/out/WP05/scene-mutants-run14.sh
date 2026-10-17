#!/usr/bin/env bash
# Run 14 (Windows): the design system's tests once fresh, then GlassStageTest against the scene as it was
# before run 14 and against one mutant per promise the new scene makes, all inside one Gradle lock hold.
# SceneBackdrop.kt is restored from HEAD after each run and on any exit. Call it through
# `bin/locked gradle bash`.
set -u
OV=D:/projects/tools/.overhaul
WT=$OV/wt/WP05
A=$WT/templates/android
REL=templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass/SceneBackdrop.kt
SRC=$WT/$REL
RES=$A/core/designsystem/build/test-results/testDebugUnitTest
LOG=$OV/logs/lens
BEFORE=5d515a06
NO_SDK_DOWNLOAD=-Pandroid.builder.sdkDownload=false
restore() { git -C "$WT" checkout -- "$REL"; }
git -C "$WT" diff --quiet HEAD -- "$REL" || { echo "SceneBackdrop.kt has uncommitted changes; refusing" >&2; exit 2; }
trap restore EXIT

summarise() {
  python -I - "$RES" "$1" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
for f in sorted(glob.glob(sys.argv[1] + "/TEST-*" + sys.argv[2] + "*.xml")):
    for case in ET.parse(f).getroot().iter("testcase"):
        fail = case.find("failure")
        status = "PASS" if fail is None else "FAIL"
        detail = "" if fail is None else " :: " + (fail.get("message") or "")[:160].replace("\n", " ")
        print(f"{status} {case.get('classname').rsplit('.', 1)[-1]}: {case.get('name')}{detail}")
PY
}

stage_tests() {
  rm -f "$RES"/TEST-*GlassStageTest*.xml
  (cd "$A" && ./gradlew $NO_SDK_DOWNLOAD :core:designsystem:testDebugUnitTest --tests '*GlassStageTest' --console=plain) > "$1" 2>&1
  echo "EXIT $?" >> "$1"
  summarise GlassStageTest >> "$1"
}

# The whole module, executed rather than restored from the build cache.
rm -rf "$RES"
(cd "$A" && ./gradlew $NO_SDK_DOWNLOAD :core:designsystem:testDebugUnitTest --rerun --console=plain) > "$LOG/run14-test2.log" 2>&1
echo "EXIT $?" >> "$LOG/run14-test2.log"
summarise "" >> "$LOG/run14-test2.log"
rm -rf "$LOG/run14-results" && cp -r "$RES" "$LOG/run14-results"

# The scene before run 14, which tracked its position in the root through onGloballyPositioned.
git -C "$WT" show "$BEFORE:$REL" > "$SRC"
stage_tests "$LOG/run14-old-scene.log"
restore

# mutate NAME OLD NEW: replaces the one occurrence of OLD with NEW in SceneBackdrop.kt, or records that
# it could not.
mutate() {
  python -I - "$SRC" "$2" "$3" > "$LOG/run14-mutant$1.applied" <<'PY'
import sys
path, old, new = sys.argv[1:]
text = open(path, encoding="utf-8", newline="").read()
print(text.count(old))
if text.count(old) == 1:
    open(path, "w", encoding="utf-8", newline="").write(text.replace(old, new))
PY
}

# P: the recorder a plain field, so a sample drawn before the scene was recorded is never redrawn.
mutate P 'private var recorder by mutableStateOf<Modifier.Node?>(null)' 'private var recorder: Modifier.Node? = null'
stage_tests "$LOG/run14-mutantP.log"
restore

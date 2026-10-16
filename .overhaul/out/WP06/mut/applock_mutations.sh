#!/usr/bin/env bash
# Runs AppLockGateTest against the committed gate and against each mutation, restoring the gate after each.
# Run under the Gradle lock: bin/locked gradle bash applock_mutations.sh <mutation ids...>
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
T=/home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP06/templates/android
G=core/applock/src/main/kotlin/com/base/app/core/applock/AppLockGate.kt
cd "$T"
restore() { git checkout -- "$G"; }
trap restore EXIT
mutate() {
  case "$1" in
    none) ;;
    no-onexit) python3 - "$G" <<'PY'
import sys; p=sys.argv[1]; s=open(p).read()
line="            .focusProperties { onExit = { cancelFocusChange() } }\n"
assert line in s; open(p,'w').write(s.replace(line,""))
PY
    ;;
    no-onenter) python3 - "$G" <<'PY'
import sys; p=sys.argv[1]; s=open(p).read()
line="                .focusProperties { onEnter = { cancelFocusChange() } }\n"
assert line in s; open(p,'w').write(s.replace(line,""))
PY
    ;;
    *) echo "unknown mutation $1"; exit 2;;
  esac
}
for m in "$@"; do
  restore
  echo "=== mutation: $m"
  mutate "$m"
  rm -rf core/applock/build/test-results/testDebugUnitTest
  ./gradlew :core:applock:testDebugUnitTest --offline --console=plain -q > /dev/null 2>&1
  python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
for f in glob.glob('core/applock/build/test-results/testDebugUnitTest/*AppLockGateTest.xml'):
    for tc in ET.parse(f).getroot().iter('testcase'):
        fail = tc.find('failure')
        print('  %s %s | %s' % ('FAIL' if fail is not None else 'pass', tc.get('name'),
              (fail.get('message') or '')[:160].replace('\n',' ') if fail is not None else ''))
PY
done
restore
git status --short "$G"

#!/usr/bin/env bash
# applock_mutations.sh on the Windows machine: AppLockGateTest against the committed gate and against each mutation,
# restoring the gate after each. Run under the Gradle lock: $L gradle bash applock_mutations.win.sh <mutation ids...>
set -uo pipefail
O=D:/projects/tools/.overhaul/out/WP06
T=D:/projects/tools/.overhaul/wt/WP06/templates/android
G=core/applock/src/main/kotlin/com/base/app/core/applock/AppLockGate.kt
cd "$T"
restore() { git checkout -- "$G"; }
trap restore EXIT
drop() {
  python - "$G" "$1" <<'PY'
import sys
p, line = sys.argv[1], sys.argv[2] + "\n"
s = open(p, encoding="utf-8", newline="").read()
assert s.count(line) == 1, line
open(p, "w", encoding="utf-8", newline="").write(s.replace(line, ""))
PY
}
mutate() {
  case "$1" in
    none) ;;
    no-onexit) drop "            .focusProperties { onExit = { cancelFocusChange() } }" ;;
    no-onenter) drop "                .focusProperties { onEnter = { cancelFocusChange() } }" ;;
    no-pointer) drop "            .pointerInput(Unit) {}" ;;
    no-semantics) drop "                .clearAndSetSemantics {}" ;;
    no-focus) drop "    SideEffect(focus) { focus.requestFocus() }" ;;
    *) echo "unknown mutation $1"; exit 2;;
  esac
}
for m in "$@"; do
  restore
  echo "=== mutation: $m"
  mutate "$m"
  rm -rf core/applock/build/test-results/testDebugUnitTest
  timeout 10m ./gradlew -Pandroid.builder.sdkDownload=false -I $O/test-timeout.init.gradle \
    :core:applock:testDebugUnitTest --tests '*AppLockGateTest' --offline --console=plain -q > /dev/null 2>&1
  python - <<'PY'
import glob, xml.etree.ElementTree as ET
files = glob.glob('core/applock/build/test-results/testDebugUnitTest/*AppLockGateTest.xml')
if not files:
    print('  no results (compile error or timeout)')
for f in files:
    for tc in ET.parse(f).getroot().iter('testcase'):
        fail = tc.find('failure')
        print('  %s %s | %s' % ('FAIL' if fail is not None else 'pass', tc.get('name'),
              (fail.get('message') or '')[:160].replace('\n', ' ') if fail is not None else ''))
PY
done
restore
git status --short "$G"

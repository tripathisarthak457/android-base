#!/usr/bin/env bash
# Diagnosis 3: does the release velocity decide the side? Throws that start outside the back-gesture zone.
set -uo pipefail
S=emulator-5554; A="adb -s $S"; O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP06; D=$O/diag
PKG=com.example.wp06.everything.dev
$A install -r -d $O/everything/app/build/outputs/apk/dev/release/app-dev-x86_64-release.apk >/dev/null
bounds() { $A shell uiautomator dump /sdcard/d.xml >/dev/null; $A pull /sdcard/d.xml $D/d.xml >/dev/null; python3 -c '
import re,xml.etree.ElementTree as E
for n in E.parse("'$D'/d.xml").iter("node"):
    if n.get("resource-id","").endswith("devtools_badge"): print(n.get("bounds"))'; }
launch() { $A shell pm clear $PKG >/dev/null; comp=$($A shell cmd package resolve-activity --brief $PKG | tail -1 | tr -d '\r'); $A shell am start -W -n $comp >/dev/null; sleep 4; }
launch; echo "A start: $(bounds)"
$A shell input swipe 975 1403 675 1403 120; sleep 2; echo "A fast left throw 300 px / 120 ms from x=975: $(bounds)"
launch; echo "B start: $(bounds)"
$A shell input swipe 975 1403 675 1403 1500; sleep 2; echo "B slow left drag 300 px / 1500 ms: $(bounds)"
launch; echo "C start: $(bounds)"
$A shell input swipe 975 1403 475 603 600; sleep 2; echo "C diagonal (-500,-800) / 600 ms from x=975: $(bounds)"
launch; echo "D start: $(bounds)"
$A shell input swipe 1017 1403 517 603 600; sleep 2; echo "D diagonal from x=1017 (edge zone): $(bounds)"
$A logcat -d | grep -E "startBackNavigation" | tail -3 | cut -c1-80

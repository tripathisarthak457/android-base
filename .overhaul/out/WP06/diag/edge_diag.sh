#!/usr/bin/env bash
# Diagnosis: does a drag starting inside the right-edge back-gesture zone reach the badge, and one starting
# further in? Run inside with_emulator.sh.
set -uo pipefail
S=emulator-5554; A="adb -s $S"; O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP06; D=$O/diag
PKG=com.example.wp06.everything.dev
$A shell cmd overlay list | grep -i navbar
$A shell settings get secure back_gesture_inset_scale_right
$A shell wm density
$A install -r -d $O/everything/app/build/outputs/apk/dev/release/app-dev-x86_64-release.apk >/dev/null
$A shell pm clear $PKG >/dev/null
comp=$($A shell cmd package resolve-activity --brief $PKG | tail -1 | tr -d '\r')
$A shell am start -W -n $comp >/dev/null
sleep 3
bounds() { $A shell uiautomator dump /sdcard/d.xml >/dev/null; $A pull /sdcard/d.xml $D/d.xml >/dev/null; python3 -c '
import re,xml.etree.ElementTree as E
for n in E.parse("'$D'/d.xml").iter("node"):
    if n.get("resource-id","").endswith("devtools_badge"): print(n.get("bounds"))'; }
echo "start: $(bounds)"
$A logcat -c
$A shell input swipe 1017 1403 517 603 600; sleep 1.5
echo "after swipe from x=1017: $(bounds)"; $A shell dumpsys activity activities | grep -m2 -E "topResumedActivity|mResumedActivity"
$A logcat -d | grep -i -E "back|EdgeBack" | tail -5
$A shell am start -W -n $comp >/dev/null; sleep 2
echo "relaunched: $(bounds)"
$A shell input swipe 975 1403 475 603 600; sleep 1.5
echo "after swipe from x=975: $(bounds)"
$A shell dumpsys activity activities | grep -m2 -E "topResumedActivity|mResumedActivity"

#!/usr/bin/env bash
# Diagnosis 2: reproduce the drag check right after boot, recorded with the pointer trace on, with the
# system's back-gesture log; then the same check again on the settled emulator.
set -uo pipefail
S=emulator-5554; A="adb -s $S"; O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP06; D=$O/diag
PKG=com.example.wp06.everything.dev
APK=$O/everything/app/build/outputs/apk/dev/release/app-dev-x86_64-release.apk
pl=$($A shell settings get system pointer_location | tr -d '\r')
trap '$A shell settings put system pointer_location ${pl/null/0}' EXIT
$A shell settings put system pointer_location 1
$A logcat -c
$A shell screenrecord --size 720x1600 --time-limit 25 /sdcard/diag2.mp4 &
rec=$!
sleep 1
python3 $O/badge_drag_check.py --serial $S --package $PKG --apk $APK --out $D/run1; echo "run1 rc=$?"
wait $rec
$A pull /sdcard/diag2.mp4 $D/diag2.mp4 >/dev/null
$A logcat -d > $D/logcat1.txt
grep -i -E "EdgeBack|BackNavigation|BackAnimation|onBackInvoked|OnBackPressed|back gesture|ShellBackPreview" $D/logcat1.txt | tail -20
$A logcat -c
python3 $O/badge_drag_check.py --serial $S --package $PKG --apk $APK --out $D/run2; echo "run2 rc=$?"
$A logcat -d > $D/logcat2.txt
grep -i -E "EdgeBack|BackNavigation|BackAnimation|onBackInvoked|OnBackPressed|back gesture|ShellBackPreview" $D/logcat2.txt | tail -20

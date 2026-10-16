#!/usr/bin/env bash
# One adb lock hold: the e68bbfa build and the current build of pull to refresh measured back to back
# (same phone state, same day), then the current build traced and recorded in light and dark.
# For the session the phone stays awake, in portrait, without auto-rotation; those settings and night
# mode are put back as they were, however the session ends. Stops at once if the phone is locked:
# input sent to a lock screen lands on the system UI.  $L adb ptr_r8_session.sh <serial>
set -uo pipefail
S=$1
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP03
P=/home/crazyhusir/Documents/projects/tools/.overhaul/perf/WP03/ptr
PKG=com.example.wp03.ptr.dev
APK=app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk
A="adb -s $S"
get() { $A shell settings get "$1" "$2" | tr -d '\r'; }
night=$($A shell cmd uimode night | sed 's/Night mode: //' | tr -d '\r')
auto_rotate=$(get system accelerometer_rotation)
user_rotation=$(get system user_rotation)
stay_on=$(get global stay_on_while_plugged_in)
restore() {
  $A shell cmd uimode night "$night" >/dev/null
  $A shell settings put system user_rotation "$user_rotation"
  $A shell settings put system accelerometer_rotation "$auto_rotate"
  $A shell settings put global stay_on_while_plugged_in "$stay_on"
  echo "restored: night $night, auto-rotate $auto_rotate, rotation $user_rotation, stay-on $stay_on"
}
trap restore EXIT
$A shell input keyevent KEYCODE_WAKEUP
sleep 1
if $A shell dumpsys trust | grep -q "deviceLocked=1"; then
  echo "PHONE LOCKED: nothing measured"; exit 3
fi
$A shell settings put global stay_on_while_plugged_in 7
$A shell settings put system accelerometer_rotation 0
$A shell settings put system user_rotation 0
bash "$O/scripts/ptr_measure.sh" "$S" "$PKG" "$P/r8-head" "$O/ptr0/$APK"
bash "$O/scripts/ptr_measure.sh" "$S" "$PKG" "$P/r8-after" "$O/ptr/$APK" trace
rm -rf "$P/r8-recorded"
bash "$O/scripts/ptr_record.sh" "$S" "$PKG" "$P/r8-recorded"
echo "R8 SESSION DONE"

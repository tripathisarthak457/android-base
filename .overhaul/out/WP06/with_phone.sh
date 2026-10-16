#!/usr/bin/env bash
# Runs a device command with the phone awake, in portrait and without auto-rotation, and puts those
# settings and night mode back as they were however the command ends. Refuses a locked phone: input
# sent to a lock screen lands on the system UI.  $L adb with_phone.sh <serial> <command...>
set -uo pipefail
S=$1; shift
A="adb -s $S"
get() { $A shell settings get "$1" "$2" | tr -d '\r'; }
night=$($A shell cmd uimode night | sed 's/Night mode: //' | tr -d '\r')
auto_rotate=$(get system accelerometer_rotation)
user_rotation=$(get system user_rotation)
stay_on=$(get global stay_on_while_plugged_in)
font_scale=$(get system font_scale)
restore() {
  $A shell cmd uimode night "$night" >/dev/null
  $A shell settings put system user_rotation "$user_rotation"
  $A shell settings put system accelerometer_rotation "$auto_rotate"
  $A shell settings put global stay_on_while_plugged_in "$stay_on"
  if [ "$font_scale" = null ]; then $A shell settings delete system font_scale >/dev/null; else $A shell settings put system font_scale "$font_scale"; fi
  echo "restored: night $night, auto-rotate $auto_rotate, rotation $user_rotation, stay-on $stay_on, font scale $font_scale"
}
trap restore EXIT
$A shell input keyevent KEYCODE_WAKEUP
sleep 1
if $A shell dumpsys trust | grep -q "deviceLocked=1"; then
  echo "PHONE LOCKED: nothing run"; exit 3
fi
$A shell settings put global stay_on_while_plugged_in 7
$A shell settings put system accelerometer_rotation 0
$A shell settings put system user_rotation 0
"$@"

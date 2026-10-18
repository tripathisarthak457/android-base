#!/usr/bin/env bash
# HWUI's overdraw view of the installed everything APK: the sample list, a pushed detail, and the back card held at
# about 40 % of a swipe over the list. Each pixel is tinted by how many times it was drawn (none: once; blue: twice;
# green: three times; pink: four; red: five or more). The property is read when the process starts, so the app is
# restarted with it on and again after it is put back as it was.
# Under the adb lock:  $L adb ./overdraw.win.sh <out-dir> [package]
set -uo pipefail
# Device paths (/sdcard) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"
mkdir -p "$out"
comp=$($A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | tr -d '\r' | tail -1)
orig=$($A shell getprop debug.hwui.overdraw | tr -d '\r')
echo "debug.hwui.overdraw was '$orig'"
restore() {
  $A shell "setprop debug.hwui.overdraw '$orig'"
  echo "debug.hwui.overdraw restored to '$($A shell getprop debug.hwui.overdraw | tr -d '\r')'"
  $A shell am force-stop $pkg
}
trap restore EXIT
shot() {
  $A shell screencap -p /data/local/tmp/od.png
  $A pull /data/local/tmp/od.png "$out/$1.png" > /dev/null
  $A shell rm -f /data/local/tmp/od.png
}
$A shell setprop debug.hwui.overdraw show
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell am start -W -n "$comp" --es perf.fakeSession true > /dev/null
sleep 6
size=$($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
w=${size%x*}; h=${size#*x}
shot list
: > "$out/row.txt"
for attempt in 1 2 3 4 5 6 7; do
  $A shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; $A pull /sdcard/ui.xml "$out/ui.xml" > /dev/null 2>&1
  python3 "$(dirname "$0")/findrow.py" "$out/ui.xml" > "$out/row.txt" && break
  sleep 3
done
read -r rx ry < "$out/row.txt" || { echo "OVERDRAW: no row"; exit 2; }
$A shell input tap $rx $ry; sleep 2
shot detail
# A back swipe held still: the card over the list, its strips, scrim and the page beneath as they draw mid-gesture.
y=$((h / 2))
$A shell input motionevent DOWN 3 $y
for x in $((w / 20)) $((w / 10)) $((w / 6)) $((w / 4)) $((w / 3)) $((w * 2 / 5)); do
  $A shell input motionevent MOVE $x $y
done
sleep 0.8
shot gesture
# Back to the edge before lifting, so the gesture cancels and the detail stays.
for x in $((w / 4)) $((w / 10)) 3; do $A shell input motionevent MOVE $x $y; done
$A shell input motionevent UP 3 $y
sleep 1.5
shot detail-after
$A shell input keyevent 4; sleep 1.5
ls -la "$out"
echo OVERDRAW DONE

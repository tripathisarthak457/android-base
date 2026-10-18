#!/usr/bin/env bash
# A full-resolution screenshot of the back card held at 45 % of the width (rec-gesture.win.sh's swipe, without the
# drift), to compare how text reads on the card between builds; the recording is scaled to 720 px and cannot show
# it. The screenshot goes to /data/local/tmp (shared storage would wake Google Photos).
#   $L adb ./heldshot.win.sh <out.png> [package]
set -uo pipefail
export MSYS_NO_PATHCONV=1
outpng=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"; tmp=$(dirname "$outpng")
comp=$($A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | tr -d '\r' | tail -1)
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell am start -W -n "$comp" --es perf.fakeSession true > /dev/null 2>&1
sleep 5
size=$($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
w=${size%x*}; h=${size#*x}
rx=""
for attempt in 1 2 3 4 5 6 7; do
  $A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $A pull /sdcard/ui.xml "$tmp/ui.xml" >/dev/null 2>&1
  read -r rx ry < <(python3 "$(dirname "$0")/findrow.py" "$tmp/ui.xml") && break
  sleep 3
done
[ -n "${rx:-}" ] || { echo "HELDSHOT: no row"; exit 2; }
$A shell input tap $rx $ry; sleep 1.5
y=$((h / 2)); far=$((w * 45 / 100))
moves="input motionevent DOWN 2 $y"
for i in $(seq 1 24); do moves="$moves; input motionevent MOVE $((2 + far * i / 24)) $y"; done
$A shell "$moves; sleep 0.8; screencap -p /data/local/tmp/held.png; input motionevent UP $((2 + far)) $y"
$A pull /data/local/tmp/held.png "$outpng" > /dev/null 2>&1
$A shell rm -f /data/local/tmp/held.png
echo "HELDSHOT $outpng"

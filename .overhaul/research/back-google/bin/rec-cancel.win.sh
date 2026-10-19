#!/usr/bin/env bash
# rec-gesture.win.sh's slow swipe, cancelled: the card dragged to 45 % in 24 moves, held 0.6 s, dragged back to the
# edge in 24 moves and let go, recorded at 720 px, every composed frame extracted (no resampling), so the end of the
# cancel can be checked for a flash or a step. Then Back pops the detail. Device files go to /data/local/tmp.
#   $L adb ./rec-cancel.win.sh <out-dir> [package]
set -uo pipefail
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"
mkdir -p "$out/frames"
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell am start -W -n $pkg/com.example.wp04.everything.MainActivity --es perf.fakeSession true > /dev/null 2>&1
sleep 5
size=$($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
w=${size%x*}; h=${size#*x}
rx=""
for attempt in 1 2 3 4 5 6 7; do
  $A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $A pull /sdcard/ui.xml "$out/ui.xml" >/dev/null 2>&1
  read -r rx ry < <(python3 D:/projects/tools/.overhaul/out/WP04-perf/findrow.py "$out/ui.xml") && break
  sleep 3
done
[ -n "${rx:-}" ] || { echo "REC-CANCEL: no row"; exit 2; }
$A shell input tap $rx $ry; sleep 1.5
y=$((h / 2)); far=$((w * 45 / 100))
moves="input motionevent DOWN 2 $y"
for i in $(seq 1 24); do moves="$moves; input motionevent MOVE $((2 + far * i / 24)) $y"; done
moves="$moves; sleep 0.6"
for i in $(seq 23 -1 0); do moves="$moves; input motionevent MOVE $((2 + far * i / 24)) $y"; done
moves="$moves; input motionevent UP 2 $y"
$A shell rm -f /data/local/tmp/cancel.mp4
$A shell screenrecord --size 720x1600 --time-limit 12 /data/local/tmp/cancel.mp4 &
rec=$!
sleep 1
$A shell "$moves"
wait $rec
$A pull /data/local/tmp/cancel.mp4 "$out/cancel.mp4" >/dev/null 2>&1
$A shell rm -f /data/local/tmp/cancel.mp4
$A shell input keyevent 4
ffmpeg -v error -y -i "$out/cancel.mp4" -fps_mode passthrough "$out/frames/f%04d.png"
ls "$out/frames" | wc -l
echo REC-CANCEL DONE

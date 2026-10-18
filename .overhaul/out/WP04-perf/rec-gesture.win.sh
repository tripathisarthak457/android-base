#!/usr/bin/env bash
# A screen recording of a slow back swipe on the installed everything APK: a row opened, then the card dragged to
# 45 % of the width in 24 moves, drifting down 120 px, held 1 s, and let go (a commit), so every frame of the card
# over the page beneath can be checked for gaps or seams. Frames come out as PNGs at 30 fps.
#   $L adb ./rec-gesture.win.sh <out-dir> [package]
set -uo pipefail
# Device paths (/sdcard, /data) must reach adb as typed; every local path here is a D:/ path.
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
  read -r rx ry < <(python3 "$(dirname "$0")/findrow.py" "$out/ui.xml") && break
  sleep 3
done
[ -n "${rx:-}" ] || { echo "REC-GESTURE: no row"; exit 2; }
$A shell input tap $rx $ry; sleep 1.5
y=$((h / 2)); far=$((w * 45 / 100)); drift=120
moves="input motionevent DOWN 2 $y"
for i in $(seq 1 24); do moves="$moves; input motionevent MOVE $((2 + far * i / 24)) $((y + drift * i / 24))"; done
moves="$moves; sleep 1; input motionevent UP $((2 + far)) $((y + drift))"
$A shell rm -f /data/local/tmp/gesture.mp4
$A shell screenrecord --size 720x1600 --time-limit 10 /data/local/tmp/gesture.mp4 &
rec=$!
sleep 1
$A shell "$moves"
wait $rec
$A pull /data/local/tmp/gesture.mp4 "$out/gesture.mp4" >/dev/null 2>&1
$A shell rm -f /data/local/tmp/gesture.mp4
ffmpeg -v error -y -i "$out/gesture.mp4" -vf fps=30 "$out/frames/f%03d.png"
ls "$out/frames" | wc -l
echo REC-GESTURE DONE

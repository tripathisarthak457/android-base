#!/usr/bin/env bash
# The installed ladder build's card held at 45 % (heldshot.win.sh's swipe): a full-resolution screenshot for text,
# corners and shadows, and HWUI's and the process's memory before, during and after the gesture; then a touch held
# still at the back edge (c2's prewarm takes the texture there, no other build does) and released. Writes into
# <out-dir>: held.png and gfx-*.txt / mem-*.txt (dumpsys gfxinfo / meminfo at: before, held, after1 = 1.5 s after
# the commit, after35 = 35 s after it, edgehold, edgeafter = 0.4 s after the edge touch ended). Reads only; device
# files go to /data/local/tmp. Under the adb lock, after perf.sh or back-trace.win.sh installed the build:
#   $L adb ./heldmem.win.sh <out-dir> [package]
set -uo pipefail
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"; T=/data/local/tmp
mkdir -p "$out"
comp=$($A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | tr -d '\r' | tail -1)
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell am start -W -n "$comp" --es perf.fakeSession true > /dev/null 2>&1
sleep 5
size=$($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
w=${size%x*}; h=${size#*x}
rx=""
for attempt in 1 2 3 4 5 6 7; do
  $A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $A pull /sdcard/ui.xml "$out/ui.xml" >/dev/null 2>&1
  read -r rx ry < <(python3 D:/projects/tools/.overhaul/out/WP04-perf/findrow.py "$out/ui.xml") && break
  sleep 3
done
[ -n "${rx:-}" ] || { echo "HELDMEM: no row"; exit 2; }
dump() { $A shell "dumpsys gfxinfo $pkg > $T/gfx-$1.txt; dumpsys meminfo $pkg > $T/mem-$1.txt"; }
$A shell input tap $rx $ry; sleep 1.5
dump before
y=$((h / 2)); far=$((w * 45 / 100))
moves="input motionevent DOWN 2 $y"
for i in $(seq 1 24); do moves="$moves; input motionevent MOVE $((2 + far * i / 24)) $y"; done
$A shell "$moves; sleep 0.8; screencap -p $T/held.png; dumpsys gfxinfo $pkg > $T/gfx-held.txt; dumpsys meminfo $pkg > $T/mem-held.txt; input motionevent UP $((2 + far)) $y"
sleep 1.5; dump after1
sleep 33.5; dump after35
$A shell input tap $rx $ry; sleep 1.5
$A shell "input motionevent DOWN 3 $((h * 40 / 100)); sleep 0.5; dumpsys gfxinfo $pkg > $T/gfx-edgehold.txt; input motionevent CANCEL 3 $((h * 40 / 100)); sleep 0.4; dumpsys gfxinfo $pkg > $T/gfx-edgeafter.txt"
$A shell input keyevent 4; sleep 1
for f in held.png gfx-before.txt mem-before.txt gfx-held.txt mem-held.txt gfx-after1.txt mem-after1.txt gfx-after35.txt mem-after35.txt gfx-edgehold.txt gfx-edgeafter.txt; do
  $A pull $T/$f "$out/$f" > /dev/null 2>&1
  $A shell rm -f $T/$f
done
for f in before held after1 after35 edgehold edgeafter; do
  printf '%-10s %s\n' $f "$(grep -A1 'Total GPU memory usage' "$out/gfx-$f.txt" | tail -1 | tr -s ' ')"
done
echo "HELDMEM $out"

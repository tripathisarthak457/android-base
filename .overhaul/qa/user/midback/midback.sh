#!/usr/bin/env bash
# Freezes a predictive back gesture at ~40% and screenshots it, for the old and new builds, from both edges.
set -uo pipefail
S=9a7c74f4; A="adb -s $S"; OUT=/home/crazyhusir/Documents/projects/tools/.overhaul/qa/user/midback
read W H < <($A shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1); Y=$((H/2))
open_detail() {  # $1 package
  $A shell am force-stop $1
  $A shell monkey -p $1 -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 3.5
  $A shell input tap $((W/2)) $((H*30/100)); sleep 2
}
hold_back() {  # $1 edge left|right, $2 file
  local x0 x
  if [ "$1" = left ]; then x0=2; else x0=$((W-2)); fi
  $A shell input motionevent DOWN $x0 $Y
  for i in $(seq 1 14); do
    if [ "$1" = left ]; then x=$((x0 + W*40/100*i/14)); else x=$((x0 - W*40/100*i/14)); fi
    $A shell input motionevent MOVE $x $Y; sleep 0.03
  done
  sleep 0.6; $A shell screencap -p /sdcard/mb.png; $A pull /sdcard/mb.png $OUT/$2.png >/dev/null
  # cancel: move back to the edge, so the screen stays and the next run starts from the same place
  for i in $(seq 13 -1 0); do
    if [ "$1" = left ]; then x=$((x0 + W*40/100*i/14)); else x=$((x0 - W*40/100*i/14)); fi
    $A shell input motionevent MOVE $x $Y; sleep 0.02
  done
  $A shell input motionevent UP $x0 $Y; sleep 1
}
for pkg in com.acme.field.dev com.example.wp04.p2.dev; do
  tag=$([ $pkg = com.acme.field.dev ] && echo old || echo new)
  open_detail $pkg
  hold_back right ${tag}_right
  hold_back left ${tag}_left
done
$A shell input keyevent KEYCODE_HOME
echo MIDBACK DONE

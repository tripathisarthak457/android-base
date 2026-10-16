#!/usr/bin/env bash
# Starts a back swipe from the right edge right after tapping a row (while the push is still animating), freezes it, screenshots.
set -uo pipefail
S=9a7c74f4; A="adb -s $S"; OUT=/home/crazyhusir/Documents/projects/tools/.overhaul/qa/user/midback
read W H < <($A shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1); Y=$((H/2)); X0=$((W-2))
for pkg in com.acme.field.dev com.example.wp04.p2.dev; do
  tag=$([ $pkg = com.acme.field.dev ] && echo old || echo new)
  for n in 1 2 3; do
    $A shell am force-stop $pkg; $A shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 3.5
    # one shell call: tap, then the back swipe starts ~60-120 ms later, mid-push
    $A shell "input tap $((W/2)) $((H*30/100)); input motionevent DOWN $X0 $Y; for i in 1 2 3 4 5 6 7 8; do input motionevent MOVE \$(( $X0 - $W*35/100*i/8 )) $Y; done; screencap -p /sdcard/bdp.png; input motionevent UP \$(( $X0 - $W*35/100 )) $Y"
    $A pull /sdcard/bdp.png $OUT/${tag}_backduringpush_$n.png >/dev/null; sleep 1.2
    $A shell screencap -p /sdcard/bdp2.png; $A pull /sdcard/bdp2.png $OUT/${tag}_backduringpush_${n}_after.png >/dev/null
  done
done
$A shell input keyevent KEYCODE_HOME; echo BDP DONE

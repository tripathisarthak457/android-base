#!/usr/bin/env bash
# Records the current pull-to-refresh on the Redmi (new-shell build, old design system), light and dark.
set -uo pipefail
S=9a7c74f4; A="adb -s $S"; PKG=com.example.wp04.p2.dev
OUT=/home/crazyhusir/Documents/projects/tools/.overhaul/qa/ptr; mkdir -p $OUT/frames
read W H < <($A shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
X=$((W/2)); Y0=$((H*45/100))
$A shell input keyevent KEYCODE_WAKEUP
launch() {
  $A shell am force-stop $PKG
  $A shell am start -n $PKG/com.base.app.MainActivity --es perf.fakeSession true >/dev/null 2>&1 || \
    $A shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 3.5
}
pull() {  # $1 = distance px, $2 = steps
  $A shell input motionevent DOWN $X $Y0
  for i in $(seq 1 $2); do $A shell input motionevent MOVE $X $((Y0 + $1 * i / $2)); sleep 0.03; done
  sleep 0.8
  $A shell input motionevent UP $X $((Y0 + $1))
}
record() {
  local name=$1 secs=$2; shift 2
  $A shell rm -f /sdcard/ptr-$name.mp4
  $A shell screenrecord --size 720x1600 --bit-rate 12000000 --time-limit $secs /sdcard/ptr-$name.mp4 & local rp=$!
  sleep 1.2; "$@"; wait $rp; sleep 0.5
  $A pull /sdcard/ptr-$name.mp4 $OUT/$name.mp4 >/dev/null && echo "recorded $name"
  mkdir -p $OUT/frames/$name
  ffmpeg -loglevel error -y -i $OUT/$name.mp4 -vf fps=30,scale=360:-1 $OUT/frames/$name/f_%04d.png
  ffmpeg -loglevel error -y -i $OUT/$name.mp4 -vf "fps=8,scale=240:-1,tile=8x2" -frames:v 1 $OUT/${name}_strip.png
}
$A shell cmd uimode night no; launch
record light_slow 6 pull 700 30
record light_short 5 pull 260 12
$A shell cmd uimode night yes; sleep 2; launch; sleep 1.5
record dark_slow 6 pull 700 30
$A shell cmd uimode night no
echo RECORD-PTR DONE

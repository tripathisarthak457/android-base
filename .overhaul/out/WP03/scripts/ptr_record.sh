#!/usr/bin/env bash
# Records pull to refresh on a device, light and dark: a long pull that refreshes and a short one that
# does not, each one continuous swipe as a finger gives it, plus the frame statistics of each gesture
# (taken while the screen is recorded, so heavier than ptr_measure.sh's). Run under the adb lock:
#   $L adb ptr_record.sh <serial> <package> <out-dir> [am start extras, e.g. "--es perf.fakeSession true"]
set -uo pipefail
S=$1; PKG=$2; OUT=$3; EXTRAS=${4:-}
A="adb -s $S"; mkdir -p "$OUT/frames"
read W H < <($A shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
X=$((W/2)); Y0=$((H*40/100))
ACT=$($A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "$PKG" | tail -1 | tr -d '\r')
$A shell input keyevent KEYCODE_WAKEUP
$A shell cmd package compile -m speed -f "$PKG" >/dev/null
launch() {
  $A shell cmd statusbar collapse
  $A shell input keyevent KEYCODE_HOME
  $A shell am force-stop "$PKG"
  $A shell am start -W -n "$ACT" $EXTRAS >/dev/null 2>&1
  sleep 4
}
pull() {  # $1 = distance px, $2 = duration ms
  $A shell input swipe $X $Y0 $X $((Y0 + $1)) "$2"
}
record() {
  local name=$1 secs=$2; shift 2
  $A shell rm -f /sdcard/ptr-$name.mp4
  $A shell dumpsys gfxinfo "$PKG" reset >/dev/null
  $A shell screenrecord --size 720x1600 --bit-rate 12000000 --time-limit "$secs" /sdcard/ptr-$name.mp4 & local rp=$!
  sleep 1.2; "$@"; wait $rp; sleep 0.5
  $A shell dumpsys gfxinfo "$PKG" > "$OUT/$name.gfxinfo.txt"
  $A pull /sdcard/ptr-$name.mp4 "$OUT/$name.mp4" >/dev/null && echo "recorded $name"
  mkdir -p "$OUT/frames/$name"
  ffmpeg -loglevel error -y -i "$OUT/$name.mp4" -vf fps=30,scale=360:-1 "$OUT/frames/$name/f_%04d.png"
  ffmpeg -loglevel error -y -i "$OUT/$name.mp4" -vf "fps=8,scale=240:-1,tile=8x2" -frames:v 1 "$OUT/${name}_strip.png"
}
$A shell cmd uimode night no; launch
record light_slow 6 pull 800 1400
record light_short 5 pull 260 600
$A shell cmd uimode night yes; sleep 2; launch; sleep 1.5
record dark_slow 6 pull 800 1400
$A shell cmd uimode night no
echo PTR-RECORD DONE

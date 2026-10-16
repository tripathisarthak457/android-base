#!/usr/bin/env bash
# Records Gmail's predictive back (in-app and back-to-home) on the Redmi, for the motion spec.
# Run under the adb lock: locked adb bash record-gmail.sh
set -uo pipefail
S=9a7c74f4; A="adb -s $S"
OUT=/home/crazyhusir/Documents/projects/tools/.overhaul/research/predictive-back/gmail
mkdir -p $OUT/frames
read W H < <($A shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
Y=$((H/2)); echo "screen ${W}x${H}"
$A shell input keyevent KEYCODE_WAKEUP
$A shell am force-stop com.google.android.gm
$A shell monkey -p com.google.android.gm -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 4
open_first_mail() {
  $A shell uiautomator dump /sdcard/pb-ui.xml >/dev/null 2>&1
  $A pull /sdcard/pb-ui.xml $OUT/ui.xml >/dev/null 2>&1
  python3 - "$OUT/ui.xml" "$H" <<'PY' > $OUT/tap.txt
import re, sys
xml = open(sys.argv[1]).read(); h = int(sys.argv[2])
best = None
for m in re.finditer(r'<node [^>]*?resource-id="([^"]*)"[^>]*?clickable="true"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    rid, x1, y1, x2, y2 = m.group(1), *map(int, m.groups()[1:])
    if "conversation" in rid or "viewified" in rid:
        if y1 > h * 0.15 and (best is None or y1 < best[1]): best = ((x1 + x2) // 2, y1 + (y2 - y1) // 2)
print(*(best or (540, int(h * 0.38))))
PY
  read TX TY < $OUT/tap.txt; echo "tap $TX $TY"
  $A shell input tap $TX $TY; sleep 2.5
}
slow_back() {  # $1 = how far (fraction of width), $2 = commit|cancel
  local far=$(( W * $1 / 100 )) x=2
  $A shell input motionevent DOWN 2 $Y
  for i in $(seq 1 24); do x=$(( 2 + far * i / 24 )); $A shell input motionevent MOVE $x $Y; sleep 0.05; done
  sleep 1.0
  if [ "$2" = cancel ]; then for i in $(seq 23 -1 0); do x=$(( 2 + far * i / 24 )); $A shell input motionevent MOVE $x $Y; sleep 0.04; done; fi
  $A shell input motionevent UP $x $Y
}
record() {  # $1 = name, $2 = seconds, then the gesture function and args
  local name=$1 secs=$2; shift 2
  $A shell rm -f /sdcard/pb-$name.mp4
  $A shell screenrecord --bit-rate 24000000 --time-limit $secs /sdcard/pb-$name.mp4 &
  local rp=$!; sleep 1.2
  "$@"
  wait $rp; sleep 0.5
  $A pull /sdcard/pb-$name.mp4 $OUT/$name.mp4 >/dev/null && echo "recorded $name"
  mkdir -p $OUT/frames/$name
  ffmpeg -loglevel error -i $OUT/$name.mp4 -vf fps=60,scale=360:-1 $OUT/frames/$name/f_%04d.png
  ffmpeg -loglevel error -i $OUT/$name.mp4 -vf "fps=12,scale=240:-1,tile=8x2" -frames:v 1 $OUT/${name}_strip.png
}
open_first_mail
$A shell screencap -p /sdcard/pb-before.png; $A pull /sdcard/pb-before.png $OUT/conversation.png >/dev/null
record slow_cancel 7 slow_back 45 cancel
record slow_commit 6 slow_back 45 commit
open_first_mail
record fast_flick 4 $A shell input swipe 2 $Y $((W*60/100)) $Y 120
record to_home_slow 7 slow_back 40 cancel
record to_home_commit 6 slow_back 40 commit
$A shell input keyevent KEYCODE_HOME
echo RECORD-GMAIL DONE

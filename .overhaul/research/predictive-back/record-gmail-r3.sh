#!/usr/bin/env bash
# Gmail predictive back, in-app (conversation -> inbox) and back-to-home. Opens only a conversation that is
# already read, so nothing in the mailbox changes. screenrecord needs --size on this phone.
set -uo pipefail
S=9a7c74f4; A="adb -s $S"
OUT=/home/crazyhusir/Documents/projects/tools/.overhaul/research/predictive-back/gmail
SP=$(mktemp -d)
read W H < <($A shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
Y=$((H/2)); echo "screen ${W}x${H}"
$A shell input keyevent KEYCODE_WAKEUP
$A shell monkey -p com.google.android.gm -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 3
find_read_row() {
  $A shell uiautomator dump /sdcard/pb-ui.xml >/dev/null 2>&1; $A pull /sdcard/pb-ui.xml $SP/gm-ui.xml >/dev/null 2>&1
  python3 - "$SP/gm-ui.xml" "$H" <<'PY'
import re, sys
x = open(sys.argv[1]).read(); h = int(sys.argv[2])
for m in re.finditer(r'<node [^>]*?>', x):
    n = m.group(0)
    t = re.search(r' text="([^"]*)"', n); b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
    if not t or not b: continue
    t = t.group(1); x1, y1, x2, y2 = map(int, b.groups())
    if x2 - x1 > 900 and ', ' in t and not t.startswith('Unread') and y1 > h * 0.1 and y2 < h * 0.85 and (y2 - y1) > 150:
        print(x1 + 260, y1 + 60, t[:40].replace(' ', '_')); break
PY
}
TAP=""
for i in 1 2 3 4 5 6; do
  TAP=$(find_read_row); [ -n "$TAP" ] && break
  $A shell input swipe 540 1900 540 1000 300; sleep 1.2
done
echo "row: $TAP"; [ -z "$TAP" ] && { echo "no read row found"; exit 1; }
read TX TY _ <<< "$TAP"
open_row() { $A shell input tap $TX $TY; sleep 2.5; $A shell dumpsys activity activities | grep -m1 topResumedActivity; }
slow_back() {  # $1 = how far (percent of width), $2 = commit|cancel
  local far=$(( W * $1 / 100 )) x=2
  $A shell input motionevent DOWN 2 $Y
  for i in $(seq 1 24); do x=$(( 2 + far * i / 24 )); $A shell input motionevent MOVE $x $Y; sleep 0.05; done
  sleep 1.0
  if [ "$2" = cancel ]; then for i in $(seq 23 -1 0); do x=$(( 2 + far * i / 24 )); $A shell input motionevent MOVE $x $Y; sleep 0.04; done; fi
  $A shell input motionevent UP $x $Y
}
diag_back() {  # the finger drifts down while pulling, then returns and cancels
  local far=$(( W * 40 / 100 )) x=2 y=$Y
  $A shell input motionevent DOWN 2 $Y
  for i in $(seq 1 24); do x=$(( 2 + far * i / 24 )); y=$(( Y + (H/4) * i / 24 )); $A shell input motionevent MOVE $x $y; sleep 0.05; done
  sleep 1.0
  for i in $(seq 23 -1 0); do x=$(( 2 + far * i / 24 )); $A shell input motionevent MOVE $x $y; sleep 0.04; done
  $A shell input motionevent UP $x $y
}
record() {
  local name=$1 secs=$2; shift 2
  $A shell rm -f /sdcard/pb-$name.mp4
  $A shell screenrecord --size 720x1600 --bit-rate 12000000 --time-limit $secs /sdcard/pb-$name.mp4 &
  local rp=$!; sleep 1.2
  "$@"
  wait $rp; sleep 0.5
  $A pull /sdcard/pb-$name.mp4 $OUT/$name.mp4 >/dev/null && echo "recorded $name ($(stat -c %s $OUT/$name.mp4) bytes)"
  rm -rf $OUT/frames/$name; mkdir -p $OUT/frames/$name
  ffmpeg -loglevel error -y -i $OUT/$name.mp4 -vf fps=60,scale=360:-1 $OUT/frames/$name/f_%04d.png
  $A shell rm -f /sdcard/pb-$name.mp4
}
open_row
$A shell screencap -p /sdcard/pb-before.png; $A pull /sdcard/pb-before.png $OUT/conversation.png >/dev/null; $A shell rm -f /sdcard/pb-before.png
record slow_cancel 7 slow_back 45 cancel
record diag_cancel 7 diag_back
record slow_commit 6 slow_back 45 commit
$A shell dumpsys activity activities | grep -m1 topResumedActivity
open_row
record fast_flick 4 $A shell input swipe 2 $Y $((W*60/100)) $Y 120
$A shell dumpsys activity activities | grep -m1 topResumedActivity
record to_home_slow 7 slow_back 40 cancel
record to_home_commit 6 slow_back 40 commit
$A shell rm -f /sdcard/pb-ui.xml
echo RECORD-GMAIL DONE

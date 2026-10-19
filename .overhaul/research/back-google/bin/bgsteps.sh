# Runs on the phone (back-trace.win.sh pushes it to /data/local/tmp), inside one Perfetto capture, from the list at
# the root of the first tab: back gestures of three kinds, then touches at the back edge that are not gestures. Each
# step's device uptime goes to /data/local/tmp/bgsteps.txt, so every trace event can be put in its step.
#   sh /data/local/tmp/bgsteps.sh <row x> <row y> <width> <height>
rx=$1; ry=$2; w=$3; h=$4; y=$((h / 2)); f=/data/local/tmp/bgsteps.txt
step() { printf '%s\t%s\n' "$1" "$(cut -d' ' -f1 /proc/uptime)" >> $f; }
# perf.py's own back swipe (gesture_swipe): about 2 vsyncs from the touch-down to the system taking the pointer.
i=1
while [ $i -le 4 ]; do
  input tap $rx $ry; step push$i; sleep 1.5
  step fast$i; input swipe 3 $y $((w * 7 / 10)) $((y + 50)) 260; step fast${i}_end; sleep 1.5
  i=$((i + 1))
done
# A slow start, as a thumb starts a swipe: each `input` call takes tens of ms, so the system takes the pointer
# several frames after the touch-down; then a commit.
for i in 1 2; do
  input tap $rx $ry; step push_slow$i; sleep 1.5
  step slow$i; input motionevent DOWN 3 $y
  for x in 10 20 40 80 140 220 320 440 560; do input motionevent MOVE $x $y; done
  input motionevent UP 560 $y; step slow${i}_end; sleep 1.5
done
# Cancels: out to 40 %, a pause, back to the edge.
for i in 1 2; do
  input tap $rx $ry; step push_cancel$i; sleep 1.5
  step cancel$i; input motionevent DOWN 3 $y
  for x in 40 120 200 280 360 430; do input motionevent MOVE $x $y; done
  sleep 0.4
  for x in 360 280 200 120 40 3; do input motionevent MOVE $x $y; done
  input motionevent UP 3 $y; step cancel${i}_end; sleep 1.5
  input keyevent 4; sleep 1.5
done
# Touches at the back edge of the detail that never become a gesture: held still and ended with CANCEL (no click
# on whatever lies under x = 3), then vertical swipes that scroll the detail.
input tap $rx $ry; step push_edge; sleep 1.5
for i in 1 2 3; do
  step edgetouch$i; input motionevent DOWN 3 $((h * 40 / 100)); sleep 0.25
  input motionevent CANCEL 3 $((h * 40 / 100)); step edgetouch${i}_end; sleep 1.5
done
for i in 1 2; do
  step edgescroll$i; input swipe 3 $((h * 70 / 100)) 3 $((h * 35 / 100)) 200; step edgescroll${i}_end; sleep 1.5
done
input keyevent 4; step done; sleep 1.5

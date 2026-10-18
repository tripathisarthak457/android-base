# Runs on the phone (firstfling.win.sh pushes it to /data/local/tmp): the drag, the lift and the two flings, each
# step's uptime written on the device, so adb's own latency on a busy host never stretches the drag past the capture.
#   sh /data/local/tmp/ffdrag.sh <x> <y0> <height>
x=$1; y0=$2; h=$3; f=/data/local/tmp/ffsteps.txt
step() { printf '%s\t%s\n' "$1" "$(cut -d' ' -f1 /proc/uptime)" >> $f; }
step rest
input motionevent DOWN $x $y0; step down; sleep 0.6
i=1
while [ $i -le 30 ]; do
  input motionevent MOVE $x $((y0 - i * 20)); step move$((i * 20)); sleep 0.15
  i=$((i + 1))
done
screencap -p /data/local/tmp/ff1.png
input motionevent UP $x $((y0 - 600)); step up; sleep 1
input swipe $x $y0 $x $((h * 29 / 100)) 140; step fling1; sleep 1
input swipe $x $y0 $x $((h * 29 / 100)) 140; step fling2; sleep 1

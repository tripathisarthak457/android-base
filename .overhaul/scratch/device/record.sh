# Usage: record.sh <tag> <actions-shell-string>
source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
TAG=$1; ACT=$2; PRE=${3:-""}
[ -n "$PRE" ] && $A shell "$PRE" && sleep 1.5
$A shell rm -f /sdcard/rec_$TAG.mp4
$A shell "screenrecord --time-limit 8 --size 720x1600 --bit-rate 16000000 /sdcard/rec_$TAG.mp4" &
RP=$!
sleep 1.2
$A shell "$ACT"
wait $RP
sleep 1
$A pull /sdcard/rec_$TAG.mp4 $R/rec_$TAG.mp4 >/dev/null
$A shell rm -f /sdcard/rec_$TAG.mp4
ls -la $R/rec_$TAG.mp4

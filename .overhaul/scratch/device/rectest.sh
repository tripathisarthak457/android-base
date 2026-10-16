A="adb -s 9a7c74f4"
( $A shell screenrecord --verbose --time-limit 4 --size 720x1600 /sdcard/rt.mp4 2>&1 | tail -4 ) &
sleep 1.2; $A shell "input tap 284 2213; sleep 1; input tap 156 2213"; wait
$A shell ls -la /sdcard/rt.mp4
$A shell rm -f /sdcard/rt.mp4
$A shell dumpsys SurfaceFlinger | grep -iE "secure|DisplayDevice" | head -5

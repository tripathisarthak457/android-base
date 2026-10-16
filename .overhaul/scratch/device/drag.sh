source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
for spec in "97 162 600 1200 600" "97 162 700 1500 1500" ; do
  set -- $spec
  pos=$(python3 $D/ui.py $R/ui_dt_cur.xml 2>/dev/null | grep "DEV build" | awk '{print $1, $2}')
  $A shell input swipe $spec
  sleep 1
  dump_ui dt_cur
  echo "swipe $spec -> badge now: $(python3 $D/ui.py $R/ui_dt_cur.xml | grep 'DEV build' | awk '{print $1, $2, $3}')"
done
# record a drag to see motion
$A shell rm -f /sdcard/rec_drag.mp4
$A shell "screenrecord --time-limit 4 --size 720x1600 /sdcard/rec_drag.mp4" &
RP=$!; sleep 1
B=$(python3 $D/ui.py $R/ui_dt_cur.xml | grep 'DEV build' | awk '{print $1, $2}')
$A shell input swipe $B 800 1800 1200
wait $RP; sleep 1
$A pull /sdcard/rec_drag.mp4 $R/rec_drag.mp4 >/dev/null; $A shell rm -f /sdcard/rec_drag.mp4
dump_ui dt_after; echo "after recorded drag from $B: $(python3 $D/ui.py $R/ui_dt_after.xml | grep 'DEV build' | awk '{print $1, $2, $3}')"
# open the panel
B=$(python3 $D/ui.py $R/ui_dt_after.xml | grep 'DEV build' | awk '{print $1, $2}')
$A shell input tap $B; sleep 1.5; dump_ui dt_panel; shot dt_panel

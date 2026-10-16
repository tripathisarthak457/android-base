source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=com.jorwen.periflow.dev
for i in 1 2 3 4 5 6; do $A shell am force-stop $PKG; sleep 2; $A shell am start -W -n $PKG/com.jorwen.periflow.MainActivity | grep -E "TotalTime|LaunchState" | tr '\n' ' '; echo; sleep 3; done
$A shell dumpsys package dexopt | grep -A3 "\[$PKG\]" | grep status
dump_ui p0; shot p0
python3 $D/ui.py $R/ui_p0.xml | awk '{$1="";$2="";$3=""; print}' | grep -v "''" | head -20

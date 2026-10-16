source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
$A shell am start -W -n com.acme.field.dev/com.acme.field.MainActivity >/dev/null; sleep 2
$A shell input tap 156 2213; sleep 1
$A shell "input swipe 540 1800 540 600 300; sleep 0.8; input swipe 540 1800 540 600 300; sleep 1.5"
dump_ui rs1; echo "before: $(python3 $D/ui.py $R/ui_rs1.xml | grep TextView | sed -n 1,4p | awk '{$1=$2=$3=$4=$5=$6=""; print}' | tr '\n' '|')"
$A shell input tap 284 2213; sleep 1.5; $A shell input tap 156 2213; sleep 1.5
dump_ui rs2; echo "after tab round-trip: $(python3 $D/ui.py $R/ui_rs2.xml | grep TextView | sed -n 1,4p | awk '{$1=$2=$3=$4=$5=$6=""; print}' | tr '\n' '|')"
$A shell input tap 540 1000; sleep 1.5; $A shell input keyevent 4; sleep 1.5
dump_ui rs3; echo "after push/pop: $(python3 $D/ui.py $R/ui_rs3.xml | grep TextView | sed -n 1,4p | awk '{$1=$2=$3=$4=$5=$6=""; print}' | tr '\n' '|')"

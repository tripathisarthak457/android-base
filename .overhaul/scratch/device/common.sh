A="adb -s 9a7c74f4"
D=/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device
R=$D/results; mkdir -p $R
dump_ui() { # $1 = out name
  $A shell uiautomator dump /sdcard/ui_$1.xml >/dev/null 2>&1
  $A pull /sdcard/ui_$1.xml $R/ui_$1.xml >/dev/null 2>&1
  $A shell rm -f /sdcard/ui_$1.xml
}
shot() { # $1 = out name
  $A exec-out screencap -p > $R/shot_$1.png
}

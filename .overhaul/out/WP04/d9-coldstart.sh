#!/usr/bin/env bash
# D9 (qa/shimmer EVIDENCE): a cold start with the app's theme dark on a light phone. Records two launches
# of the installed app: the first after `pm clear` with perf.theme=dark (the theme is written at launch),
# then a plain cold start (the theme already stored). Per frame: mean luma of the app's middle.
#   $L adb ./d9-coldstart.sh <apk> <package> <activity> <out dir> > logs/<step>.log 2>&1
set -uo pipefail
apk=$1; pkg=$2; act=$3; out=$4; S=9a7c74f4
a() { adb -s $S "$@"; }
mkdir -p "$out"
a shell input keyevent KEYCODE_WAKEUP; sleep 0.5
if a shell dumpsys window | grep -m1 isKeyguardShowing | grep -q "isKeyguardShowing=true"; then echo "LOCKED"; exit 2; fi
echo "system night mode: $(a shell cmd uimode night)"
rot=$(a shell settings get system accelerometer_rotation)
a shell settings put system accelerometer_rotation 0
[ -n "$apk" ] && a install -r -d "$apk" | tail -1
record() { # name, then the launch command
  local name=$1; shift
  a shell am force-stop $pkg; sleep 1
  a shell rm -f /sdcard/d9.mp4
  a shell screenrecord --time-limit 7 --size 720x1600 --bit-rate 16000000 /sdcard/d9.mp4 &
  local rec=$!
  sleep 1.2
  a shell "$@"
  wait $rec; sleep 1
  a pull /sdcard/d9.mp4 "$out/$name.mp4" >/dev/null
  mkdir -p "$out/$name"; rm -f "$out/$name"/f_*.png
  ffmpeg -v fatal -y -i "$out/$name.mp4" -fps_mode passthrough "$out/$name/f_%04d.png"
  ffmpeg -v fatal -i "$out/$name.mp4" -fps_mode passthrough -vf "crop=iw:ih*0.6:0:ih*0.2,signalstats,metadata=print:file=-" -f null - \
    | grep -o "lavfi.signalstats.YAVG=[0-9.]*" | cut -d= -f2 > "$out/$name/luma.txt"
  ffprobe -v error -select_streams v:0 -show_entries frame=pts_time -of csv=p=0 "$out/$name.mp4" > "$out/$name/times.txt"
  echo "== $name: $(wc -l < "$out/$name/luma.txt") frames; luma $(paste -sd' ' "$out/$name/luma.txt" | cut -c1-600)"
}
a shell pm clear $pkg >/dev/null
record first "am start -W -n $pkg/$act --es perf.fakeSession true --es perf.theme dark"
echo "app night mode after first: $(a shell cmd uimode night)"
sleep 3
record returning "am start -W -n $pkg/$act"
# The person changing the theme while the app runs: Settings, then Light. The app's night mode changes
# with it, which recreates an activity that does not handle uiMode.
a shell am start -W -n $pkg/$act >/dev/null; sleep 3
center() { # the centre of the first node whose text or resource id is $1, from a UI dump
  a shell uiautomator dump /sdcard/d9.xml >/dev/null; a shell cat /sdcard/d9.xml | python3 -c "
import re,sys
x=sys.stdin.read()
for n in re.finditer(r'<node [^>]*>', x):
    t=n.group(0)
    if 'text=\"$1\"' in t or 'resource-id=\"$1\"' in t:
        b=list(map(int, re.findall(r'\d+', re.search(r'bounds=\"([^\"]*)\"', t).group(1))))
        print((b[0]+b[2])//2, (b[1]+b[3])//2); break"
}
read -r tx ty < <(center tab_4); a shell input tap $tx $ty; sleep 2
read -r lx ly < <(center Light)
a shell rm -f /sdcard/d9.mp4
a shell screenrecord --time-limit 5 --size 720x1600 --bit-rate 16000000 /sdcard/d9.mp4 &
rec=$!
sleep 1.2
a shell input tap $lx $ly
wait $rec; sleep 1
a pull /sdcard/d9.mp4 "$out/switch.mp4" >/dev/null
mkdir -p "$out/switch"; rm -f "$out/switch"/f_*.png
ffmpeg -v fatal -y -i "$out/switch.mp4" -fps_mode passthrough "$out/switch/f_%04d.png"
ffmpeg -v fatal -i "$out/switch.mp4" -fps_mode passthrough -vf "crop=iw:ih*0.6:0:ih*0.2,signalstats,metadata=print:file=-" -f null - \
  | grep -o "lavfi.signalstats.YAVG=[0-9.]*" | cut -d= -f2 > "$out/switch/luma.txt"
ffprobe -v error -select_streams v:0 -show_entries frame=pts_time -of csv=p=0 "$out/switch.mp4" > "$out/switch/times.txt"
echo "== switch: $(wc -l < "$out/switch/luma.txt") frames"
# Put the app's theme back to System for whatever runs next.
read -r sx sy < <(center System); a shell input tap $sx $sy; sleep 1
a shell am force-stop $pkg
a shell settings put system accelerometer_rotation ${rot:-0}
echo D9 DONE

#!/usr/bin/env bash
# Investigation (debug build, counts only): which composables run in each frame of tab switches, pushes and pops.
#   $L adb ./inv-run.sh > logs/inv-run.log 2>&1
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04; S=9a7c74f4; PKG=com.example.wp04.inv.dev
export ANDROID_SERIAL=$S
adb install -r $O/inv-everything/app/build/outputs/apk/dev/debug/app-dev-debug.apk
adb shell pm clear $PKG
adb shell am start -W -n $PKG/com.example.wp04.inv.MainActivity --es perf.fakeSession true
sleep 8
adb shell uiautomator dump /data/local/tmp/inv.xml >/dev/null
adb pull /data/local/tmp/inv.xml $O/logs/inv-ui.xml >/dev/null
coords=$(python3 - <<'PY'
import re
x=open('/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/logs/inv-ui.xml').read()
out={}
for m in re.finditer(r'resource-id="(tab_\d|primary_list)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
    i,a,b,c,d=m.groups(); out[i]=((int(a)+int(c))//2,(int(b)+int(d))//2, int(b))
print(' '.join(f"{k}={v[0]},{v[1]},{v[2]}" for k,v in sorted(out.items())))
PY
)
echo "coords: $coords"
tap() { local v; v=$(echo "$coords" | tr ' ' '\n' | grep "^$1=" | cut -d= -f2); echo "${v%,*}" | tr ',' ' '; }
mark() { adb shell log -t perf.mark "$1"; }
# Visit every tab once so each is built, then return to the first.
for t in 1 2 3 4 0; do adb shell input tap $(tap tab_$t); sleep 2; done
adb logcat -c
for round in 1 2; do
  for t in 1 2 3 4 0; do mark "tab_switch $t"; adb shell input tap $(tap tab_$t); sleep 2; done
done
pl=$(echo "$coords" | tr ' ' '\n' | grep '^primary_list=' | cut -d= -f2)
px=$(echo $pl | cut -d, -f1); py=$(( $(echo $pl | cut -d, -f3) + 250 ))
for i in 1 2 3; do
  mark push; adb shell input tap $px $py; sleep 2
  mark pop; adb shell input keyevent 4; sleep 2
done
adb logcat -d -v epoch -s perf.compose:I perf.mark:I perf.shell:I > $O/logs/inv-compose${INV_SUFFIX:-}.txt
echo INV-RUN DONE

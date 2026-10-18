#!/usr/bin/env bash
# Run 24: what one back swipe leaves in the standard preset's heaps. The no-swipe A/B moved app PSS from 48.3 to
# 42.1 MB (native heap 19.0 -> 14.2 MB). This separates the first swipe after pm clear (shaders compiled from
# source) from a swipe in a later process (program binaries from HWUI's disk cache), each against the same process
# before it swipes.
#   $L adb ./swipe-mem.win.sh > logs/r24-swipe-mem.log 2>&1
set -euo pipefail
export MSYS_NO_PATHCONV=1
S=9a7c74f4; PKG=com.example.wp04.standard.dev; O=D:/projects/tools/.overhaul/out/WP04
a() { adb -s $S "$@"; }
A=$(a shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $PKG | tail -1 | tr -d '\r')
read W H < <(a shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
start() { a shell am force-stop $PKG; a shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
  -f 0x10200000 -n $A --es perf.fakeSession true > /dev/null; sleep 5; }
mem() { a shell dumpsys meminfo $PKG > $O/swipe-mem/$1.txt
  echo "$1: $(grep -E '^ +(Native Heap|Java Heap|Code|Graphics):|TOTAL PSS' $O/swipe-mem/$1.txt | tr -s ' ' | tr '\n' ' ')"; }
row() { a shell uiautomator dump /sdcard/wp04-ui.xml > /dev/null; a shell cat /sdcard/wp04-ui.xml > $O/swipe-mem/ui.xml
  python3 - $O/swipe-mem/ui.xml <<'PY'
import re, sys
xml = open(sys.argv[1], encoding="utf-8").read()
start = xml.index('resource-id="primary_list"')
for match in re.finditer(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml[start:]):
    x1, y1, x2, y2 = map(int, match.groups())
    if y2 - y1 < 400:
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
}
push() { a shell input tap $(row); sleep 1.5; }
swipe() { a shell input swipe 3 $((H / 2)) $((W * 7 / 10)) $((H / 2 + 50)) 260; sleep 2; }
# Run 24b: does every swipe keep more, or does it level off? Then whether a trim gives it back.
start; mem q-fresh
for i in 1 2 3 4 5 6 7 8 9 10; do push; swipe; case $i in 1|3|6|10) mem q-after-swipe-$i;; esac; done
for i in 1 2 3 4 5; do push; a shell input keyevent 4; sleep 2; done; mem q-after-5-keypops
a shell am send-trim-memory $PKG RUNNING_CRITICAL > /dev/null 2>&1 || true; sleep 3; mem q-after-trim
a shell input keyevent 3; sleep 4; mem q-in-background
a shell rm -f /sdcard/wp04-ui.xml
echo SWIPE-MEM DONE

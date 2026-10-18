#!/usr/bin/env bash
# Run 24: which native allocations a back swipe keeps that the Back key does not (standard's PSS row, Deviation 31).
# heapprofd attached to a running everything benchmark build (profileable), one case per fresh process:
# `key` = a push and the Back key, `swipe` = a push and a back swipe. The trace's unreleased bytes are what stays.
#   $L adb ./heap.win.sh > ../logs/r24-heap.log 2>&1
set -euo pipefail
export MSYS_NO_PATHCONV=1
S=9a7c74f4; PKG=com.example.wp04.everything.dev; H=D:/projects/tools/.overhaul/out/WP04/heap
a() { adb -s $S "$@"; }
a install -r $H/everything-7fa49d5.apk > /dev/null
a shell pm clear $PKG > /dev/null
A=$(a shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $PKG | tail -1 | tr -d '\r')
read W HT < <(a shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
start() { a shell am force-stop $PKG; a shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
  -f 0x10200000 -n $A --es perf.fakeSession true > /dev/null; sleep 6; }
row() { a shell uiautomator dump /sdcard/wp04-ui.xml > /dev/null; a shell cat /sdcard/wp04-ui.xml > $H/ui.xml
  python3 - $H/ui.xml <<'PY'
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
# A first pass after pm clear compiles every shader the two cases draw, so both cases load program binaries.
start; xy=$(row); a shell input tap $xy; sleep 1.5; a shell input keyevent 4; sleep 2
a shell input tap $xy; sleep 1.5; a shell input swipe 3 $((HT / 2)) $((W * 7 / 10)) $((HT / 2 + 50)) 260; sleep 2
for case in key swipe; do
  start
  remote=/data/misc/perfetto-traces/wp04-heap-$case.pftrace
  a shell rm -f $remote
  a shell "perfetto --txt -c - -o $remote --background" <<CFG
buffers { size_kb: 65536 }
duration_ms: 14000
data_sources { config { name: "android.heapprofd" heapprofd_config {
  sampling_interval_bytes: 4096 process_cmdline: "$PKG" block_client: true } } }
CFG
  sleep 3
  a shell input tap $xy; sleep 1.5
  if [ $case = key ]; then a shell input keyevent 4; else a shell input swipe 3 $((HT / 2)) $((W * 7 / 10)) $((HT / 2 + 50)) 260; fi
  sleep 13
  a pull $remote $H/$case.pftrace > /dev/null
  a shell rm -f $remote
  a shell dumpsys meminfo $PKG | grep -E 'Native Heap:|Java Heap:' | tr -s ' ' | tr '\n' ' '; echo " <- $case"
done
a shell rm -f /sdcard/wp04-ui.xml
ls -la $H/*.pftrace
echo HEAP DONE

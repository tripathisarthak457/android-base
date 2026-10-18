#!/usr/bin/env bash
# Where the app's PSS after a journey comes from: the same journey (every tab twice, a row pushed and popped three
# times, the list flung up and down) run on the installed everything APK twice, first right after pm clear (no
# shader cache: every program is compiled in the process), then in a new process with the cache the first run
# wrote, each followed by dumpsys meminfo. Under the adb lock:  $L adb ./mem-ab.win.sh <out-dir> [package]
set -uo pipefail
# Device paths (/sdcard, /data) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"
mkdir -p "$out"
here=$(dirname "$0")
journey() {
  local tag=$1
  $A shell am start -W -n $pkg/com.example.wp04.everything.MainActivity --es perf.fakeSession true > /dev/null 2>&1
  sleep 5
  local rx="" ry=""
  for attempt in 1 2 3 4 5 6 7; do
    $A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $A pull /sdcard/ui.xml "$out/ui-$tag.xml" >/dev/null 2>&1
    read -r rx ry < <(python3 "$here/findrow.py" "$out/ui-$tag.xml") && break
    sleep 3
  done
  [ -n "$rx" ] || { echo "MEM-AB: no row ($tag)"; return 2; }
  # The tab cells' centres, from the same dump.
  local tabs
  tabs=$(python3 - "$out/ui-$tag.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
cells = {}
for node in ET.parse(sys.argv[1]).getroot().iter("node"):
    rid = node.get("resource-id") or ""
    if re.fullmatch(r"tab_\d", rid):
        l, t, r, b = map(int, re.findall(r"\d+", node.get("bounds")))
        cells[rid] = f"{(l + r) // 2},{(t + b) // 2}"
print(" ".join(cells[k] for k in sorted(cells)))
PY
)
  local w h
  read -r w h < <($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{s=(o!=""?o:p); sub("x"," ",s); print s}')
  for pass in 1 2; do
    for cell in $tabs; do $A shell input tap ${cell%,*} ${cell#*,}; sleep 1.2; done
  done
  local first=${tabs%% *}
  $A shell input tap ${first%,*} ${first#*,}; sleep 1.5
  for i in 1 2 3; do
    $A shell input tap $rx $ry; sleep 1.5
    $A shell input keyevent 4; sleep 1.5
  done
  for i in 1 2 3; do
    $A shell input swipe $((w / 2)) $((h * 7 / 10)) $((w / 2)) $((h * 3 / 10)) 120; sleep 1.2
    $A shell input swipe $((w / 2)) $((h * 3 / 10)) $((w / 2)) $((h * 7 / 10)) 120; sleep 1.2
  done
  sleep 3
  $A shell dumpsys meminfo $pkg > "$out/meminfo-$tag.txt"
  grep -E "Native Heap|\.so mmap|TOTAL PSS|Graphics:|Code:|Java Heap:" "$out/meminfo-$tag.txt" | head -8 | sed "s/^/  $tag /"
}
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell pm clear $pkg > /dev/null
journey cleared
$A shell am force-stop $pkg; sleep 2
journey warm
$A shell am force-stop $pkg
$A shell input keyevent KEYCODE_HOME
echo MEM-AB DONE

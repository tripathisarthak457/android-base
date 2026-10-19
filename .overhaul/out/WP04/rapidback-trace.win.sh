#!/usr/bin/env bash
# A Perfetto capture of Rapid Back next to plain Back on the installed everything APK, to tell why a burst's second
# frame comes two vsyncs after its first (handoff, run 28): three cycles of a push and perf.py's burst
# (`input keyevent 4 4 4`), then three of a push and a single Back, 1.5-2 s apart so each starts from idle. With
# DEPTH=4 each push is perf.py's push_to_depth (the row, then two related rows), so the burst pops past the first
# detail and its counted presses become a second pop.
# FrameTimeline, sched and the app's gfx, view and input sections. Under the adb lock:
#   $L adb ./rapidback-trace.win.sh <out-dir> [package]
set -uo pipefail
# Device paths (/sdcard, /data) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"
FINDROW=D:/projects/tools/.overhaul/out/WP04-perf/findrow.py
FINDRELATED=D:/projects/tools/.overhaul/out/WP04/findrelated.py
DEPTH=${DEPTH:-2}
mkdir -p "$out"
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell am start -W -n $pkg/com.example.wp04.everything.MainActivity --es perf.fakeSession true > "$out/am-start.txt" 2>&1
sleep 5
# The list loads from the network and uiautomator refuses a screen that keeps animating: try for 20 s.
: > "$out/row.txt"
for attempt in 1 2 3 4 5 6 7; do
  $A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $A pull /sdcard/ui.xml "$out/ui.xml" >/dev/null 2>&1
  python3 "$FINDROW" "$out/ui.xml" > "$out/row.txt" && break
  sleep 3
done
$A shell rm -f /sdcard/ui.xml
read -r rx ry < "$out/row.txt" || { echo "RAPIDBACK-TRACE: no row"; exit 2; }
echo "row at $rx,$ry"
h=$($A shell wm size | tr -d '\r' | awk -F'x' 'END{print $2}')
push() {
  $A shell input tap $rx $ry; sleep 1.3
  local level
  for level in $(seq 3 "$DEPTH"); do
    $A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $A pull /sdcard/ui.xml "$out/detail.xml" >/dev/null 2>&1
    read -r dx dy < <(python3 "$FINDRELATED" "$out/detail.xml" "$h") || break
    $A shell input tap $dx $dy; sleep 1.3
  done
  $A shell rm -f /sdcard/ui.xml
}
# One pass before the capture, so shaders, glyphs and code are paid.
push; sleep 0.2; $A shell input keyevent 4 4 4; sleep 2
$A shell input tap $rx $ry; sleep 1.5; $A shell input keyevent 4; sleep 2
sed "s|\$pkg|$pkg|" > "$out/config.pbtxt" <<'CFG'
buffers { size_kb: 131072 fill_policy: RING_BUFFER }
data_sources { config { name: "android.surfaceflinger.frametimeline" } }
data_sources { config { name: "linux.process_stats" target_buffer: 0 } }
data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      ftrace_events: "sched/sched_switch"
      ftrace_events: "sched/sched_wakeup"
      ftrace_events: "power/cpu_frequency"
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "input"
      atrace_apps: "$pkg"
    }
  }
}
duration_ms: 45000
CFG
# The config goes in on stdin: /data/local/tmp is not readable by perfetto on this phone.
$A shell "perfetto --txt -c - -o /data/misc/perfetto-traces/rapidback.perfetto-trace" < "$out/config.pbtxt" > "$out/perfetto.log" 2>&1 &
sleep 2.5
for i in 1 2 3; do
  push; sleep 0.2
  $A shell input keyevent 4 4 4; sleep 2
done
# A single Back from one push, as perf.py's pushpop: it lands on the list again.
for i in 1 2 3; do
  $A shell input tap $rx $ry; sleep 1.5
  $A shell input keyevent 4; sleep 2
done
wait
$A pull /data/misc/perfetto-traces/rapidback.perfetto-trace "$out/trace.perfetto-trace" >/dev/null 2>&1
$A shell rm -f /data/misc/perfetto-traces/rapidback.perfetto-trace
ls -la "$out/trace.perfetto-trace"
echo RAPIDBACK-TRACE DONE

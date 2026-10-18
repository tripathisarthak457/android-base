#!/usr/bin/env bash
# A Perfetto capture of pushes and pops from idle on the installed everything APK, as dvfs-trace.win.sh captures tab
# switches: FrameTimeline, cpufreq, sched and the app's own sections around four cycles of a row tap (push), then
# Back (pop), a row tap, then the back swipe of perf.py's timing scenarios (gesture pop), 1.5 s apart, so every
# transition starts from idle. Under the adb lock:  $L adb ./nav-trace.win.sh <out-dir> [package]
set -uo pipefail
# Device paths (/sdcard, /data) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"
mkdir -p "$out"
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell am start -W -n $pkg/com.example.wp04.everything.MainActivity --es perf.fakeSession true > "$out/am-start.txt" 2>&1
sleep 5
size=$($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
w=${size%x*}; h=${size#*x}
# The list loads from the network and uiautomator refuses a screen that keeps animating: try for 20 s.
: > "$out/row.txt"
for attempt in 1 2 3 4 5 6 7; do
  $A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $A pull /sdcard/ui.xml "$out/ui.xml" >/dev/null 2>&1
  python3 "$(dirname "$0")/findrow.py" "$out/ui.xml" > "$out/row.txt" && break
  sleep 3
done
read -r rx ry < "$out/row.txt" || { echo "NAV-TRACE: no row"; exit 2; }
echo "window ${w}x${h}, row at $rx,$ry"
swipe="input swipe 3 $((h / 2)) $((w * 7 / 10)) $((h / 2 + 50)) 260"
# One pass before the capture, so what is measured is a warm push and pop (shaders, glyphs and code paid).
$A shell input tap $rx $ry; sleep 1.5; $A shell input keyevent 4; sleep 1.5
$A shell input tap $rx $ry; sleep 1.5; $A shell "$swipe"; sleep 2
sed "s|\$pkg|$pkg|" > "$out/config.pbtxt" <<'CFG'
buffers { size_kb: 131072 fill_policy: RING_BUFFER }
data_sources { config { name: "android.surfaceflinger.frametimeline" } }
data_sources { config { name: "linux.process_stats" target_buffer: 0 } }
data_sources { config { name: "linux.sys_stats" sys_stats_config { cpufreq_period_ms: 10 } } }
data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      ftrace_events: "sched/sched_switch"
      ftrace_events: "sched/sched_wakeup"
      ftrace_events: "power/cpu_frequency"
      ftrace_events: "power/cpu_idle"
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "input"
      atrace_categories: "dalvik"
      atrace_apps: "$pkg"
    }
  }
}
duration_ms: 30000
CFG
# The config goes in on stdin: /data/local/tmp is not readable by perfetto on this phone.
$A shell "perfetto --txt -c - -o /data/misc/perfetto-traces/nav.perfetto-trace" < "$out/config.pbtxt" > "$out/perfetto.log" 2>&1 &
sleep 2.5
for i in 1 2 3 4; do
  $A shell input tap $rx $ry; sleep 1.5
  $A shell input keyevent 4; sleep 1.5
  $A shell input tap $rx $ry; sleep 1.5
  $A shell "$swipe"; sleep 1.5
done
wait
$A pull /data/misc/perfetto-traces/nav.perfetto-trace "$out/trace.perfetto-trace" >/dev/null 2>&1
$A shell rm -f /data/misc/perfetto-traces/nav.perfetto-trace
ls -la "$out/trace.perfetto-trace"
echo NAV-TRACE DONE

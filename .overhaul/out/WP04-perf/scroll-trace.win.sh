#!/usr/bin/env bash
# A Perfetto capture of perf.py's scroll scenario as its runs meet it: after `pm clear` (which empties the app's
# shader cache), the APK's profile and a speed-profile compile, a launch and a settle, six flings down the root list
# and six back up. HWUI's sections (shader_compile, cache_hit), FrameTimeline, sched and cpufreq, so a slow
# RenderThread frame can be told apart: a shader compiled on first use, a little core, or the GPU.
# Then the same flings again in a second capture, with the shaders the first one compiled now cached.
# Under the adb lock:  $L adb ./scroll-trace.win.sh <out-dir> [package]
set -uo pipefail
# Device paths (/sdcard, /data) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"
mkdir -p "$out"
comp=$($A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | tr -d '\r' | tail -1)
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell pm clear $pkg > /dev/null
$A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n $pkg/androidx.profileinstaller.ProfileInstallReceiver > /dev/null
$A shell am force-stop $pkg
$A shell cmd package compile -f -m speed-profile $pkg > /dev/null
$A shell am start -W -n "$comp" --es perf.fakeSession true > "$out/am-start.txt" 2>&1
sleep 6
size=$($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
w=${size%x*}; h=${size#*x}
x=$((w / 2)); low=$((h * 75 / 100)); high=$((h * 29 / 100))
flings="for i in 1 2 3 4 5 6; do input swipe $x $low $x $high 140; sleep 0.7; done; for i in 1 2 3 4 5 6; do input swipe $x $high $x $low 140; sleep 0.7; done"
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
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "input"
      atrace_categories: "dalvik"
      atrace_apps: "$pkg"
    }
  }
}
duration_ms: 16000
CFG
for pass in first cached; do
  # The config goes in on stdin: /data/local/tmp is not readable by perfetto on this phone.
  $A shell "perfetto --txt -c - -o /data/misc/perfetto-traces/scroll.perfetto-trace" < "$out/config.pbtxt" > "$out/perfetto-$pass.log" 2>&1 &
  sleep 2.5
  $A shell "$flings"
  wait
  $A pull /data/misc/perfetto-traces/scroll.perfetto-trace "$out/$pass.perfetto-trace" > /dev/null 2>&1
  $A shell rm -f /data/misc/perfetto-traces/scroll.perfetto-trace
  sleep 1
done
ls -la "$out"
echo SCROLL-TRACE DONE

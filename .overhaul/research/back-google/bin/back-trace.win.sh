#!/usr/bin/env bash
# For each ladder build out/WP04-perf/<name>: installs its APK as perf.sh does (install, pm clear, the APK's profile,
# speed-profile), opens it once, makes one warm push and back swipe, then captures bgsteps.sh in one Perfetto trace
# (FrameTimeline, sched, cpufreq, GPU frequency if the kernel has it, input, the app's sections and its BackCard.*
# counters) into perf/WP04-perf/<name>-<tag>/: trace.perfetto-trace and steps.txt (device uptime of every step).
# Device files stay in /data/local/tmp and /data/misc/perfetto-traces. Under the adb lock:
#   $L adb ./back-trace.win.sh <tag> <name>...
set -uo pipefail
export MSYS_NO_PATHCONV=1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; R=$OV/research/back-google; S=9a7c74f4
pkg=com.example.wp04.everything.dev
A="adb -s $S"
tag=$1; shift
for n in "$@"; do
  apk=$O/$n/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
  [ -f "$apk" ] || { echo "no apk for $n"; continue; }
  out=$P/$n-$tag; mkdir -p "$out"
  echo "== $(date +%H:%M:%S) $n ($(cat $O/$n/BUILT_FROM) + $(cat $O/$n/BUILT_DIRTY)) -> $out"
  $A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
  $A shell am force-stop $pkg
  $A install -r -t -d "$apk" > /dev/null
  $A shell pm clear $pkg > /dev/null
  $A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n $pkg/androidx.profileinstaller.ProfileInstallReceiver > /dev/null
  $A shell am force-stop $pkg
  $A shell cmd package compile -f -m speed-profile $pkg > /dev/null
  $A shell am start -W -n $pkg/com.example.wp04.everything.MainActivity --es perf.fakeSession true > "$out/am-start.txt" 2>&1
  sleep 6
  size=$($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
  w=${size%x*}; h=${size#*x}
  : > "$out/row.txt"
  for attempt in 1 2 3 4 5 6 7; do
    $A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $A pull /sdcard/ui.xml "$out/ui.xml" >/dev/null 2>&1
    python3 $O/findrow.py "$out/ui.xml" > "$out/row.txt" && break
    sleep 3
  done
  read -r rx ry < "$out/row.txt" || { echo "BACK-TRACE: no row for $n"; continue; }
  # One warm pass, so what is captured is a warm push and gesture (shaders, glyphs and code paid).
  $A shell input tap $rx $ry; sleep 1.5
  $A shell "input swipe 3 $((h / 2)) $((w * 7 / 10)) $((h / 2 + 50)) 260"; sleep 2
  sed "s|\$pkg|$pkg|" > "$out/config.pbtxt" <<'CFG'
buffers { size_kb: 196608 fill_policy: RING_BUFFER }
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
      ftrace_events: "power/gpu_frequency"
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "input"
      atrace_categories: "dalvik"
      atrace_apps: "$pkg"
    }
  }
}
duration_ms: 90000
CFG
  $A push $R/bin/bgsteps.sh /data/local/tmp/bgsteps.sh > /dev/null
  $A shell rm -f /data/local/tmp/bgsteps.txt
  $A shell "perfetto --txt -c - -o /data/misc/perfetto-traces/bg.perfetto-trace" < "$out/config.pbtxt" > "$out/perfetto.log" 2>&1 &
  sleep 2.5
  $A shell sh /data/local/tmp/bgsteps.sh $rx $ry $w $h
  wait
  $A pull /data/local/tmp/bgsteps.txt "$out/steps.txt" > /dev/null 2>&1
  $A pull /data/misc/perfetto-traces/bg.perfetto-trace "$out/trace.perfetto-trace" > /dev/null 2>&1
  $A shell rm -f /data/misc/perfetto-traces/bg.perfetto-trace /data/local/tmp/bgsteps.txt /data/local/tmp/bgsteps.sh
  ls -la "$out/trace.perfetto-trace"
done
echo BACK-TRACE DONE

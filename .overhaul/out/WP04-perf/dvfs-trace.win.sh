#!/usr/bin/env bash
# Characterises the GPU and CPU clocks around a tab switch from idle on the installed everything APK: a Perfetto
# capture with FrameTimeline, cpu/gpu frequency (ftrace + devfreq polling) and the app's own sections, around
# plain taps and press-hold taps on the tabs. Windows port of dvfs-trace.sh. Run under the adb lock:  $L adb ./dvfs-trace.win.sh <out-dir> [package]
set -uo pipefail
# Device paths (/sdcard, /data) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"
mkdir -p "$out"
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell am start -W -n $pkg/com.example.wp04.everything.MainActivity --ez perf.fakeSession true > "$out/am-start.txt" 2>&1
sleep 4
$A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $A pull /sdcard/ui.xml "$out/ui.xml" >/dev/null 2>&1
python3 - "$out/ui.xml" > "$out/tabs.txt" <<'PY'
import re,sys
xml=open(sys.argv[1],errors='ignore').read()
for m in re.finditer(r'resource-id="tab_(\d)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    i,l,t,r,b=map(int,m.groups()); print(i,(l+r)//2,(t+b)//2)
PY
cat "$out/tabs.txt"
tapx() { awk -v i=$1 '$1==i{print $2}' "$out/tabs.txt"; }
tapy() { awk -v i=$1 '$1==i{print $3}' "$out/tabs.txt"; }
# A pass over every tab before the capture, so what is measured is a warm switch (shaders, glyphs and code paid).
for i in 1 2 3 4 0; do $A shell input tap $(tapx $i) $(tapy $i); sleep 1.2; done
sleep 2
cat > "$out/config.pbtxt" <<CFG
buffers { size_kb: 131072 fill_policy: RING_BUFFER }
data_sources { config { name: "android.surfaceflinger.frametimeline" } }
data_sources { config { name: "linux.process_stats" target_buffer: 0 } }
data_sources { config { name: "linux.sys_stats" sys_stats_config { devfreq_period_ms: 10 cpufreq_period_ms: 10 } } }
data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      ftrace_events: "sched/sched_switch"
      ftrace_events: "sched/sched_wakeup"
      ftrace_events: "power/cpu_frequency"
      ftrace_events: "power/cpu_idle"
      ftrace_events: "power/gpu_frequency"
      ftrace_events: "kgsl/kgsl_pwrlevel"
      ftrace_events: "kgsl/kgsl_gpubusy"
      ftrace_events: "kgsl/kgsl_clk"
      ftrace_events: "kgsl/kgsl_pwr_set_state"
      ftrace_events: "kgsl/kgsl_buslevel"
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "input"
      atrace_categories: "power"
      atrace_categories: "hal"
      atrace_apps: "$pkg"
    }
  }
}
duration_ms: 22000
CFG
# The config goes in on stdin: /data/local/tmp is not readable by perfetto on this phone.
$A shell "perfetto --txt -c - -o /data/misc/perfetto-traces/dvfs.perfetto-trace" < "$out/config.pbtxt" > "$out/perfetto.log" 2>&1 &
sleep 2.5
# Plain taps: 1, 2, 3; then press-hold (250 ms) taps: 4, 0, 1; then a tap after a long idle.
for i in 1 2 3; do $A shell input tap $(tapx $i) $(tapy $i); sleep 1.5; done
for i in 4 0 1; do $A shell "input motionevent DOWN $(tapx $i) $(tapy $i); sleep 0.25; input motionevent UP $(tapx $i) $(tapy $i)"; sleep 1.5; done
sleep 4
$A shell input tap $(tapx 2) $(tapy 2); sleep 1.5
$A shell input tap $(tapx 0) $(tapy 0); sleep 1.5
wait
$A pull /data/misc/perfetto-traces/dvfs.perfetto-trace "$out/trace.perfetto-trace" >/dev/null 2>&1
$A shell rm -f /data/misc/perfetto-traces/dvfs.perfetto-trace
ls -la "$out/trace.perfetto-trace"
echo DVFS-TRACE DONE

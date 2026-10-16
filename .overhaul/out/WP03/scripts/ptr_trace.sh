#!/usr/bin/env bash
# Pull to refresh on the phone with nothing else running: frame statistics of three slow pulls in
# light and dark, and a Perfetto trace of the light pulls. Run under the adb lock:
#   $L adb ptr_trace.sh <serial> <apk> <package> <out-dir>
set -uo pipefail
S=$1; APK=$2; PKG=$3; OUT=$4
A="adb -s $S"; mkdir -p "$OUT"
$A install -r -d "$APK" | tail -1
read W H < <($A shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
X=$((W/2)); Y0=$((H*45/100))
ACT=$($A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "$PKG" | tail -1 | tr -d '\r')
$A shell input keyevent KEYCODE_WAKEUP
$A shell cmd package compile -m speed -f "$PKG" >/dev/null
launch() {
  $A shell cmd statusbar collapse
  $A shell input keyevent KEYCODE_HOME
  $A shell am force-stop "$PKG"
  $A shell am start -W -n "$ACT" >/dev/null 2>&1
  sleep 5
}
pull() {  # $1 = distance px, $2 = steps
  $A shell input motionevent DOWN $X $Y0
  for i in $(seq 1 "$2"); do $A shell input motionevent MOVE $X $((Y0 + $1 * i / $2)); sleep 0.03; done
  sleep 0.8
  $A shell input motionevent UP $X $((Y0 + $1))
  sleep 2.5
}
pulls() { pull 700 30; pull 700 30; pull 260 12; }
measure() {
  local name=$1
  $A shell dumpsys gfxinfo "$PKG" reset >/dev/null
  pulls
  $A shell dumpsys gfxinfo "$PKG" > "$OUT/$name.gfxinfo.txt"
  $A exec-out screencap -p > "$OUT/$name-rest.png"
}
$A shell cmd uimode night no; launch
$A exec-out screencap -p > "$OUT/light-launch.png"
measure light
$A shell cmd uimode night yes; sleep 2; launch
measure dark
$A shell cmd uimode night no; sleep 2; launch

cat > "$OUT/trace.pbtxt" <<EOF
buffers { size_kb: 65536 fill_policy: RING_BUFFER }
duration_ms: 16000
data_sources { config { name: "linux.ftrace" ftrace_config {
  ftrace_events: "sched/sched_switch" ftrace_events: "sched/sched_waking"
  ftrace_events: "power/cpu_frequency" ftrace_events: "power/gpu_frequency"
  atrace_categories: "gfx" atrace_categories: "view" atrace_categories: "input"
  atrace_apps: "$PKG" } } }
data_sources { config { name: "android.surfaceflinger.frametimeline" } }
data_sources { config { name: "linux.process_stats" process_stats_config { scan_all_processes_on_start: true } } }
EOF
$A push "$OUT/trace.pbtxt" /data/local/tmp/ptr.pbtxt >/dev/null
$A shell rm -f /data/misc/perfetto-traces/ptr.perfetto-trace
$A shell "cat /data/local/tmp/ptr.pbtxt | perfetto --txt -c - -o /data/misc/perfetto-traces/ptr.perfetto-trace" >/dev/null 2>&1 &
TP=$!
sleep 2
pulls
wait $TP
$A pull /data/misc/perfetto-traces/ptr.perfetto-trace "$OUT/ptr.perfetto-trace" >/dev/null && echo "traced"
echo PTR-TRACE DONE

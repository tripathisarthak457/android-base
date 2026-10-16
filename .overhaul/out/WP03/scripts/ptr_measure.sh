#!/usr/bin/env bash
# Frame statistics of pull to refresh on a phone, with continuous input as a finger gives it
# (`input swipe` streams moves; one adb call per move would draw one isolated frame per call).
# Light and dark, each: two long pulls that refresh, one short pull that does not, one quick flick.
# Optionally a Perfetto trace of the light set. Run under the adb lock:
#   $L adb ptr_measure.sh <serial> <package> <out-dir> [apk] [trace]
set -uo pipefail
S=$1; PKG=$2; OUT=$3; APK=${4:-}; TRACE=${5:-}
A="adb -s $S"; mkdir -p "$OUT"
[ -n "$APK" ] && $A install -r -d "$APK" | tail -1
read W H < <($A shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
X=$((W/2)); Y0=$((H*40/100))
ACT=$($A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "$PKG" | tail -1 | tr -d '\r')
EXTRAS=""
case "$PKG" in *wp04.p2*) EXTRAS="--es perf.fakeSession true" ;; esac
$A shell input keyevent KEYCODE_WAKEUP
$A shell cmd package compile -m speed -f "$PKG" >/dev/null
launch() {
  $A shell cmd statusbar collapse
  $A shell input keyevent KEYCODE_HOME
  $A shell am force-stop "$PKG"
  $A shell am start -W -n "$ACT" $EXTRAS >/dev/null 2>&1
  sleep 5
}
pulls() {
  $A shell input swipe $X $Y0 $X $((Y0 + 800)) 1400; sleep 3
  $A shell input swipe $X $Y0 $X $((Y0 + 800)) 1000; sleep 3
  $A shell input swipe $X $Y0 $X $((Y0 + 260)) 600; sleep 1.5
  $A shell input swipe $X $Y0 $X $((Y0 + 500)) 120; sleep 3
}
measure() {
  local name=$1
  $A shell dumpsys gfxinfo "$PKG" reset >/dev/null
  pulls
  $A shell dumpsys gfxinfo "$PKG" > "$OUT/$name.gfxinfo.txt"
  $A exec-out screencap -p > "$OUT/$name-rest.png"
}
$A shell cmd uimode night no; launch
measure light
$A shell cmd uimode night yes; sleep 2; launch
measure dark
$A shell cmd uimode night no; sleep 2

if [ -n "$TRACE" ]; then
  launch
  cat > "$OUT/trace.pbtxt" <<EOF
buffers { size_kb: 65536 fill_policy: RING_BUFFER }
duration_ms: 18000
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
  tp=$!
  sleep 2
  pulls
  wait $tp
  $A pull /data/misc/perfetto-traces/ptr.perfetto-trace "$OUT/ptr.perfetto-trace" >/dev/null && echo "traced"
fi
echo PTR-MEASURE DONE

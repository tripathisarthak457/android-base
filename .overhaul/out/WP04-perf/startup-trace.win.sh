#!/usr/bin/env bash
# A Perfetto capture of three cold starts of the installed everything APK (as perf.sh measures them: the process
# killed, then `am start -W`), with sched, cpufreq, binder and the app's own sections, for reading where the
# start's time goes. Run under the adb lock:  $L adb ./startup-trace.win.sh <out-dir> [package]
set -uo pipefail
# Device paths (/data, /sdcard) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"
mkdir -p "$out"
# The package's launcher activity, so the old template (another package and activity) is traced the same way.
comp=$($A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | tr -d '\r' | tail -1)
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
cat > "$out/config.pbtxt" <<CFG
buffers { size_kb: 131072 fill_policy: RING_BUFFER }
data_sources { config { name: "android.surfaceflinger.frametimeline" } }
data_sources { config { name: "linux.process_stats" target_buffer: 0 process_stats_config { scan_all_processes_on_start: true } } }
data_sources { config { name: "linux.sys_stats" sys_stats_config { cpufreq_period_ms: 10 } } }
data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      ftrace_events: "sched/sched_switch"
      ftrace_events: "sched/sched_wakeup"
      ftrace_events: "sched/sched_process_exec"
      ftrace_events: "task/task_newtask"
      ftrace_events: "power/cpu_frequency"
      ftrace_events: "binder/binder_transaction"
      ftrace_events: "binder/binder_transaction_received"
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "am"
      atrace_categories: "wm"
      atrace_categories: "dalvik"
      atrace_categories: "binder_driver"
      atrace_apps: "$pkg"
    }
  }
}
duration_ms: 20000
CFG
$A shell "perfetto --txt -c - -o /data/misc/perfetto-traces/startup.perfetto-trace" < "$out/config.pbtxt" > "$out/perfetto.log" 2>&1 &
sleep 2
for i in 1 2 3; do
  $A shell am force-stop $pkg; sleep 1.5
  $A shell am start -W -n "$comp" --es perf.fakeSession true | grep -E 'TotalTime|WaitTime' | tr '\n' ' '; echo
  sleep 3
done
wait
$A pull /data/misc/perfetto-traces/startup.perfetto-trace "$out/trace.perfetto-trace" >/dev/null 2>&1
$A shell rm -f /data/misc/perfetto-traces/startup.perfetto-trace
ls -la "$out/trace.perfetto-trace"
echo STARTUP-TRACE DONE

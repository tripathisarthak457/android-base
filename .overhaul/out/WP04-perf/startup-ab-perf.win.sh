#!/usr/bin/env bash
# startup-ab-trace.win.sh with CPU callstack samples (traced_perf, linux.perf) of both apps: cold starts of two
# installed apps alternated in one capture, so the main thread's extra time in one of them can be put on the code
# that spends it. Both apps must be profileable (benchmarkRelease is). Run under the adb lock:
#   $L adb ./startup-ab-perf.win.sh <out-dir> <rounds> <package A> <package B> [sampling Hz, default 500]
set -uo pipefail
# Device paths (/data, /sdcard) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
out=$1; rounds=$2; a=$3; b=$4; hz=${5:-500}; S=9a7c74f4
A="adb -s $S"
mkdir -p "$out"
comp() { $A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $1 | tr -d '\r' | tail -1; }
ca=$(comp $a); cb=$(comp $b)
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
dur=$(( (rounds * 2 * 6 + 8) * 1000 ))
cat > "$out/config.pbtxt" <<CFG
buffers { size_kb: 262144 fill_policy: RING_BUFFER }
data_sources { config { name: "linux.process_stats" target_buffer: 0 process_stats_config { scan_all_processes_on_start: true } } }
data_sources { config { name: "linux.sys_stats" sys_stats_config { cpufreq_period_ms: 10 } } }
data_sources {
  config {
    name: "linux.perf"
    perf_event_config {
      timebase { frequency: $hz }
      callstack_sampling {
        scope { target_cmdline: "$a" target_cmdline: "$b" }
        kernel_frames: false
      }
    }
  }
}
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
      atrace_categories: "view"
      atrace_categories: "am"
      atrace_categories: "dalvik"
      atrace_apps: "$a"
      atrace_apps: "$b"
    }
  }
}
duration_ms: $dur
CFG
$A shell "perfetto --txt -c - -o /data/misc/perfetto-traces/startabperf.perfetto-trace" < "$out/config.pbtxt" > "$out/perfetto.log" 2>&1 &
sleep 3
: > "$out/totals.tsv"
for i in $(seq 1 $rounds); do
  for c in "$ca" "$cb"; do
    pkg=${c%%/*}
    $A shell am force-stop $pkg; sleep 1.5
    t=$($A shell am start -W -n "$c" --es perf.fakeSession true | tr -d '\r' | awk '/TotalTime/{print $2}')
    echo -e "$i\t$pkg\t$t" | tee -a "$out/totals.tsv"
    sleep 3
    $A shell input keyevent KEYCODE_HOME; sleep 1
  done
done
wait
$A pull /data/misc/perfetto-traces/startabperf.perfetto-trace "$out/trace.perfetto-trace" >/dev/null 2>&1
$A shell rm -f /data/misc/perfetto-traces/startabperf.perfetto-trace
ls -la "$out/trace.perfetto-trace"
cat "$out/perfetto.log" | tail -5
echo STARTUP-AB-PERF DONE

#!/usr/bin/env bash
# Where the native heap of perf.sh's journey goes (handoff §2c: 27.8 MB after the journey against 13.9 MB in a
# restored process). heapprofd samples every native allocation of the package's processes (it attaches to each one
# that starts while the session runs; the benchmark build is profileable) while perf.sh runs the journey once, the
# scenarios measure.win.sh runs up to meminfo; the session is stopped after perf.sh, which makes heapprofd dump what
# each live process still holds. Under the adb lock, from the build's folder:
#   $L adb ./heapprof.win.sh <out-dir> <build>
set -uo pipefail
export MSYS_NO_PATHCONV=1
out=$1; n=$2; S=9a7c74f4; A="adb -s $S"
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; T=$OV/wt/WP04-perf/templates/android/tools/perf
mkdir -p "$out"
pkg=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['applicationId'])" \
  $O/$n/app/build/outputs/apk/dev/benchmarkRelease/output-metadata.json)
cat > "$out/config.pbtxt" <<CFG
buffers { size_kb: 262144 fill_policy: DISCARD }
data_sources {
  config {
    name: "android.heapprofd"
    target_buffer: 0
    heapprofd_config {
      sampling_interval_bytes: 4096
      process_cmdline: "$pkg"
      shmem_size_bytes: 8388608
      block_client: true
    }
  }
}
data_sources { config { name: "linux.process_stats" target_buffer: 0 } }
duration_ms: 900000
CFG
$A shell "perfetto --txt -c - -o /data/misc/perfetto-traces/heapprof.perfetto-trace --background-wait" < "$out/config.pbtxt" > "$out/perfetto.log" 2>&1
pid=$(tr -dc '0-9' < "$out/perfetto.log" | tail -c 10)
echo "perfetto pid $pid"
cd $O/$n
$T/perf.sh --package $pkg --variant devBenchmarkRelease --runs 1 --serial $S --out "$out/perf" \
  tabs tabs_tap tab1 pushpop pushpop_gesture rapid_back scroll_root scroll_minimize meminfo
echo "perf.sh exit $?"
$A shell kill -TERM $pid
# Wait for perfetto to have written the trace and exited: adb shell's exit status is not reliable here, so the
# process list is read instead.
for i in $(seq 1 90); do $A shell ps -A -o PID | tr -d ' \r' | grep -qx "$pid" || break; sleep 2; done
$A pull /data/misc/perfetto-traces/heapprof.perfetto-trace "$out/trace.perfetto-trace" > /dev/null 2>&1
$A shell rm -f /data/misc/perfetto-traces/heapprof.perfetto-trace
ls -la "$out/trace.perfetto-trace"
echo HEAPPROF DONE

#!/usr/bin/env bash
# After phase 18: heapprofd over perf.sh's journey on dlplay (HEAD's tree), for §2c's unattributed native heap.
#   ./phase19.win.sh > logs/phase19-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
true
echo "== $(date '+%F %H:%M:%S') phase 19"
$L adb bash -c "$O/heapprof.win.sh $P/dlplay-heapprof2 dlplay"
echo "== $(date '+%F %H:%M:%S') PHASE19 DONE"

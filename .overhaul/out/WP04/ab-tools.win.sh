#!/usr/bin/env bash
# Run 24: the standard build of 7fa49d5 measured with the perf tools as of 7fa49d5 less the warm-up's back swipe
# (0caab64). With ab-standard-measure.win.sh (both builds, the tools as they are) this separates the swipe's share
# of standard's PSS rise (phase3c-standard 41.7 MB, final-standard 50.4 MB) from the code's.
#   ./ab-tools.win.sh > logs/r24-ab-tools.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04; P=$OV/perf/WP04
T=$O/ab-tools-noswipe/perf
rm -rf $O/ab-tools-noswipe; mkdir -p $O/ab-tools-noswipe
git -C $OV/wt/WP04 archive 7fa49d5 templates/android/tools/perf | tar -x -C $O/ab-tools-noswipe --strip-components=3
python3 - $T/perf.py <<'PY'
import sys
path = sys.argv[1]
source = open(path, encoding="utf-8").read()
swipe = """        width, height = self.device.size
        self.device.shell(self.tap(row))
        time.sleep(1.3)
        gfx, _ = self.segment(f"first_gesture{suffix}", gesture_swipe(width, height), 1.3)
        self.record(f"first_gesture{suffix}.janky", gfx["janky"])
"""
assert source.count(swipe) == 1, "the warm-up swipe is not where 0caab64 put it"
open(path, "w", encoding="utf-8", newline="\n").write(source.replace(swipe, ""))
PY
$L adb bash -c '
set -euo pipefail
out='"$P"'/ab-standard-7fa49d5-noswipe
if [ -e $out ]; then mv $out $out-before-$(date +%H%M%S); fi
cd '"$O"'/final-standard
'"$T"'/perf.sh --serial 9a7c74f4 --runs 3 --out $out --package com.example.wp04.standard.dev --variant devRelease \
  tabs pushpop scroll_root idle coldstart meminfo || echo "perf.sh exit $?"
echo "== $(date +%H:%M:%S) measured"'
echo AB-TOOLS DONE

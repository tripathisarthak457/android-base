#!/usr/bin/env bash
# Run 24: the measuring half of ab-standard.sh (its build of d10ef94 finished at 02:59 on Oct 18; the adb lock went
# to WP04-perf and the run was cut off). Measures the phase 3c standard build and the 7fa49d5 one in one adb hold
# with the scenarios the standard gate runs before meminfo, so the PSS row compares code on the same phone state.
#   ./ab-standard-measure.win.sh > logs/r24-ab-standard.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04; P=$OV/perf/WP04
T=$O/ab-tools/perf
# The tools as of 7fa49d5, so later edits in the worktree cannot change what the two builds are measured with.
rm -rf $O/ab-tools; mkdir -p $O/ab-tools
git -C $OV/wt/WP04 archive 7fa49d5 templates/android/tools/perf | tar -x -C $O/ab-tools --strip-components=3
$L adb bash -c '
set -euo pipefail
for d in "$@"; do
  set -- $d
  if [ -e '"$P"'/$3 ]; then mv '"$P"'/$3 '"$P"'/$3-before-$(date +%H%M%S); fi
  (cd $1 && '"$T"'/perf.sh --serial 9a7c74f4 --runs 3 --out '"$P"'/$3 --package $2 --variant devRelease \
    tabs pushpop scroll_root idle coldstart meminfo) || echo "perf.sh exit $? for $3"
  echo "== $(date +%H:%M:%S) measured $3"
done' _ "$O/ab-standard-d10ef94 com.example.wp04.abstd.dev ab-standard-d10ef94" \
  "$O/final-standard com.example.wp04.standard.dev ab-standard-7fa49d5"
echo AB DONE

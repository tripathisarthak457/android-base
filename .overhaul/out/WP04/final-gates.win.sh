#!/usr/bin/env bash
# accept.win.sh's device gates in one adb lock hold: solid on final-everything, standard and lean on their presets.
# perf.sh gates every run against the template's gates.toml (the release gate; its failing rows stay in each
# summary.md), then the merge passes on gates-merge.toml (make-merge-gates.py, the handoff's Deviation 28).
#   $L adb ./final-gates.win.sh > logs/<step>.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04
T=$OV/wt/WP04/templates/android/tools/perf; S=9a7c74f4
python3 $O/make-merge-gates.py $T/gates.toml $O/gates-merge.toml
for d in final final-standard final-lean; do
  if [ -e $P/$d ]; then mv $P/$d $P/$d-before-$(date +%H%M%S); fi
done
# perf.sh exits 1 when a release row fails (the summary is written) and 2 or more when the harness itself fails.
gate() {
  local out=$1 column=$2; shift 2
  local rc=0
  $T/perf.sh --serial $S --runs 3 --out $P/$out --gate $column "$@" || rc=$?
  echo "== $(date +%H:%M:%S) $out: release gate exit $rc"
  test $rc -le 1
  python3 $T/check_gates.py --gate $column --gates $O/gates-merge.toml $P/$out/summary.json
  echo "== $(date +%H:%M:%S) $out: merge gate passed"
}
cd $O/final-everything
gate final solid --package com.example.wp04.everything.dev --variant devBenchmarkRelease \
  tabs tabs_tap tab1 pushpop pushpop_gesture back_during_push rapid_back scroll_root idle nav_stress restore meminfo \
  scroll_minimize recompose coldstart coldstart_installed
# standard and lean do not ship tools/perf; ABI splits mean --variant picks the phone's APK.
cd $O/final-standard
gate final-standard standard --package com.example.wp04.standard.dev --variant devRelease \
  tabs pushpop scroll_root idle coldstart meminfo coldstart_installed
cd $O/final-lean
gate final-lean lean --package com.example.wp04.lean.dev --variant devRelease \
  tabs pushpop idle coldstart coldstart_installed
echo FINAL-GATES OK

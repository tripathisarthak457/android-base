#!/usr/bin/env bash
# One adb hold: the full timing scenarios on the given builds, each followed by a Perfetto capture of tab switches
# from idle (dvfs-trace.sh). Builds are out/WP04-perf/<name>; results go to perf/WP04-perf/<name>.
#   $L adb ./measure.sh <name>... > logs/<step>.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf
T=$OV/wt/WP04-perf/templates/android/tools/perf; S=9a7c74f4
runs=${RUNS:-3}
scenarios=${SCENARIOS:-"tabs tabs_tap tab1 pushpop pushpop_gesture rapid_back scroll_root scroll_minimize meminfo recompose coldstart coldstart_installed"}
step() { echo "== $(date +%H:%M:%S) $*"; "$@"; echo "== exit $? : $1"; }
for n in "$@"; do
  cd $O/$n || continue
  echo "== $n built from $(cat BUILT_FROM) ($(wc -l < BUILT_DIRTY) dirty)"
  step $T/perf.sh --package com.example.wp04.everything.dev --variant devBenchmarkRelease --runs $runs --serial $S \
    --out $P/$n --gate solid $scenarios
  step $O/dvfs-trace.sh $P/$n-trace
done
echo MEASURE DONE

#!/usr/bin/env bash
# Windows port of measure.sh, in one adb hold: perf.sh --runs 3 with the worktree's perf tooling on each build
# (out/WP04-perf/<name>, all with the everything preset's package), then dvfs-trace.win.sh's Perfetto capture of
# tab switches from idle. A harness failure (exit 2 or more, such as uiautomator refusing to dump a busy screen)
# is retried once into a fresh folder; a release-gate failure (exit 1) is a result, not a retry. TAG=<suffix> puts a
# build's results in perf/WP04-perf/<name><suffix>, beside an earlier measurement of the same build; NOTRACE=1 skips the
# Perfetto capture.
#   $L adb ./measure.win.sh <name>... > logs/<step>.log 2>&1
set -uo pipefail
export MSYS_NO_PATHCONV=1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf
T=$OV/wt/WP04-perf/templates/android/tools/perf; S=9a7c74f4
runs=${RUNS:-3}
scenarios=${SCENARIOS:-"tabs tabs_tap tab1 pushpop pushpop_gesture rapid_back scroll_root scroll_minimize meminfo recompose coldstart coldstart_installed"}
for n in "$@"; do
  cd $O/$n || continue
  echo "== $(date +%H:%M:%S) $n built from $(cat BUILT_FROM) ($(cat BUILT_DIRTY))"
  for attempt in 1 2; do
    out=$P/$n${TAG:-}; [ $attempt = 2 ] && out=$P/$n${TAG:-}-retry
    rc=0
    # shellcheck disable=SC2086
    $T/perf.sh --package com.example.wp04.everything.dev --variant devBenchmarkRelease --runs $runs --serial $S \
      --out $out --gate solid $scenarios || rc=$?
    echo "== $(date +%H:%M:%S) $n attempt $attempt: perf.sh exit $rc"
    [ $rc -le 1 ] && break
  done
  [ -n "${NOTRACE:-}" ] && continue
  $O/dvfs-trace.win.sh $P/$n${TAG:-}-trace
  echo "== $(date +%H:%M:%S) $n: trace done"
done
echo MEASURE DONE

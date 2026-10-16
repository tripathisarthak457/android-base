#!/usr/bin/env bash
# Phase 3 gates on the phone, in one adb lock hold: solid on final-everything, standard and lean on their
# presets, with the worktree's tools/perf (as accept.sh runs them).
#   $L adb ./phone-gates.sh <name> [presets...] > logs/<step>.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04
T=$OV/wt/WP04/templates/android/tools/perf; S=9a7c74f4
name=${1:-phase3b}; shift || true
presets=("${@:-everything standard lean}")
read -r -a presets <<< "${presets[*]}"
step() { echo "== $(date +%H:%M:%S) $*"; "$@"; echo "== exit $? : $1"; }
for n in "${presets[@]}"; do
  cd $O/final-$n
  case $n in
    everything) step $T/perf.sh --package com.example.wp04.everything.dev --variant devBenchmarkRelease --runs 3 --serial $S \
      --out $P/$name --gate solid \
      tabs tabs_tap tab1 pushpop pushpop_gesture back_during_push rapid_back scroll_root idle nav_stress restore meminfo \
      scroll_minimize recompose coldstart coldstart_installed ;;
    standard) step $T/perf.sh --package com.example.wp04.standard.dev --variant devRelease --runs 3 --serial $S \
      --out $P/$name-standard --gate standard tabs pushpop scroll_root idle coldstart meminfo coldstart_installed ;;
    lean) step $T/perf.sh --package com.example.wp04.lean.dev --variant devRelease --runs 3 --serial $S \
      --out $P/$name-lean --gate lean tabs pushpop idle coldstart coldstart_installed ;;
  esac
done
if [ "${SPEED:-0}" = 1 ]; then
  cd $O/final-everything
  step $T/perf.sh --package com.example.wp04.everything.dev --variant devBenchmarkRelease --runs 3 --serial $S \
    --compile speed --out $P/$name-speed pushpop tabs tab1
fi
echo PHONE-GATES DONE

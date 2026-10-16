#!/usr/bin/env bash
# Run 16: everything at HEAD (the shipped profile), then the solid and lean gates and a Perfetto capture on the phone.
#   ./r16-chain.sh > logs/r16-chain.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04; L=$OV/bin/locked
T=$OV/wt/WP04/templates/android/tools/perf
cd $O
./r14-build.sh everything > logs/r16-build.log 2>&1 || { echo "build failed: logs/r16-build.log"; exit 1; }
echo "== $(date +%H:%M:%S) built $(cat final-everything/BUILT_FROM)"
$L adb ./phone-gates.sh phase3d everything lean > logs/r16-gates.log 2>&1
echo "== $(date +%H:%M:%S) gates exit $?"
cd $O/final-everything
$L adb $T/perf.sh --package com.example.wp04.everything.dev --variant devBenchmarkRelease --runs 1 --serial 9a7c74f4 \
  --out $P/r16-trace trace > $O/logs/r16-trace.log 2>&1
echo "== $(date +%H:%M:%S) trace exit $?"
echo R16-CHAIN DONE

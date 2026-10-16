#!/usr/bin/env bash
# Before/after on the emulator (relative evidence only, never gated): the pre-rewrite baseline
# project and the phase 2 project have the same spec, so the first-frame rows compare like for like.
# Run under one adb lock: $L adb ./emu-compare.sh
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; O=$OV/out/WP04; P=$OV/perf/WP04
T=$W/templates/android/tools/perf; S=emulator-5554
(cd $O/base && $T/perf.sh --package com.example.wp04.base.dev --variant devBenchmarkRelease --runs 3 --serial $S \
  --out $P/emulator-baseline tabs pushpop pushpop_gesture rapid_back idle; echo "BASE EXIT=$?")
(cd $O/p2 && $T/perf.sh --package com.example.wp04.p2.dev --variant devBenchmarkRelease --runs 3 --serial $S \
  --out $P/emulator-p2-3runs tabs pushpop pushpop_gesture rapid_back idle; echo "P2 EXIT=$?")
echo EMU-COMPARE DONE

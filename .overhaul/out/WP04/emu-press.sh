#!/usr/bin/env bash
# Emulator, relative only: tabs and push/pop measured from the finger's lift (perf.py press()), so
# the old template's press overlay frame is not taken for the first frame of the push or switch.
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; O=$OV/out/WP04; P=$OV/perf/WP04
T=$W/templates/android/tools/perf; S=emulator-5554
(cd $O/base && $T/perf.sh --package com.example.wp04.base.dev --variant devBenchmarkRelease --runs 3 --serial $S \
  --out $P/emulator-baseline-lift tabs pushpop; echo "BASE EXIT=$?")
(cd $O/p2 && $T/perf.sh --package com.example.wp04.p2.dev --variant devBenchmarkRelease --runs 3 --serial $S \
  --out $P/emulator-p2-lift tabs pushpop; echo "P2 EXIT=$?")
echo EMU-PRESS DONE

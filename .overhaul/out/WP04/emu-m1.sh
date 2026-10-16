#!/usr/bin/env bash
# Emulator, correctness only: the merged tree (p2 at HEAD) with M1's related rows, so rapid_back runs at depth 4.
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; O=$OV/out/WP04; P=$OV/perf/WP04
T=$W/templates/android/tools/perf; S=emulator-5554
adb -s 9a7c74f4 shell dumpsys trust | grep -o 'deviceLocked=[0-9]' || true
(cd $O/p2 && $T/perf.sh --package com.example.wp04.p2.dev --variant devBenchmarkRelease --runs 1 --serial $S \
  --out $P/emulator-m1 tabs pushpop rapid_back idle nav_stress restore; echo "P2 EXIT=$?")
$T/record.sh com.example.wp04.p2.dev rapid_back $P/emulator-m1/frames/rapid_back --serial $S
echo EMU-M1 DONE

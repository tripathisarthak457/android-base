#!/usr/bin/env bash
# Emulator pass (correctness only, never gated): the shell scenarios on the current p2 build, then
# the checkpoint 1 recordings. The phone is locked, so these stand in until it can be used.
# Run under one adb lock: $L adb ./emu-r3.sh
set -euo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; O=$OV/out/WP04; P=$OV/perf/WP04
T=$W/templates/android/tools/perf; S=emulator-5554; PKG=com.example.wp04.p2.dev
cd $O/p2
$T/perf.sh --package $PKG --variant devBenchmarkRelease --runs 1 --serial $S --out $P/emulator-r3 \
  tabs pushpop pushpop_gesture rapid_back idle nav_stress restore || echo "PERF EXIT=$?"
C=$P/cp1-emulator
for r in 0.24 0.30 0.36; do
  $T/record.sh $PKG pushpop $C/pushpop_$r --extra perf.screenResponse=$r --serial $S
done
$T/record.sh $PKG tab_switch $C/tab_switch --serial $S
$T/record.sh $PKG gesture_pop $C/gesture_pop --serial $S
$T/record.sh $PKG rapid_back $C/rapid_back --serial $S
$T/record.sh $PKG tab_switch $C/bar_labels_selected --extra perf.barLabels=Selected --serial $S
$T/record.sh $PKG tab_switch $C/bar_labels_never --extra perf.barLabels=Never --serial $S
$T/record.sh $PKG custom $C/bar_minimize --extra perf.barMinimize=OnScrollDown --serial $S \
  --prepare 'sleep 3' --actions 'input swipe 540 1800 540 700 140; sleep 1.5; input swipe 540 700 540 1800 140'
echo EMU-R3 OK

#!/usr/bin/env bash
# Everything WP04 still needs from the phone before phase 3, in one adb lock hold:
#   $L adb ./phone-pending.sh > logs/phone-pending.log 2>&1
# 1. the phase 1 baseline (pre-rewrite tree), 2. the phase 2 numbers and the solid-gate table
# (spike outcome: pop first frame), 3. the checkpoint 1 recordings. perf.sh refuses a locked phone.
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; O=$OV/out/WP04; P=$OV/perf/WP04
T=$W/templates/android/tools/perf; S=9a7c74f4
step() { echo "== $*"; "$@"; echo "== exit $? : $1 $2 $3"; }

cd $O/base
step $T/perf.sh --package com.example.wp04.base.dev --variant devBenchmarkRelease --runs 3 --serial $S --out $P/baseline \
  tabs tab1 pushpop pushpop_gesture rapid_back scroll_root idle coldstart meminfo
step $T/record.sh com.example.wp04.base.dev rapid_back $P/baseline/frames/rapid_back --serial $S
cd $O/base-standard
step $T/perf.sh --package com.example.wp04.basestandard.dev --variant devRelease --runs 3 --serial $S --out $P/baseline-standard \
  tabs pushpop coldstart
cd $O/base-lean
step $T/perf.sh --package com.example.wp04.baselean.dev --variant devRelease --runs 3 --serial $S --out $P/baseline-lean \
  tabs pushpop coldstart

cd $O/p2
step $T/perf.sh --package com.example.wp04.p2.dev --variant devBenchmarkRelease --runs 3 --serial $S --out $P/phase2 --gate solid \
  tabs tab1 pushpop pushpop_gesture rapid_back nav_stress restore scroll_root scroll_minimize idle coldstart meminfo coldstart_installed
C=$P/cp1
PKG=com.example.wp04.p2.dev
for r in 0.24 0.30 0.36; do
  step $T/record.sh $PKG push $C/push_$r --extra perf.screenResponse=$r --serial $S
  step $T/record.sh $PKG pop $C/pop_$r --extra perf.screenResponse=$r --serial $S
done
step $T/record.sh $PKG tab_switch $C/tab_switch --serial $S
step $T/record.sh $PKG gesture_pop $C/gesture_pop --serial $S
step $T/record.sh $PKG rapid_back $C/rapid_back --serial $S
step $T/record.sh $PKG tab_switch $C/bar_labels_selected --extra perf.barLabels=Selected --serial $S
step $T/record.sh $PKG tab_switch $C/bar_labels_never --extra perf.barLabels=Never --serial $S
step $T/record.sh $PKG custom $C/bar_minimize --extra perf.barMinimize=OnScrollDown --serial $S \
  --prepare 'sleep 3' --actions 'input swipe 540 1800 540 700 140; sleep 1.5; input swipe 540 700 540 1800 140'
echo PHONE-PENDING DONE

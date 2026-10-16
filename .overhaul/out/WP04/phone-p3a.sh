#!/usr/bin/env bash
# Phase 3, first pass on the phone, in one adb lock hold: the solid gate on the everything build at
# the branch head (p3), then the recordings requirement 24, 28 and 32 ask for.
#   $L adb ./phone-p3a.sh > logs/r7-phone-p3a.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04
T=$O/p3/tools/perf; S=9a7c74f4; PKG=com.example.wp04.p3.dev
step() { echo "== $*"; "$@"; echo "== exit $? : $1 $2 $3"; }

cd $O/p3
step $T/perf.sh --package $PKG --variant devBenchmarkRelease --runs 3 --serial $S --out $P/phase3a --gate solid \
  tabs tabs_tap tab1 pushpop pushpop_gesture back_during_push rapid_back scroll_root idle nav_stress restore meminfo \
  scroll_minimize recompose coldstart coldstart_installed

R=$P/predictive-back
for j in back_slow_commit back_slow_cancel back_fast_flick back_y_follow back_to_home gesture_pop; do
  step $T/record.sh $PKG $j $R/$j --serial $S
done
step $T/record.sh $PKG back_during_push $P/back-during-push/right --serial $S
step $T/record.sh $PKG tab_switch $P/tab-switch/light --serial $S
step $T/record.sh $PKG push $P/phase3a-frames/push --serial $S
step $T/record.sh $PKG pop $P/phase3a-frames/pop --serial $S
step $T/record.sh $PKG rapid_back $P/phase3a-frames/rapid_back --serial $S
step $T/record.sh $PKG custom $P/phase3a-frames/bar_minimize --extra perf.barMinimize=OnScrollDown --serial $S \
  --actions 'input swipe 540 1800 540 700 140; sleep 1.5; input swipe 540 700 540 1800 140'
echo PHONE-P3A DONE

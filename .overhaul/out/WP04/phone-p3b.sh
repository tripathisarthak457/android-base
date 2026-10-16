#!/usr/bin/env bash
# Phase 3 on the phone, in one adb lock hold: the solid gate on final-everything at the branch head,
# then the recordings requirements 24, 28 and 32 ask for. Uses the worktree's tools/perf.
#   $L adb ./phone-p3b.sh <out name> > logs/<step>.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04
T=$OV/wt/WP04/templates/android/tools/perf; S=9a7c74f4; PKG=com.example.wp04.everything.dev
name=${1:-phase3b}
step() { echo "== $*"; "$@"; echo "== exit $? : $1 $2 $3"; }

cd $O/final-everything
step $T/perf.sh --package $PKG --variant devBenchmarkRelease --runs 3 --serial $S --out $P/$name --gate solid \
  tabs tabs_tap tab1 pushpop pushpop_gesture back_during_push rapid_back scroll_root idle nav_stress restore meminfo \
  scroll_minimize recompose coldstart coldstart_installed

if [ "${RECORD:-1}" = 1 ]; then
  R=$P/predictive-back
  for j in back_slow_commit back_slow_cancel back_fast_flick back_y_follow back_to_home gesture_pop; do
    rm -rf $R/$j; step $T/record.sh $PKG $j $R/$j --serial $S
  done
  rm -rf $R/back_slow_commit_right; step $T/record.sh $PKG back_slow_commit $R/back_slow_commit_right --edge right --serial $S
  for e in left right; do
    rm -rf $P/back-during-push/$e; step $T/record.sh $PKG back_during_push $P/back-during-push/$e --edge $e --serial $S
  done
  rm -rf $P/tab-switch/light $P/tab-switch/dark
  step $T/record.sh $PKG tab_switch $P/tab-switch/light --serial $S
  step $T/record.sh $PKG tab_switch $P/tab-switch/dark --extra perf.theme=dark --serial $S
  for j in push pop pushpop rapid_back; do
    rm -rf $P/$name-frames/$j; step $T/record.sh $PKG $j $P/$name-frames/$j --serial $S
  done
  rm -rf $P/$name-frames/bar_minimize
  step $T/record.sh $PKG custom $P/$name-frames/bar_minimize --extra perf.barMinimize=OnScrollDown --serial $S \
    --actions 'input swipe 540 1800 540 700 140; sleep 1.5; input swipe 540 700 540 1800 140'
fi
echo PHONE-P3B DONE

#!/usr/bin/env bash
# Experiment on the phone, in one adb lock hold: the copy without AppScaffold's fill against phase3d's numbers,
# then an overdraw screenshot of the HEAD build's first tab (the debug property is put back however it ends).
#   $L adb ./exp-run.sh > logs/exp-run.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04
T=$OV/wt/WP04/templates/android/tools/perf; S=9a7c74f4
export ANDROID_SERIAL=$S
cd $O/exp-everything
$T/perf.sh --package com.example.wp04.exp.dev --variant devBenchmarkRelease --runs 2 --serial $S \
  --out $P/exp-onefill tabs tabs_tap pushpop pushpop_gesture scroll_root
echo "== exit $? exp"
before=$(adb shell getprop debug.hwui.overdraw | tr -d '\r')
restore() { adb shell setprop debug.hwui.overdraw "${before:-false}"; echo "restored debug.hwui.overdraw=${before:-false}"; }
trap restore EXIT
mkdir -p $P/overdraw
for pkg in com.example.wp04.everything.dev com.example.wp04.exp.dev; do
  main=${pkg%.dev}.MainActivity
  adb shell setprop debug.hwui.overdraw show
  adb shell am force-stop $pkg
  adb shell am start -W -n $pkg/$main --es perf.fakeSession true >/dev/null
  sleep 4
  adb exec-out screencap -p > $P/overdraw/${pkg}.png
  adb shell setprop debug.hwui.overdraw false
  adb shell am force-stop $pkg
done
echo EXP-RUN DONE

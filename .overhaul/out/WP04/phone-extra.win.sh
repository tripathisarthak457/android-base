#!/usr/bin/env bash
# Run 21's phone steps besides the gates, each in its own adb lock hold: the REQUIREMENTS 34 and D9 "after" shots,
# the recordings (record.win.sh), and adaptive.sh on the phone (sizes with display overrides, which the
# orchestrator allowed on Oct 17 and which adaptive.py gives back; a second window; keyboard shortcuts).
#   ./phone-extra.win.sh > logs/<step>.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04; L=$OV/bin/locked
A=$OV/wt/WP04/templates/android/tools/adaptive
cd $O
# A copy, so a build of final-everything running beside these steps cannot replace the APK under them.
B=$(cat final-everything/BUILT_FROM)
mkdir -p extras; cp final-everything/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk extras/everything.apk
APK=$O/extras/everything.apk
PKG=com.example.wp04.everything.dev; ACT=com.example.wp04.everything.MainActivity
echo "== $(date +%H:%M:%S) built $B"
$L adb ./r17-req34.sh $APK $PKG $ACT $P/req34/after-$B > logs/w-req34-after.log 2>&1
echo "== $(date +%H:%M:%S) req34 $(tail -1 logs/w-req34-after.log)"
$L adb ./d9-coldstart.sh "" $PKG $ACT $P/d9/after-$B > logs/w-d9-after.log 2>&1
echo "== $(date +%H:%M:%S) d9 $(tail -1 logs/w-d9-after.log)"
$L adb ./record.win.sh $APK $P/run21-$B > logs/w-record.log 2>&1
echo "== $(date +%H:%M:%S) record $(tail -1 logs/w-record.log)"
$L adb $A/adaptive.sh --package $PKG --apk $APK --serial 9a7c74f4 --out $P/adaptive/phone-$B --allow-display-overrides \
  sizes multiinstance keyboard > logs/w-adaptive.log 2>&1
echo "== $(date +%H:%M:%S) adaptive exit $? $(tail -1 logs/w-adaptive.log)"
echo PHONE-EXTRA DONE

#!/usr/bin/env bash
# Run 20: the device-free acceptance lines at HEAD, the three presets built for the phone, then on the phone
# the REQUIREMENTS 34 and D9 "after" shots and the phase 3 gates (phase3f) for everything, standard and lean.
#   ./r20-chain.sh > logs/r20-chain.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04; L=$OV/bin/locked
cd $O
echo "== $(date +%H:%M:%S) accept-local"
./accept-local.sh > logs/r20-accept-local.log 2>&1; echo "== $(date +%H:%M:%S) accept-local exit $? $(tail -1 logs/r20-accept-local.log)"
./r14-build.sh everything standard lean > logs/r20-build.log 2>&1 || { echo "build failed: logs/r20-build.log"; exit 1; }
B=$(cat final-everything/BUILT_FROM)
echo "== $(date +%H:%M:%S) built $B dirty=$(wc -l < final-everything/BUILT_DIRTY)"
APK=final-everything/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
PKG=com.example.wp04.everything.dev; ACT=com.example.wp04.everything.MainActivity
$L adb ./r17-req34.sh $APK $PKG $ACT $P/req34/after-$B > logs/r20-req34-after.log 2>&1
echo "== $(date +%H:%M:%S) req34 $(tail -1 logs/r20-req34-after.log)"
$L adb ./d9-coldstart.sh "" $PKG $ACT $P/d9/after-$B > logs/r20-d9-after.log 2>&1
echo "== $(date +%H:%M:%S) d9 $(tail -1 logs/r20-d9-after.log)"
$L adb ./phone-gates.sh phase3f everything standard lean > logs/r20-gates.log 2>&1
echo "== $(date +%H:%M:%S) gates $(tail -1 logs/r20-gates.log)"
echo R20-CHAIN DONE

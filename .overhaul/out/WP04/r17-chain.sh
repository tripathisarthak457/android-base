#!/usr/bin/env bash
# Run 17: the three presets at HEAD, then on the phone: the REQUIREMENTS 34 shots and the D9 cold start
# (after), then the phase 3 gates (phase3f) for everything, standard and lean.
#   ./r17-chain.sh > logs/r17-chain.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04; L=$OV/bin/locked
cd $O
./r14-build.sh everything standard lean > logs/r17-build.log 2>&1 || { echo "build failed: logs/r17-build.log"; exit 1; }
echo "== $(date +%H:%M:%S) built $(cat final-everything/BUILT_FROM) dirty=$(wc -l < final-everything/BUILT_DIRTY)"
APK=final-everything/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
PKG=com.example.wp04.everything.dev; ACT=com.example.wp04.everything.MainActivity
$L adb ./r17-req34.sh $APK $PKG $ACT $P/req34/after-$(cat final-everything/BUILT_FROM) > logs/r17-req34-after.log 2>&1
echo "== $(date +%H:%M:%S) req34 $(tail -1 logs/r17-req34-after.log)"
$L adb ./d9-coldstart.sh "" $PKG $ACT $P/d9/after-$(cat final-everything/BUILT_FROM) > logs/r17-d9-after.log 2>&1
echo "== $(date +%H:%M:%S) d9 $(tail -1 logs/r17-d9-after.log)"
$L adb ./phone-gates.sh phase3f everything standard lean > logs/r17-gates.log 2>&1
echo "== $(date +%H:%M:%S) gates $(tail -1 logs/r17-gates.log)"
echo R17-CHAIN DONE

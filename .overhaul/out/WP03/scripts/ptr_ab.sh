#!/usr/bin/env bash
# Repeats the back-to-back measurement in the reverse order (current build first, then e68bbfa), so
# a drift in the phone's state over a session does not favour one build.  $L adb with_phone.sh <serial> ptr_ab.sh <serial> <round>
set -uo pipefail
S=$1; R=$2
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP03
P=/home/crazyhusir/Documents/projects/tools/.overhaul/perf/WP03/ptr
APK=app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk
bash "$O/scripts/ptr_measure.sh" "$S" com.example.wp03.ptr.dev "$P/r8-after-$R" "$O/ptr/$APK"
bash "$O/scripts/ptr_measure.sh" "$S" com.example.wp03.ptr.dev "$P/r8-head-$R" "$O/ptr0/$APK"
echo "AB ROUND $R DONE"

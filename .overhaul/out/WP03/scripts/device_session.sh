#!/usr/bin/env bash
# One adb lock hold on the phone: pull to refresh after (perf/WP03/ptr/after) and the CP1 set on
# the phone (perf/WP03/cp1-phone).  $L adb device_session.sh <serial>
set -uo pipefail
S=$1
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP03; P=$OV/perf/WP03
APK=$(find "$O/ptr/app/build/outputs/apk/dev/release" -name '*arm64*release.apk' -o -name '*universal*release.apk' -o -name 'app-dev-release.apk' | head -1)
adb -s "$S" install -r -d "$APK" | tail -1
rm -rf "$P/ptr/after"; mkdir -p "$P/ptr/after"
bash "$O/scripts/ptr_record.sh" "$S" com.example.wp03.ptr.dev "$P/ptr/after"
bash "$O/scripts/cp1_session.sh" "$S" "$P/cp1-phone"
echo DEVICE SESSION DONE

#!/usr/bin/env bash
# Installs out/WP04-perf/<name>'s everything APK as perf.sh does (install, the APK's profile, speed-profile), then
# captures dvfs-trace.sh into perf/WP04-perf/<name>-trace. Under the adb lock:  $L adb ./trace-apk.sh <name>...
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; S=9a7c74f4
pkg=com.example.wp04.everything.dev
A="adb -s $S"
for n in "$@"; do
  apk=$O/$n/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
  [ -f "$apk" ] || { echo "no apk for $n"; continue; }
  echo "== $(date +%H:%M:%S) $n ($(cat $O/$n/BUILT_FROM))"
  $A shell am force-stop $pkg
  $A install -r -t -d "$apk" > /dev/null
  $A shell pm clear $pkg > /dev/null
  $A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n $pkg/androidx.profileinstaller.ProfileInstallReceiver > /dev/null
  $A shell am force-stop $pkg
  $A shell cmd package compile -f -m speed-profile $pkg > /dev/null
  $O/dvfs-trace.sh $P/$n-trace $pkg
done
echo TRACE-APK DONE

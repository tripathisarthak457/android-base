#!/usr/bin/env bash
# For each build out/WP04-perf/<name>: installs its everything APK as perf.sh does (install, pm clear, the APK's
# profile, speed-profile), opens it once so the first run's one-off work is paid, then captures nav-trace.win.sh
# into perf/WP04-perf/<name>-nav. Under the adb lock:  $L adb ./nav-traces.win.sh <name>...
set -uo pipefail
# Device paths (/data, /sdcard) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; S=9a7c74f4
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
  $A shell am start -W -n $pkg/com.example.wp04.everything.MainActivity --es perf.fakeSession true > /dev/null
  sleep 6
  $O/nav-trace.win.sh $P/$n-nav $pkg
done
echo NAV-TRACES DONE

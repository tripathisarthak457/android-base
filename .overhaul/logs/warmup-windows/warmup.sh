#!/usr/bin/env bash
# First Gradle builds on the Windows machine, alone, before any agent runs: fills D:/.caches/gradle, proves the
# environment locked.py provides, and shows heat and memory under a real build (bin/thermlog runs beside it).
set -uo pipefail
L=D:/projects/tools/.overhaul/bin/locked
LOG=D:/projects/tools/.overhaul/logs/warmup-windows
# AGP may not install SDK packages on its own here: SDK changes go through Android Studio's SDK Manager.
NO_SDK_DOWNLOAD=-Pandroid.builder.sdkDownload=false
cd D:/projects/tools/android-base/templates/android || exit 1

step() {
  local name=$1
  shift
  echo "=== $(date +%T) $name: $*"
  "$@" >"$LOG/$name.log" 2>&1
  local rc=$?
  echo "=== $(date +%T) $name rc=$rc"
  return $rc
}

step 1-version "$L" gradle ./gradlew --version || exit 1
step 2-core-common-test "$L" gradle ./gradlew $NO_SDK_DOWNLOAD --info :core:common:test || exit 1
step 3-assemble-dev-debug "$L" gradle ./gradlew $NO_SDK_DOWNLOAD assembleDevDebug || exit 1
step 4-robolectric "$L" gradle ./gradlew $NO_SDK_DOWNLOAD :core:database:testDebugUnitTest || exit 1
echo "=== $(date +%T) all steps passed"

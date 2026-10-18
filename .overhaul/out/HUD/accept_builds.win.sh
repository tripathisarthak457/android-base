#!/usr/bin/env bash
# Every acceptance build of package HUD in one hold of the shared Gradle lock:
#   bin/locked gradle bash out/HUD/accept_builds.win.sh
# Each build has a wall-clock limit, so a stall cannot hold the lock for hours (WP06, Oct 17).
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/HUD; O=$OV/out/HUD
NO_SDK_DOWNLOAD=-Pandroid.builder.sdkDownload=false
gw() {
  local limit=$1; shift
  local log rc=0; log=$(mktemp)
  timeout --kill-after=2m "$limit" ./gradlew $NO_SDK_DOWNLOAD --console=plain "$@" 2>&1 | tee "$log" || rc=$?
  if [ $rc -eq 0 ]; then rm -f "$log"; return 0; fi
  if [ $rc -eq 124 ] || [ $rc -eq 137 ]; then rm -f "$log"; echo "TIMEOUT: gradlew $* ran past $limit"; return 1; fi
  if grep -q "OutOfMemoryError: Metaspace" "$log"; then
    rm -f "$log"; echo "RETRY: the daemon ran out of Metaspace"
    timeout --kill-after=2m "$limit" ./gradlew $NO_SDK_DOWNLOAD --console=plain "$@"; return
  fi
  rm -f "$log"; return 1
}

cd $W/templates/android
gw 80m build --continue
cd $W/generator
python create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.hud.fixture \
  --out $O/fixture-everything --no-zip --force
python create_project.py --spec tests/fixtures/v2/lean.json --set package_name=com.example.hud.lean \
  --out $O/fixture-lean --no-zip --force
(cd $O/fixture-everything && gw 45m check assembleDevDebug)
(cd $O/standard && gw 40m check assembleDevDebug)
(cd $O/fixture-lean && gw 30m assembleDevDebug)
echo BUILDS OK

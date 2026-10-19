#!/usr/bin/env bash
# Fix run 1's acceptance on the final head, in one hold of the shared Gradle lock:
#   bin/locked gradle bash out/HUD/f1_accept.win.sh
# The template's whole build (every variant, unit tests, lint, the release guards), both generator fixtures, and
# the standard preset regenerated and checked (its perfhud tests take the no-analytics branch). The phone APKs
# were copied to out/HUD/apks-f1 first, so regenerating here cannot pull them from under a phone run.
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/HUD; O=$OV/out/HUD
NO_SDK_DOWNLOAD=-Pandroid.builder.sdkDownload=false
# Each build has a wall-clock limit, so a stall cannot hold the lock for hours (WP06, Oct 17); a daemon that ran
# out of Metaspace is retried once with a fresh one.
gw() {
  local limit=$1; shift
  local log rc=0; log=$(mktemp)
  timeout --kill-after=2m "$limit" ./gradlew $NO_SDK_DOWNLOAD --console=plain "$@" 2>&1 | tee "$log" || rc=$?
  if [ $rc -eq 0 ]; then rm -f "$log"; return 0; fi
  if [ $rc -eq 124 ] || [ $rc -eq 137 ]; then rm -f "$log"; echo "TIMEOUT: gradlew $* ran past $limit"; return 1; fi
  if grep -q "Metaspace" "$log"; then
    rm -f "$log"; echo "RETRY: the daemon ran out of Metaspace"
    timeout --kill-after=2m "$limit" ./gradlew $NO_SDK_DOWNLOAD --console=plain "$@"; return
  fi
  rm -f "$log"; return 1
}

cd $W/templates/android
gw 80m build --continue
echo TEMPLATE BUILD OK
cd $W/generator
python create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.hud.fixture \
  --out $O/fixture-everything --no-zip --force
python create_project.py --spec tests/fixtures/v2/lean.json --set package_name=com.example.hud.lean \
  --out $O/fixture-lean --no-zip --force
python create_project.py --spec $O/specs/hud-standard.json --out $O/standard --no-zip --force
(cd $O/fixture-everything && gw 45m check assembleDevDebug)
echo FIXTURE EVERYTHING OK
(cd $O/standard && gw 40m check assembleDevDebug)
echo STANDARD OK
(cd $O/fixture-lean && gw 30m assembleDevDebug)
echo BUILDS OK

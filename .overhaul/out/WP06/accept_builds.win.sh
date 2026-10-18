#!/usr/bin/env bash
# accept_builds.sh on the Windows machine, every Gradle build of accept.win.sh in one hold of the shared lock:
#   $L gradle bash accept_builds.win.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP06; O=$OV/out/WP06
# SDK packages change only through Android Studio's SDK Manager on this machine, never from a build.
NO_SDK_DOWNLOAD=-Pandroid.builder.sdkDownload=false
# Two guards keep a stalled build from holding the shared lock for hours (accept14b, Oct 17: a unit test task sat idle
# from 16:18 until the run was killed at 18:45): test-timeout.init.gradle fails a test task that runs past
# WP06_TEST_TIMEOUT_MIN, and each build has a wall-clock limit after which its client is stopped, which makes the daemon
# cancel the build and stop its workers.
TIMEOUTS="-I $O/test-timeout.init.gradle"
# The shared daemon can run out of class metadata after many projects' builds and expires after that build, so a
# build that died of it runs once more on a fresh daemon. Any other failure stands.
gw() {
  local limit=$1; shift
  local log rc=0; log=$(mktemp)
  timeout --kill-after=2m "$limit" ./gradlew $NO_SDK_DOWNLOAD $TIMEOUTS "$@" 2>&1 | tee "$log" || rc=$?
  if [ $rc -eq 0 ]; then rm -f "$log"; return 0; fi
  if [ $rc -eq 124 ] || [ $rc -eq 137 ]; then rm -f "$log"; echo "TIMEOUT: gradlew $* ran past $limit"; return 1; fi
  if grep -q "OutOfMemoryError: Metaspace" "$log"; then
    rm -f "$log"; echo "RETRY: the daemon ran out of Metaspace"
    timeout --kill-after=2m "$limit" ./gradlew $NO_SDK_DOWNLOAD $TIMEOUTS "$@"; return
  fi
  rm -f "$log"; return 1
}

cd $W/templates/android
gw 75m build --continue --console=plain
gw 30m :core:network:testDebugUnitTest :core:database:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :core:devtools:testDebugUnitTest :core:applock:testDebugUnitTest :core:playstore:testDebugUnitTest \
  :core:analytics:testDebugUnitTest :data:profile:testDebugUnitTest :app:testDevDebugUnitTest --console=plain
cd $W/generator
for s in standard everything standard-nofirebase; do
  python create_project.py --spec tests/fixtures/v2/$s.json --set package_name=com.example.wp06.${s//-/} --out $O/$s --no-zip --force
  (cd $O/$s && gw 40m check assembleDevDebug --console=plain)
done
# QA-44 needs the tab root without sign-in or onboarding: standard without auth and onboarding.
python create_project.py --spec $O/specs/badge-qa.json --set package_name=com.example.wp06.badgeqa --out $O/badgeqa --no-zip --force
(cd $O/badgeqa && gw 30m :app:assembleDevRelease --console=plain)
(cd $O/everything && gw 30m :app:assembleDevRelease --console=plain)
echo BUILDS OK

#!/usr/bin/env bash
# accept_builds.sh on the Windows machine, every Gradle build of accept.win.sh in one hold of the shared lock:
#   $L gradle bash accept_builds.win.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP06; O=$OV/out/WP06
# SDK packages change only through Android Studio's SDK Manager on this machine, never from a build.
NO_SDK_DOWNLOAD=-Pandroid.builder.sdkDownload=false
# The shared daemon can run out of class metadata after many projects' builds and expires after that build, so a
# build that died of it runs once more on a fresh daemon. Any other failure stands.
gw() {
  local log; log=$(mktemp)
  if ./gradlew $NO_SDK_DOWNLOAD "$@" 2>&1 | tee "$log"; then rm -f "$log"; return 0; fi
  if grep -q "OutOfMemoryError: Metaspace" "$log"; then
    rm -f "$log"; echo "RETRY: the daemon ran out of Metaspace"; ./gradlew $NO_SDK_DOWNLOAD "$@"; return
  fi
  rm -f "$log"; return 1
}

cd $W/templates/android
gw build --continue --console=plain
gw :core:network:testDebugUnitTest :core:database:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :core:devtools:testDebugUnitTest :core:applock:testDebugUnitTest :core:playstore:testDebugUnitTest \
  :core:analytics:testDebugUnitTest :data:profile:testDebugUnitTest :app:testDevDebugUnitTest --console=plain
cd $W/generator
for s in standard everything standard-nofirebase; do
  python create_project.py --spec tests/fixtures/v2/$s.json --set package_name=com.example.wp06.${s//-/} --out $O/$s --no-zip --force
  (cd $O/$s && gw check assembleDevDebug --console=plain)
done
# QA-44 needs the tab root without sign-in or onboarding: standard without auth and onboarding.
python create_project.py --spec $O/specs/badge-qa.json --set package_name=com.example.wp06.badgeqa --out $O/badgeqa --no-zip --force
(cd $O/badgeqa && gw :app:assembleDevRelease --console=plain)
(cd $O/everything && gw :app:assembleDevRelease --console=plain)
echo BUILDS OK

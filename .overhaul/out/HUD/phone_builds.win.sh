#!/usr/bin/env bash
# The builds the phone checks need, in one hold of the Gradle lock:
#   bin/locked gradle bash out/HUD/phone_builds.win.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/HUD; O=$OV/out/HUD
gw() {
  local limit=$1; shift
  timeout --kill-after=2m "$limit" ./gradlew -Pandroid.builder.sdkDownload=false --console=plain "$@"
}
cd $W/generator
python create_project.py --spec $O/specs/hud-everything.json --out $O/everything --no-zip --force
python create_project.py --spec $O/specs/hud-standard.json --out $O/standard --no-zip --force
(cd $O/everything && gw 50m :app:assembleDevRelease :app:assembleProdRelease :app:assembleDevDebug)
(cd $O/standard && gw 30m :app:assembleDevRelease)
echo BUILDS OK

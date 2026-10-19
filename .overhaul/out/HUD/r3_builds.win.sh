#!/usr/bin/env bash
# Run 3 (after merging wp/WP06 08c0d0a): the everything phone builds for the demo and the APK inspection, then
# every acceptance build, each in its own hold of the shared Gradle lock:
#   bash out/HUD/r3_builds.win.sh   (it calls bin/locked itself)
set -uo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/HUD; O=$OV/out/HUD
cd $W/generator
python create_project.py --spec $O/specs/hud-everything.json --out $O/everything --no-zip --force || exit 1
cd $O/everything
$OV/bin/locked gradle ./gradlew -Pandroid.builder.sdkDownload=false --console=plain \
  :app:assembleDevRelease :app:assembleProdRelease :app:assembleDevDebug
echo "PHONE BUILDS EXIT $?"
$OV/bin/locked gradle bash $O/accept_builds.win.sh
echo "ACCEPT EXIT $?"

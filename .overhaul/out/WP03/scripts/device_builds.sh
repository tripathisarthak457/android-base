#!/usr/bin/env bash
# Regenerates the PTR and CP1 projects from the worktree and builds their release APKs, in one
# Gradle lock hold:  $L gradle device_builds.sh
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP03; O=$OV/out/WP03
cd "$W/generator"
python3 create_project.py --spec "$O/specs/ptr.json" --out "$O/ptr" --no-zip --force >/dev/null
python3 create_project.py --spec "$O/specs/cp1.json" --out "$O/cp1" --no-zip --force >/dev/null
(cd "$O/ptr" && ./gradlew :app:assembleDevRelease --console=plain -q)
(cd "$O/cp1" && ./gradlew :catalog:assembleRelease --console=plain -q)
find "$O/ptr/app/build/outputs/apk" "$O/cp1/catalog/build/outputs/apk" -name '*.apk'
echo DEVICE BUILDS OK

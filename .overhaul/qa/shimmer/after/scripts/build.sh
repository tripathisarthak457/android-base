#!/usr/bin/env bash
# Generates the pull-to-refresh sample app and the catalog from wt/WP03-skel (the shimmer fix) under
# their own package names, so WP03's installed test apps stay as they are, and builds both release
# APKs in one Gradle lock hold:  bin/locked gradle bash qa/shimmer/after/scripts/build.sh
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP03-skel; O=$OV/out/WP03-skel
cd "$W/generator"
python3 create_project.py --spec "$O/specs/ptr.json" --out "$O/ptr" --no-zip --force >/dev/null
python3 create_project.py --spec "$O/specs/cp1.json" --out "$O/cp1" --no-zip --force >/dev/null
git -C "$W" rev-parse HEAD > "$O/BUILT_FROM"
git -C "$W" status --porcelain >> "$O/BUILT_FROM"
(cd "$O/ptr" && ./gradlew :app:assembleDevRelease --console=plain -q)
(cd "$O/cp1" && ./gradlew :catalog:assembleRelease --console=plain -q)
find "$O/ptr/app/build/outputs/apk" "$O/cp1/catalog/build/outputs/apk" -name '*.apk'
echo DEVICE BUILDS OK

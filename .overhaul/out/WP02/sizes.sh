#!/usr/bin/env bash
# Builds devRelease for each project dir given and prints arm64/universal APK size and total dex size.
# Usage: sizes.sh <label> <project dir>...
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
L=${L:-/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked}
label=$1; shift
for d in "$@"; do
  (cd "$d" && $L gradle ./gradlew :app:assembleDevRelease --console=plain -PabiSplits=true) > "$d.build.log" 2>&1 || { echo "$label $(basename $d) BUILD FAILED (see $d.build.log)"; continue; }
  for apk in $(find "$d/app/build/outputs/apk/dev/release" -name '*.apk' | sort); do
    bytes=$(stat -c %s "$apk")
    dex=$(unzip -l "$apk" | awk '/classes[0-9]*\.dex$/ {s+=$1} END {print s}')
    echo "$label $(basename $d) $(basename $apk) apk=$bytes dex=$dex"
  done
done

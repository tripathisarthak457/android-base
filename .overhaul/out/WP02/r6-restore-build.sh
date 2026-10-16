#!/usr/bin/env bash
# Minified devRelease of a sample+settings project without the nav-key keep rules, for the restore check.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
cd /home/crazyhusir/Documents/projects/tools/.overhaul/out/WP02/restore
/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked gradle ./gradlew :app:assembleDevRelease --console=plain
echo "exit=$?"
find app/build/outputs/apk -name '*.apk'

#!/usr/bin/env bash
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
cd /home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP02/templates/android
/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked gradle ./gradlew build --continue --console=plain
echo "exit=$?"

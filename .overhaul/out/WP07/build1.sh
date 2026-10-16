#!/usr/bin/env bash
set -o pipefail
export ANDROID_HOME=$HOME/Android/Sdk
cd /home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP07/templates/multiplatform
date
/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked gradle ./gradlew :androidApp:assembleDevDebug --continue --console=plain
echo "exit=$?"
date

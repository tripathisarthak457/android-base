#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked
cd $W/templates/android
$L gradle ./gradlew :core:navigation:testDebugUnitTest :app:testDevDebugUnitTest --offline --console=plain "$@"
echo TEST OK

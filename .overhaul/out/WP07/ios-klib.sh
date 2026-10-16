#!/usr/bin/env bash
# The iOS klib compile of the template, as the acceptance runs it; result in logs/ios-klib.log.
set -o pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
cd $OV/wt/WP07/templates/multiplatform
{ date; $OV/bin/locked gradle ./gradlew :shared:compileKotlinIosSimulatorArm64 --console=plain; echo "ios klib exit=$?"; date; } > $OV/out/WP07/logs/ios-klib.log 2>&1

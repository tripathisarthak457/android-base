#!/usr/bin/env bash
# r8-analyzer Path A: AGP 9.4's standalone R8 configuration analyzer on the template's devRelease.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
cd /home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP02/templates/android
/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked gradle ./gradlew :app:analyzeDevReleaseR8Config --console=plain
echo "exit=$?"
ls -la app/build/reports/r8/ 2>&1

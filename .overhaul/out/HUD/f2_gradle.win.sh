#!/usr/bin/env bash
# Fix run 2: Gradle tasks on the template, one hold of the shared lock:
#   bin/locked gradle bash out/HUD/f2_gradle.win.sh <tasks...>
set -euo pipefail
cd D:/projects/tools/.overhaul/wt/HUD/templates/android
timeout --kill-after=2m 40m ./gradlew -Pandroid.builder.sdkDownload=false --console=plain "$@"
echo GRADLE OK

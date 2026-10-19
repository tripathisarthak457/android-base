#!/usr/bin/env bash
# Fix run 3: Gradle tasks on the template, one hold of the shared lock:
#   bin/locked gradle bash out/HUD/f3_gradle.win.sh <tasks...>
set -euo pipefail
cd D:/projects/tools/.overhaul/wt/HUD/templates/android
# One retry on a fresh daemon: lint has died of a Metaspace OOM of the daemon on this machine.
timeout --kill-after=2m 40m ./gradlew -Pandroid.builder.sdkDownload=false --console=plain --continue "$@" ||
  timeout --kill-after=2m 40m ./gradlew -Pandroid.builder.sdkDownload=false --console=plain --continue --no-daemon "$@"
echo GRADLE OK

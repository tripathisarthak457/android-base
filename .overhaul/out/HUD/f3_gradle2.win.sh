#!/usr/bin/env bash
# Fix run 3: Gradle tasks on the template, one hold of the shared lock; a retry on a fresh daemon only
# when the daemon died of a Metaspace OOM, as lint has on this machine:
#   bin/locked gradle bash out/HUD/f3_gradle2.win.sh <log> <tasks...>
set -uo pipefail
log=$1; shift
cd D:/projects/tools/.overhaul/wt/HUD/templates/android
timeout --kill-after=2m 40m ./gradlew -Pandroid.builder.sdkDownload=false --console=plain --continue "$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}
if [ "$status" -ne 0 ] && grep -q "Metaspace" "$log"; then
  timeout --kill-after=2m 40m ./gradlew -Pandroid.builder.sdkDownload=false --console=plain --continue --no-daemon "$@" 2>&1 | tee -a "$log"
  status=${PIPESTATUS[0]}
fi
[ "$status" -eq 0 ] && echo GRADLE OK || echo "GRADLE FAILED $status"
exit "$status"

#!/usr/bin/env bash
# Builds the generated everything projects out/WP04-perf/<label>... (devBenchmarkRelease) under the Gradle lock.
#   ./assemble.sh <label>... > logs/assemble-<label>.log 2>&1
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04-perf
$L gradle bash -c '
set -euo pipefail
for n in "$@"; do
  cd '"$O"'/$n
  ./gradlew :app:assembleDevBenchmarkRelease --console=plain
  echo "BUILT $n"
done' _ "$@"
echo "ASSEMBLE exit $?"

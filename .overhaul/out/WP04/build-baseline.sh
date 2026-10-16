#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04
(cd $O/base && $L gradle ./gradlew :app:assembleDevBenchmarkRelease --console=plain)
(cd $O/base-standard && $L gradle ./gradlew :app:assembleDevRelease --console=plain)
(cd $O/base-lean && $L gradle ./gradlew :app:assembleDevRelease --console=plain)
echo BUILD-BASELINE OK

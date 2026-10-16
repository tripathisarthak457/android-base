#!/usr/bin/env bash
# usage: build.sh <logname> <gradle args...>
set -o pipefail
export ANDROID_HOME=$HOME/Android/Sdk
log=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP07/logs/$1.log; shift
cd /home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP07/templates/multiplatform
{ date; /home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked gradle ./gradlew "$@" --console=plain; echo "exit=$?"; date; } > "$log" 2>&1

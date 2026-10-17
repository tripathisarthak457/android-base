#!/usr/bin/env bash
# usage: build.sh <logname> <gradle args...>   (Windows, Git Bash; locked sets the SDK and cache variables)
set -o pipefail
OV=D:/projects/tools/.overhaul
log=$OV/out/WP07/logs/$1.log; shift
cd $OV/wt/WP07/templates/multiplatform
{ date; $OV/bin/locked gradle ./gradlew "$@" --console=plain; echo "exit=$?"; date; } > "$log" 2>&1

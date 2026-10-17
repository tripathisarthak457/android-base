#!/usr/bin/env bash
# Runs one Gradle invocation in a project under the gradle lock, logging to $O/logs/<name>.log.
#   gw.sh <name> <project-dir> <gradle args...>
# locked sets JAVA_HOME, ANDROID_HOME and GRADLE_USER_HOME from the user's registry environment.
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP03; L=$OV/bin/locked
name=$1; dir=$2; shift 2
log=$O/logs/$name.log
cd "$dir" || exit 2
{ $L gradle ./gradlew "$@" --console=plain; echo "EXIT $?"; } > "$log" 2>&1
tail -n 1 "$log"

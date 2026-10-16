#!/usr/bin/env bash
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
cd /home/crazyhusir/Documents/projects/tools/.overhaul/out/Full
LOG=/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/build.log
echo "start release $(date +%s)" >> $LOG
s=$(date +%s)
./gradlew :app:assembleDevRelease --console=plain >> $LOG 2>&1; rc=$?
e=$(date +%s)
echo "END devRelease rc=$rc secs=$((e-s))" >> $LOG
s=$(date +%s)
./gradlew :app:assembleDevDebug --console=plain >> $LOG 2>&1; rc=$?
e=$(date +%s)
echo "END devDebug rc=$rc secs=$((e-s))" >> $LOG

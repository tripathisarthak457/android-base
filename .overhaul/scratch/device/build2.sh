#!/usr/bin/env bash
export ANDROID_HOME=$HOME/Android/Sdk
cd /home/crazyhusir/Documents/projects/tools/.overhaul/out/FullDevtools
LOG=/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/build2.log
s=$(date +%s); ./gradlew :app:assembleDevRelease --console=plain >> $LOG 2>&1; rc=$?; e=$(date +%s)
echo "END devtoolsRelease rc=$rc secs=$((e-s))" >> $LOG

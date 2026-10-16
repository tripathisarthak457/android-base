#!/usr/bin/env bash
# Generates the baseline profile on the emulator (the rules depend on the code paths, not on the
# device's speed) from final-everything, then splits it into the per-module profiles.
#   $L adb ./emu-profile.sh > logs/r8-emu-profile.log 2>&1      (takes the Gradle lock inside)
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk ANDROID_SERIAL=emulator-5554
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04
cd $O/final-everything
$L gradle ./gradlew :app:generateBaselineProfile --console=plain
ls -la app/src/main/generated/baselineProfiles/ || find . -path ./build -prune -o -name "baseline-prof.txt" -newer BUILT_FROM -print
echo EMU-PROFILE DONE

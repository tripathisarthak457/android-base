#!/usr/bin/env bash
# Generates the baseline profile on the phone from final-everything (adb lock outside, Gradle lock
# inside), then splits it into the per-module profiles of that project.
#   $L adb ./phone-profile.sh > logs/<step>.log 2>&1
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk ANDROID_SERIAL=9a7c74f4
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04
cd $O/final-everything
adb shell input keyevent KEYCODE_WAKEUP
$L gradle bash -c '
set -euo pipefail
./gradlew :app:tasks :benchmark:tasks --all --console=plain | grep -i -E "baselineprofile|connected.*AndroidTest" | head -40
./gradlew :app:generateBaselineProfile --console=plain'
find app/src -path '*generated/baselineProfiles*' -type f -exec ls -la {} \;
python3 $OV/wt/WP04/templates/android/tools/perf/split_profile.py --project .
echo PHONE-PROFILE DONE

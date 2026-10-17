#!/usr/bin/env bash
# Step 23 again at the worktree's HEAD (Windows port of phone-profile.sh + r15-profile.sh): records the baseline
# profile on the phone from final-everything (adb lock outside, Gradle lock inside), splits it into that project's
# modules, then copies each module's profile into the template with the package renamed back (the generator renames
# it again).
#   $L adb ./profile.win.sh > logs/<step>.log 2>&1
set -euo pipefail
export ANDROID_SERIAL=9a7c74f4
OV=D:/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04; W=$OV/wt/WP04/templates/android
EV=$O/final-everything
cd $EV
echo "== $(date +%H:%M:%S) built from $(cat BUILT_FROM)"
adb shell input keyevent KEYCODE_WAKEUP
rm -rf app/src/main/generated/baselineProfiles
$L gradle ./gradlew :app:generateBaselineProfile --console=plain
test -s app/src/main/generated/baselineProfiles/baseline-prof.txt
echo "== $(date +%H:%M:%S) generated: $(wc -l < app/src/main/generated/baselineProfiles/baseline-prof.txt) rules"
python3 $W/tools/perf/split_profile.py --project .
for m in core/navigation core/designsystem core/ui app; do
  sed 's#com/example/wp04/everything/#com/base/app/#g; s#com/example/wp04/everything;#com/base/app;#g' \
    $m/src/main/baseline-prof.txt > $W/$m/src/main/baseline-prof.txt
  echo "$m: $(wc -l < $W/$m/src/main/baseline-prof.txt) lines, $(grep -c 'wp04' $W/$m/src/main/baseline-prof.txt || true) unrenamed"
done
echo PROFILE DONE

#!/usr/bin/env bash
# Run 13: RepeatedKeyTest without the content-key fix (it must fail), then with it, then the navigation
# and app unit tests, in one Gradle lock hold.
#   ./r13-dup-test.sh > logs/<step>.log 2>&1
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked
SCR=/tmp/claude-1000/-home-crazyhusir-Documents-projects-tools/e3f9060d-6364-4a81-a9a2-20851e25521f/scratchpad/fix
N=templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation
FILES=($N/stack/StackHost.kt $N/presentation/PresentationHost.kt)
for f in "${FILES[@]}"; do cp $W/$f $SCR/$(basename $f); done
restore() { for f in "${FILES[@]}"; do cp $SCR/$(basename $f) $W/$f; done; }
trap restore EXIT
$L gradle bash -c '
cd '"$W"'/templates/android
for f in '"${FILES[*]}"'; do git -C '"$W"' show HEAD:$f > '"$W"'/$f; done
./gradlew :core:navigation:testDebugUnitTest --tests "*RepeatedKeyTest*" --console=plain > '"$OV"'/out/WP04/logs/r13-dup-before.log 2>&1
echo "== without the fix: exit $? (expected non-zero)"
grep -E "RepeatedKeyTest > .*(FAILED|PASSED)|used multiple times|Exception" '"$OV"'/out/WP04/logs/r13-dup-before.log | head -12
for f in '"${FILES[*]}"'; do cp '"$SCR"'/$(basename $f) '"$W"'/$f; done
./gradlew :core:navigation:testDebugUnitTest :app:testDevDebugUnitTest --console=plain --continue
echo "== with the fix: exit $?"'

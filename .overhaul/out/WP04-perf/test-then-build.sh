#!/usr/bin/env bash
# The navigation and design-system tests that cover the perf fixes, then, when they pass, the everything preset
# generated and built from the worktree's HEAD into out/WP04-perf/<name>. One Gradle lock hold for both.
#   ./test-then-build.sh <name> > logs/<step>.log 2>&1
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04-perf; L=$OV/bin/locked; O=$OV/out/WP04-perf
name=${1:-fixed}
cd $W/generator
python3 create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.wp04.everything \
  --out $O/$name --no-zip --force > $O/logs/gen-$name.log || { echo "GENERATE FAILED"; exit 1; }
git -C $W rev-parse --short HEAD > $O/$name/BUILT_FROM
git -C $W status --short -- templates > $O/$name/BUILT_DIRTY
$L gradle bash -c '
set -euo pipefail
cd '"$W"'/templates/android
./gradlew :core:navigation:testDebugUnitTest --tests "*Recomposition*" --tests "*TabBar*" --tests "*Pane*" --tests "*StackScene*" --tests "*ChromeLayering*" --tests "*RapidBack*" --tests "*Restore*" \
  :core:designsystem:compileDebugKotlin --console=plain
echo TESTS OK
cd '"$O"'/'"$name"'
./gradlew :app:assembleDevBenchmarkRelease --console=plain
echo "BUILT '"$name"'"'
echo TEST-THEN-BUILD OK

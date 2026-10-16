#!/usr/bin/env bash
# One Gradle lock hold: the navigation and app unit tests, then (if they pass) everything, standard and lean
# generated from the worktree and built as accept.sh builds them.
#   ./cycle.sh [presets...] > logs/<step>.log 2>&1
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
presets=("${@:-everything standard lean}")
read -r -a presets <<< "${presets[*]}"
cd $W/generator
for n in "${presets[@]}"; do
  python3 create_project.py --spec tests/fixtures/v2/$n.json --set package_name=com.example.wp04.$n \
    --out $O/final-$n --no-zip --force > $O/logs/gen-$n.log
  git -C $W rev-parse --short HEAD > $O/final-$n/BUILT_FROM
  git -C $W status --short -- templates > $O/final-$n/BUILT_DIRTY
done
$L gradle bash -c '
set -euo pipefail
cd '"$W"'/templates/android
./gradlew :core:navigation:testDebugUnitTest :app:testDevDebugUnitTest --offline --console=plain --continue
echo TESTS OK
for n in "$@"; do
  cd '"$O"'/final-$n
  if [ "$n" = everything ]; then
    ./gradlew :app:assembleDevBenchmarkRelease :benchmark:assemble --console=plain
  else
    ./gradlew :app:assembleDevRelease --console=plain
  fi
  echo "BUILT $n"
done' _ "${presets[@]}"
echo CYCLE OK

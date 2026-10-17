#!/usr/bin/env bash
# Windows port of r14-build.sh: the given presets (default everything, standard and lean) generated from the
# worktree's HEAD and built in one Gradle lock hold, with final-everything's non-minified app and benchmark APKs.
#   ./build.win.sh [presets...] > logs/<step>.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
presets=("${@:-everything standard lean}"); read -r -a presets <<< "${presets[*]}"
cd $W/generator
for n in "${presets[@]}"; do
  python3 $O/rmtree-long.py $O/final-$n
  python3 create_project.py --spec tests/fixtures/v2/$n.json --set package_name=com.example.wp04.$n \
    --out $O/final-$n --no-zip --force > $O/logs/gen-$n.log
  git -C $W rev-parse --short HEAD > $O/final-$n/BUILT_FROM
  git -C $W status --short -- templates > $O/final-$n/BUILT_DIRTY
done
$L gradle bash -c '
set -euo pipefail
for n in "$@"; do
  cd '"$O"'/final-$n
  if [ "$n" = everything ]; then
    ./gradlew :app:assembleDevBenchmarkRelease :app:assembleDevNonMinifiedRelease :benchmark:assemble --console=plain
  else
    ./gradlew :app:assembleDevRelease --console=plain
  fi
  echo "BUILT $n"
done' _ "${presets[@]}"
echo BUILD OK

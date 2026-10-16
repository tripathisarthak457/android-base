#!/usr/bin/env bash
# Generates everything, standard and lean from the worktree as accept.sh does and builds the measured APKs
# (devBenchmarkRelease + the benchmark module for everything, devRelease for the others) in one Gradle lock hold.
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
done
$L gradle bash -c '
set -euo pipefail
for n in "$@"; do
  cd '"$O"'/final-$n
  if [ "$n" = everything ]; then
    ./gradlew :app:assembleDevBenchmarkRelease :benchmark:assemble --console=plain
  else
    ./gradlew :app:assembleDevRelease --console=plain
  fi
done' _ "${presets[@]}"
echo BUILD-ALL OK

#!/usr/bin/env bash
# The given presets (default: everything) generated from the WP04-perf worktree's HEAD and built under the
# Gradle lock, as out/WP04/r14-build.sh does for WP04.
#   ./build.sh [presets...] > logs/<step>.log 2>&1
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04-perf; L=$OV/bin/locked; O=$OV/out/WP04-perf
presets=("${@:-everything}"); read -r -a presets <<< "${presets[*]}"
cd $W/generator
for n in "${presets[@]}"; do
  python3 create_project.py --spec tests/fixtures/v2/$n.json --set package_name=com.example.wp04.$n \
    --out $O/$n --no-zip --force > $O/logs/gen-$n.log
  git -C $W rev-parse --short HEAD > $O/$n/BUILT_FROM
  git -C $W status --short -- templates > $O/$n/BUILT_DIRTY
done
$L gradle bash -c '
set -euo pipefail
for n in "$@"; do
  cd '"$O"'/$n
  if [ "$n" = everything ]; then
    ./gradlew :app:assembleDevBenchmarkRelease --console=plain
  else
    ./gradlew :app:assembleDevRelease --console=plain
  fi
  echo "BUILT $n"
done' _ "${presets[@]}"
echo BUILD OK

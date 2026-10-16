#!/usr/bin/env bash
# Test fixtures: a scaffolded data module shares its fake with the feature's tests, compact and split.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
cd $OV/wt/WP02/generator
for spec in modules-compact modules-modular; do
  python3 create_project.py --spec tests/fixtures/v2/$spec.json --out $O/tf-$spec --no-zip --force > $O/logs/gen-tf-$spec.log 2>&1
done
$OV/bin/locked gradle bash -c "
  set -e
  cd $O/tf-modules-compact && ./gradlew check assembleDevDebug --continue --console=plain
  cd $O/tf-modules-modular && ./gradlew assembleDevDebug testDebugUnitTest --console=plain
"
echo FIXTURES OK

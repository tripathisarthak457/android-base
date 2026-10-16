#!/usr/bin/env bash
# Run 15, under one Gradle lock hold: the template and three generated projects pass verifyCodeRules with the
# scroll-chrome baseline (run 14's r14-rules2.sh was cut off waiting for the lock before the projects ran).
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
export OVERHAUL_HOLDS_GRADLE=1
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP02
W=/home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP02
cd $W/generator
for spec in everything standard-modular lean standard; do
  python3 create_project.py --spec tests/fixtures/v2/$spec.json --out $O/r15-$spec --no-zip --force > /dev/null
done
cd $W/templates/android && ./gradlew :build-logic:convention:test verifyCodeRules --offline --console=plain
for spec in everything standard-modular lean standard; do
  echo "=== r15-$spec"
  (cd $O/r15-$spec && ./gradlew verifyCodeRules --offline --console=plain)
done
echo R15 OK

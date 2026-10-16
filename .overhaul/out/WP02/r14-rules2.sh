#!/usr/bin/env bash
# Run 14: the template and three generated projects pass verifyCodeRules with the scroll-chrome baseline.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP02
W=/home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP02
cd $W/generator
for spec in everything standard-modular lean; do
  python3 create_project.py --spec tests/fixtures/v2/$spec.json --out $O/r14-$spec --no-zip --force > /dev/null
done
cd $W/templates/android && $L gradle ./gradlew :build-logic:convention:test verifyCodeRules --offline --console=plain
for spec in everything standard-modular lean; do
  (cd $O/r14-$spec && $L gradle ./gradlew verifyCodeRules --offline --console=plain)
done
echo R14 OK

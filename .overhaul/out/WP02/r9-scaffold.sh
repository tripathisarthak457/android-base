#!/usr/bin/env bash
# Scaffolded modules after the chrome-padding and placeholder change: build, module checks, the placeholder
# heights at four font scales (a one-off test, not shipped), and what ktlint changes or reports in them.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
W=$OV/wt/WP02
P=$O/r9-fmt
T=$P/feature/subscriptions/src/test/kotlin/io/example/wp02fmt/feature/subscriptions
cd $W/generator
python3 create_project.py --spec $O/specs/r9-fmt.json --out $P --no-zip --force > $O/logs/r9-gen.log 2>&1
cd $P && git init -q && git add -A && git -c user.name=wp02 -c user.email=wp02@localhost commit -qm base
$OV/bin/locked gradle bash -c "
  set -e
  cd $P
  ./gradlew assembleDevDebug :feature:subscriptions:check :data:subscriptions:check :feature:orders:check :data:orders:check --console=plain
  cp $O/k1/PlaceholderHeightTest.kt $T/
  ./gradlew :feature:subscriptions:testDebugUnitTest --tests '*PlaceholderHeightTest' --console=plain
  rm $T/PlaceholderHeightTest.kt
  ./gradlew spotlessApply --continue --console=plain > $O/logs/r9-spotless.log 2>&1 || echo spotless-exit=\$?
"
cp $P/feature/subscriptions/build/test-results/testDebugUnitTest/TEST-*PlaceholderHeightTest.xml $O/k1/ || true
git -C $P status --porcelain -- feature/subscriptions data/subscriptions feature/orders data/orders > $O/logs/r9-spotless-scaffold.txt
git -C $P diff -- feature/subscriptions data/subscriptions feature/orders data/orders > $O/logs/r9-spotless-scaffold.diff
echo R9 OK

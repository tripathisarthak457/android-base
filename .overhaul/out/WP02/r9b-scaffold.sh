#!/usr/bin/env bash
# The rest of r9-scaffold.sh after its first build passed: the one-off placeholder-height test and spotlessApply.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
P=$O/r9-fmt
T=$P/feature/subscriptions/src/test/kotlin/io/example/wp02fmt/feature/subscriptions
$OV/bin/locked gradle bash -c "
  set -e
  cd $P
  cp $O/k1/PlaceholderHeightTest.kt $T/
  ./gradlew :feature:subscriptions:testDebugUnitTest --tests '*PlaceholderHeightTest' --console=plain
  rm $T/PlaceholderHeightTest.kt
  ./gradlew spotlessApply --continue --console=plain > $O/logs/r9-spotless.log 2>&1 || echo spotless-exit=\$?
"
cp $P/feature/subscriptions/build/test-results/testDebugUnitTest/TEST-*PlaceholderHeightTest.xml $O/k1/ || true
git -C $P status --porcelain -- feature/subscriptions data/subscriptions feature/orders data/orders > $O/logs/r9-spotless-scaffold.txt
git -C $P diff -- feature/subscriptions data/subscriptions feature/orders data/orders > $O/logs/r9-spotless-scaffold.diff
echo R9B OK

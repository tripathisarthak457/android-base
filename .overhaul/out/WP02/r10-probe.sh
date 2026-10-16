#!/usr/bin/env bash
# Which transparent text sizes a placeholder bone exactly like the text it stands for, at six font scales.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
P=$O/r9-fmt
T=$P/feature/subscriptions/src/test/kotlin/io/example/wp02fmt/feature/subscriptions
rm -f $T/PlaceholderHeightTest.kt
cp $O/k1/PlaceholderProbeTest.kt $T/
$OV/bin/locked gradle bash -c "
  cd $P
  ./gradlew :feature:subscriptions:testDebugUnitTest --tests '*PlaceholderProbeTest' --console=plain
" || echo gradle-exit=$?
rm -f $T/PlaceholderProbeTest.kt
grep -h K1PROBE $P/feature/subscriptions/build/test-results/testDebugUnitTest/TEST-*PlaceholderProbeTest.xml
echo PROBE DONE

#!/usr/bin/env bash
# Cold-JVM runs of the instrumented AppLockGateTest, the order and position in which it stalled.
set -uo pipefail
O=D:/projects/tools/.overhaul/out/WP06
cd $O/standard-nofirebase
for i in $(seq 1 ${RUNS:-12}); do
  WP06_TEST_TIMEOUT_MIN=3 timeout 8m ./gradlew -Pandroid.builder.sdkDownload=false -I $O/test-timeout.init.gradle \
    :core:applock:testDebugUnitTest --tests '*AppLockGateTest' --rerun --console=plain > $O/diag/probe/run3.$i.log 2>&1
  echo "run $i: exit $?"
  grep -q STALL $O/diag/probe/gate.log 2>/dev/null && break
done

#!/usr/bin/env bash
set -uo pipefail
O=D:/projects/tools/.overhaul/out/WP06
cd $O/standard-nofirebase
export PROBE_REPS=${PROBE_REPS:-12}
WP06_TEST_TIMEOUT_MIN=8 timeout 15m ./gradlew -Pandroid.builder.sdkDownload=false -I $O/test-timeout.init.gradle \
  :core:applock:testDebugUnitTest --tests '*AppLockGateRaceProbe' --rerun --console=plain
echo "exit $?"

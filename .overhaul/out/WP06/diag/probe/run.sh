#!/usr/bin/env bash
set -uo pipefail
O=D:/projects/tools/.overhaul/out/WP06
cd $O/standard-nofirebase
WP06_TEST_TIMEOUT_MIN=6 timeout 15m ./gradlew -Pandroid.builder.sdkDownload=false -I $O/test-timeout.init.gradle \
  :core:applock:testDebugUnitTest --tests '*SettingsRaceProbe' --rerun --console=plain
echo "exit $?"

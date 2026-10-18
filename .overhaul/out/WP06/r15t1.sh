#!/usr/bin/env bash
set -euo pipefail
O=D:/projects/tools/.overhaul/out/WP06
cd D:/projects/tools/.overhaul/wt/WP06/templates/android
timeout 20m ./gradlew -Pandroid.builder.sdkDownload=false -I $O/test-timeout.init.gradle \
  :core:applock:testDebugUnitTest --rerun :core:applock:lintDebug --console=plain

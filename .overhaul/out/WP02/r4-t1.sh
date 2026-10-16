#!/usr/bin/env bash
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
cd /home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP02/templates/android
/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked gradle ./gradlew :build-logic:convention:test :core:testing:compose:testDebugUnitTest :feature:settings:testDebugUnitTest :feature:auth:testDebugUnitTest :feature:sample:testDebugUnitTest :core:designsystem:testDebugUnitTest :catalog:testDebugUnitTest :app:lintDevDebug :app:assembleDevRelease verifyCodeRules --continue --console=plain
echo "exit=$?"

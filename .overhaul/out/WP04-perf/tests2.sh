#!/usr/bin/env bash
# Round 2 tests: the whole navigation module, the sample feature and the design system compile.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04-perf; L=$OV/bin/locked
cd $W/templates/android
$L gradle bash -c './gradlew :core:navigation:testDebugUnitTest :feature:sample:testDevDebugUnitTest :core:designsystem:compileDebugKotlin :app:compileDevDebugKotlin --console=plain' && echo TESTS2 OK

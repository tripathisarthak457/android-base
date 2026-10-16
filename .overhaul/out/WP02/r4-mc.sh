#!/usr/bin/env bash
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP02
cd $O/modules-compact && /home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked gradle ./gradlew assembleDevDebug testDebugUnitTest verifyCodeRules --continue --console=plain
echo "exit=$?"

#!/usr/bin/env bash
# Run 14: the scroll-chrome rule's unit tests, then every module's verifyCodeRules on the template.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
cd /home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP02/templates/android
$L gradle ./gradlew :build-logic:convention:test verifyCodeRules --continue --offline --console=plain
echo "EXIT $?"

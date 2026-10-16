#!/usr/bin/env bash
# One Gradle lock hold: the template's design-system tests and the catalog, then the sheet code rules
# against the copy of wp/WP02's build-logic in the scratchpad.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP03; L=$OV/bin/locked; W=$OV/wt/WP03
SP=/tmp/claude-1000/-home-crazyhusir-Documents-projects-tools/e3f9060d-6364-4a81-a9a2-20851e25521f/scratchpad
name=${1:?name}; shift
$L gradle bash -c "
cd $W/templates/android
./gradlew $* --console=plain; echo \"TEMPLATE EXIT \$?\"
cd $SP/wp02rules/build-logic
../gradlew :convention:test --tests '*SheetRule*' --tests '*CodeRulesTest*' --console=plain; echo \"RULES EXIT \$?\"
" > $O/logs/$name.log 2>&1
echo "CHAIN DONE" >> $O/logs/$name.log

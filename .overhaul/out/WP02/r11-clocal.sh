#!/usr/bin/env bash
# r10-fit.sh's c-local step with the right task list: without network a data module has no unit tests
# of its own (its fake is in test fixtures, compiled by the feature's tests).
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
cd $O/r10-c-local
tasks=":app:assembleDevDebug"
for m in $(python3 -c "import json; print(' '.join(json.load(open('$O/specs/r10-c-local.json'))['feature_modules']))"); do
  tasks="$tasks :feature:$m:testDebugUnitTest :feature:$m:verifyCodeRules :data:$m:verifyCodeRules"
done
$OV/bin/locked gradle ./gradlew $tasks --continue --console=plain > $O/logs/r11-clocal.log 2>&1
echo "build-c-local-exit=$?"

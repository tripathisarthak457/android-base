#!/usr/bin/env bash
# WP02 milestone M1 acceptance. Run in the background; the log must end with ACCEPT OK.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
W=$OV/wt/WP02
L=$OV/bin/locked

cd $W/templates/android && $L gradle ./gradlew build --console=plain

# `! cmd` never trips `set -e`, so each negated check exits by hand.
! grep -rn "suspend fun handleEvent" $W/templates/android || exit 1
! grep -rn "core:model\|core:coroutines\|core.model\.\|core.coroutines\." $W/templates/android --include=*.kts --include=*.kt || exit 1
! grep -rnE '^import (android\.|javax\.|dagger\.|java\.)' $W/templates/android/core/common/src || exit 1
! grep -rn "@IoDispatcher\|@DefaultDispatcher\|@ApplicationScope" $W/templates/android --include=*.kt || exit 1
grep -rq 'related_row' $W/templates/android/feature/sample/src/main
cd $W/generator && python3 -m unittest discover -s tests -t .

echo ACCEPT OK

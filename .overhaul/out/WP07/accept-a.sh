#!/usr/bin/env bash
# WP07 phase A acceptance. Run in the background; ends with ACCEPT OK.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
W=$OV/wt/WP07; L=$OV/bin/locked; O=$OV/out/WP07
cd $W/templates/multiplatform && $L gradle ./gradlew :androidApp:assembleDevDebug check --console=plain
set +e; $L gradle ./gradlew :shared:compileKotlinIosSimulatorArm64 --console=plain > $O/logs/ios-klib.log 2>&1; echo "ios klib exit=$?" >> $O/logs/ios-klib.log; set -e
cd $W/generator
python3 -m unittest discover -s tests -t .
python3 -m genkit.tokens --check
python3 -m genkit.core.catalogue --check
python3 $W/tools/sync_check.py
for s in mp-standard mp-everything; do
  python3 create_project.py --spec tests/fixtures/v2/$s.json --out $O/$s --no-zip --force
  (cd $O/$s && $L gradle ./gradlew :androidApp:assembleDevDebug check --console=plain)
done
echo ACCEPT OK

#!/usr/bin/env bash
# WP07 phase A acceptance (Windows, Git Bash). Run in the background; ends with ACCEPT OK.
# locked and memwait set JAVA_HOME, ANDROID_HOME, GRADLE_USER_HOME, KONAN_DATA_DIR and the other cache variables.
# Until package WIN merges, the generator suite fails the six host tests of logs/windows/generator-baseline.md.
set -euo pipefail
OV=D:/projects/tools/.overhaul
W=$OV/wt/WP07; L=$OV/bin/locked; M=$OV/bin/memwait; O=$OV/out/WP07
cd $W/templates/multiplatform && $L gradle ./gradlew :androidApp:assembleDevDebug check --console=plain
set +e; $L gradle ./gradlew :shared:compileKotlinIosSimulatorArm64 --console=plain > $O/logs/ios-klib.log 2>&1; echo "ios klib exit=$?" >> $O/logs/ios-klib.log; set -e
cd $W/generator
$M python -B -m unittest discover -s tests -t .
$M python -B -m genkit.tokens --check
$M python -B -m genkit.core.catalogue --check
$M python -B $W/tools/sync_check.py
for s in mp-standard mp-everything; do
  $M python -B create_project.py --spec tests/fixtures/v2/$s.json --out $O/$s --no-zip --force
  (cd $O/$s && $L gradle ./gradlew :androidApp:assembleDevDebug check --console=plain)
done
echo ACCEPT OK

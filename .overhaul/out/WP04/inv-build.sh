#!/usr/bin/env bash
# Investigation copy (not a template change): everything at HEAD with a dev-only composition logger, devDebug.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
cd $W/generator
python3 create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.wp04.inv \
  --out $O/inv-everything --no-zip --force > $O/logs/gen-inv.log
d=$O/inv-everything/app/src/dev/kotlin/com/example/wp04/inv/perf
sed 's/PKG/com.example.wp04.inv/g' $O/inv-patch/CompositionLog.kt > $d/CompositionLog.kt
cd $O/inv-everything
$L gradle ./gradlew :app:assembleDevDebug --console=plain
echo INV-BUILD OK

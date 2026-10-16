#!/usr/bin/env bash
# Experiment copy (not a template change): everything at HEAD with AppScaffold's background fill removed, so each
# screen is filled once (by EntryFrame). Measures what the redundant full-screen fill costs.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
cd $W/generator
python3 create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.wp04.exp \
  --out $O/exp-everything --no-zip --force > $O/logs/gen-exp.log
f=$O/exp-everything/core/designsystem/src/main/kotlin/com/example/wp04/exp/core/designsystem/component/container/AppScaffold.kt
grep -c '            .background(background)' $f
sed -i '/^            \.background(background)$/d' $f
! grep -q '\.background(background)' $f
cd $O/exp-everything
$L gradle ./gradlew :app:assembleDevBenchmarkRelease --console=plain
echo EXP-BUILD OK

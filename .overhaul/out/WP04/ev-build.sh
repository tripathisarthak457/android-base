#!/usr/bin/env bash
# Generates the everything fixture from the worktree as accept.sh does (final-everything, app id
# com.example.wp04.everything) and builds the measured APK and the benchmark module.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
cd $W/generator
python3 create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.wp04.everything \
  --out $O/final-everything --no-zip --force
cd $O/final-everything
git -C $W rev-parse --short HEAD > BUILT_FROM
$L gradle ./gradlew :app:assembleDevBenchmarkRelease :benchmark:assemble --console=plain
echo EV-BUILD OK

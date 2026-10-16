#!/usr/bin/env bash
# Generates the phase 3 check project (the everything fixture) from the worktree and builds its
# devBenchmarkRelease APKs. Usage: p3-build.sh [name]   (default p3; the app id is com.example.wp04.<name>)
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
name=${1:-p3}
cd $W/generator
python3 create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.wp04.$name \
  --out $O/$name --no-zip --force
cd $O/$name
$L gradle ./gradlew :app:assembleDevBenchmarkRelease --console=plain
echo P3-BUILD OK

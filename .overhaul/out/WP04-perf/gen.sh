#!/usr/bin/env bash
# Generates the everything preset from the WP04-perf worktree as it is now into out/WP04-perf/<label>
# (fast; no Gradle). ./gen.sh <label>
set -euo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04-perf; O=$OV/out/WP04-perf
label=$1
cd $W/generator
python3 create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.wp04.everything \
  --out $O/$label --no-zip --force > $O/logs/gen-$label.log
git -C $W rev-parse --short HEAD > $O/$label/BUILT_FROM
git -C $W status --short -- templates > $O/$label/BUILT_DIRTY
echo "GENERATED $label from $(cat $O/$label/BUILT_FROM), $(wc -l < $O/$label/BUILT_DIRTY) dirty"
